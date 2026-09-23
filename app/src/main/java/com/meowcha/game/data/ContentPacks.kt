package com.meowcha.game.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.meowcha.game.BuildConfig
import com.meowcha.game.game.Accessory
import com.meowcha.game.game.CatCustomer
import com.meowcha.game.game.Cats
import com.meowcha.game.game.FurPattern
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.Recipe
import com.meowcha.game.game.Recipes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/** Un pack disponible sur le serveur (lu depuis index.json). */
data class RemotePack(
    val id: String,
    val version: Int,
    val name: String,
    val description: String,
    val file: String,
    val size: Long,
    val sha256: String,
)

/** Contenu issu des packs installés, utilisé par le jeu. */
data class PackInfo(
    val id: String,
    val name: String,
    val description: String,
    val cats: List<CatCustomer>,
    val recipes: List<Recipe>,
    val hasMusic: Boolean,
)

data class InstalledContent(
    val packs: List<PackInfo>,
    val music: Map<String, File>,
    val sfx: Map<String, File>,
)

sealed interface SyncEvent {
    data object Checking : SyncEvent
    data class Downloading(val pack: RemotePack, val index: Int, val count: Int, val bytes: Long, val total: Long, val bytesPerSecond: Long) : SyncEvent
    data class Installing(val pack: RemotePack) : SyncEvent
}

sealed interface SyncResult {
    data class UpToDate(val installed: Int) : SyncResult
    data class Updated(val ids: List<String>, val names: List<String>) : SyncResult
    data class Offline(val reason: String) : SyncResult
}

/**
 * Téléchargement et installation des packs de contenu (musique, bruitages, nouveaux chats, recettes).
 * Les packs sont publiés par la CI dans la release GitHub "content" et installés dans files/packs/<id>/.
 */
class ContentPacks(private val context: Context) {
    private val root = File(context.filesDir, "packs")
    private val baseUrl = BuildConfig.CONTENT_URL.trimEnd('/') + "/"

    fun installedVersion(id: String): Int = runCatching {
        JSONObject(File(root, "$id/manifest.json").readText()).getInt("version")
    }.getOrDefault(0)

    /** Liste les packs à télécharger (null si le serveur est injoignable). */
    suspend fun checkUpdates(): List<RemotePack>? = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(httpText(baseUrl + "index.json"))
            val arr = json.getJSONArray("packs")
            (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                RemotePack(
                    p.getString("id"), p.getInt("version"), p.getString("name"), p.optString("description"),
                    p.getString("file"), p.getLong("size"), p.getString("sha256"),
                )
            }.filter { it.version > installedVersion(it.id) }
        }.getOrNull()
    }

    suspend fun sync(onEvent: (SyncEvent) -> Unit): SyncResult {
        onEvent(SyncEvent.Checking)
        val updates = checkUpdates()
            ?: return SyncResult.Offline("Hors ligne — on garde le contenu déjà installé")
        if (updates.isEmpty()) return SyncResult.UpToDate(root.listFiles()?.size ?: 0)
        val done = mutableListOf<RemotePack>()
        for ((i, pack) in updates.withIndex()) {
            val ok = runCatching { download(pack, i, updates.size, onEvent) }.getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                false
            }
            if (ok) done += pack
        }
        return if (done.isEmpty()) SyncResult.Offline("Téléchargement interrompu, on réessaiera au prochain lancement")
        else SyncResult.Updated(done.map { it.id }, done.map { it.name })
    }

    private suspend fun download(pack: RemotePack, index: Int, count: Int, onEvent: (SyncEvent) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val tmp = File(context.cacheDir, "${pack.id}.zip.part")
            val conn = open(baseUrl + pack.file)
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: pack.size
            val digest = MessageDigest.getInstance("SHA-256")
            val start = System.nanoTime()
            var bytes = 0L
            var lastEmit = 0L
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        bytes += n
                        val now = System.nanoTime()
                        if (now - lastEmit > 80_000_000L || bytes == total) {
                            lastEmit = now
                            val speed = (bytes * 1_000_000_000L / maxOf(1L, now - start))
                            onEvent(SyncEvent.Downloading(pack, index, count, bytes, total, speed))
                        }
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(pack.sha256, ignoreCase = true)) {
                tmp.delete()
                return@withContext false
            }
            onEvent(SyncEvent.Installing(pack))
            val staging = File(root, "${pack.id}.new").apply { deleteRecursively(); mkdirs() }
            ZipInputStream(tmp.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(staging, entry.name).canonicalFile
                    // Protection contre les chemins "../" dans le zip
                    if (!target.path.startsWith(staging.canonicalPath)) continue
                    if (entry.isDirectory) target.mkdirs()
                    else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            tmp.delete()
            val dest = File(root, pack.id)
            dest.deleteRecursively()
            staging.renameTo(dest)
        }

    /** Charge les packs installés et enregistre leurs chats / recettes dans le jeu. */
    fun loadInstalled(): InstalledContent {
        val cats = mutableListOf<CatCustomer>()
        val recipes = mutableListOf<Recipe>()
        val music = mutableMapOf<String, File>()
        val sfx = mutableMapOf<String, File>()
        val packs = mutableListOf<PackInfo>()
        root.listFiles()?.filter { it.isDirectory && !it.name.endsWith(".new") }?.sortedBy { it.name }?.forEach { dir ->
            runCatching {
                val m = JSONObject(File(dir, "manifest.json").readText())
                val catsBefore = cats.size
                val recipesBefore = recipes.size
                m.optJSONArray("cats")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        cats += CatCustomer(
                            id = c.getString("id"),
                            name = c.getString("name"),
                            fur = color(c.getString("fur")),
                            accent = color(c.getString("accent")),
                            eyes = color(c.getString("eyes")),
                            pattern = runCatching { FurPattern.valueOf(c.getString("pattern")) }.getOrDefault(FurPattern.PLAIN),
                            accessory = runCatching { Accessory.valueOf(c.getString("accessory")) }.getOrDefault(Accessory.NONE),
                            accessoryColor = color(c.optString("accessoryColor", "#00000000")),
                            favorite = c.optString("favorite"),
                            quote = c.optString("quote"),
                        )
                    }
                }
                m.optJSONArray("recipes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        val steps = r.getJSONArray("steps")
                        val ingredients = (0 until steps.length()).map { Ingredient.valueOf(steps.getString(it)) }
                        recipes += Recipe(r.getString("id"), r.getString("name"), ingredients, r.getInt("price"), r.getInt("unlockDay"))
                    }
                }
                m.optJSONObject("music")?.let { o -> o.keys().forEach { k -> music[k] = File(dir, o.getString(k)) } }
                packs += PackInfo(
                    m.getString("id"), m.getString("name"), m.optString("description"),
                    cats.drop(catsBefore), recipes.drop(recipesBefore), m.has("music"),
                )
                m.optJSONObject("sfx")?.let { o -> o.keys().forEach { k -> sfx[k] = File(dir, o.getString(k)) } }
            }
        }
        Cats.setExtra(cats)
        Recipes.setExtra(recipes)
        return InstalledContent(packs, music.filterValues { it.exists() }, sfx.filterValues { it.exists() })
    }

    private fun color(hex: String) = Color(android.graphics.Color.parseColor(hex))

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "MeowchaCafe/${BuildConfig.VERSION_NAME}")
        if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
        return conn
    }

    private fun httpText(url: String) = open(url).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
}
