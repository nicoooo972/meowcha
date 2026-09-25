package com.meowcha.game.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.meowcha.game.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

private const val RELEASES_URL = "https://api.github.com/repos/nicoooo972/meowcha/releases?per_page=5"
private const val APK_ASSET_NAME = "meowcha-cafe.apk"

/** Une release plus récente que ce que l'app a déjà proposé d'installer. */
data class AppUpdate(
    val tagName: String,
    val versionLabel: String,
    val apkUrl: String,
    val size: Long,
    private val marker: String,
) {
    internal fun markerValue() = marker
}

sealed interface UpdateDownloadEvent {
    data class Progress(val bytes: Long, val total: Long, val bytesPerSecond: Long) : UpdateDownloadEvent
    data object Done : UpdateDownloadEvent
}

/**
 * Vérifie s'il existe une release GitHub plus récente que celle déjà proposée, télécharge l'APK
 * et propose de l'installer directement : plus besoin d'aller chercher le fichier à la main
 * à chaque nouvelle version.
 */
object AppUpdater {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    /** null si hors-ligne, en erreur, ou si la dernière release a déjà été proposée. */
    suspend fun check(ctx: Context): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val releases = JSONArray(httpText(RELEASES_URL))
            if (releases.length() == 0) return@withContext null
            val latest = releases.getJSONObject(0)
            val assets = latest.getJSONArray("assets")
            var apkUrl: String? = null
            var size = 0L
            var updatedAt = latest.optString("published_at")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name") == APK_ASSET_NAME) {
                    apkUrl = a.getString("browser_download_url")
                    size = a.getLong("size")
                    updatedAt = a.optString("updated_at", updatedAt)
                }
            }
            val url = apkUrl ?: return@withContext null
            val tagName = latest.getString("tag_name")
            val marker = "$tagName@$updatedAt"
            val prefsObj = prefs(ctx)
            val seen = prefsObj.getString("last_seen_marker", null)
            if (marker == seen) return@withContext null
            if (seen == null) {
                // Premier lancement de l'app : évite de proposer une "mise à jour" vers la
                // version déjà installée si elle correspond à la dernière release.
                val remoteVersion = tagName.removePrefix("v").removeSuffix("-beta")
                if (remoteVersion == BuildConfig.VERSION_NAME) {
                    prefsObj.edit().putString("last_seen_marker", marker).apply()
                    return@withContext null
                }
            }
            AppUpdate(tagName, tagName.removePrefix("v"), url, size, marker)
        }.getOrNull()
    }

    /** Ne reproposera plus cette même build tant qu'aucune nouvelle n'est publiée. */
    fun dismiss(ctx: Context, update: AppUpdate) {
        prefs(ctx).edit().putString("last_seen_marker", update.markerValue()).apply()
    }

    suspend fun download(ctx: Context, update: AppUpdate, onEvent: (UpdateDownloadEvent) -> Unit): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dest = File(ctx.cacheDir, "update.apk")
                val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "MeowchaCafe/${BuildConfig.VERSION_NAME}")
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.size
                val start = System.nanoTime()
                var bytes = 0L
                var lastEmit = 0L
                conn.inputStream.use { input ->
                    dest.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            bytes += n
                            val now = System.nanoTime()
                            if (now - lastEmit > 80_000_000L || bytes == total) {
                                lastEmit = now
                                val speed = bytes * 1_000_000_000L / maxOf(1L, now - start)
                                onEvent(UpdateDownloadEvent.Progress(bytes, total, speed))
                            }
                        }
                    }
                }
                onEvent(UpdateDownloadEvent.Done)
                dismiss(ctx, update)
                dest
            }.getOrNull()
        }

    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    /** À lancer si [canInstall] est faux : laisse l'utilisateur autoriser Meowcha à installer des apps. */
    fun requestInstallPermissionIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    private fun httpText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "MeowchaCafe/${BuildConfig.VERSION_NAME}")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
