package com.meowcha.game.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Progression globale de la joueuse (une seule ligne logique). */
data class PlayerEntity(
    val coins: Int = 30,
    val day: Int = 1,
    val totalServed: Int = 0,
    val perfectServed: Int = 0,
    val equippedMug: String = "classic",
    val bestDayCoins: Int = 0,
    val bestCombo: Int = 0,
)

/** Mugs achetés dans la boutique. */
data class OwnedMugEntity(val mugId: String, val boughtAt: Long = System.currentTimeMillis())

/** Décorations achetées pour le café. */
data class OwnedDecorEntity(val decorId: String, val boughtAt: Long = System.currentTimeMillis())

/** Album des chats clients : combien de fois servis et leur affection. */
data class CatMetEntity(val catId: String, val timesServed: Int = 0, val hearts: Int = 0)

/** Historique des journées au café. */
data class DayHistoryEntity(
    val id: Long = 0,
    val day: Int,
    val coins: Int,
    val served: Int,
    val perfect: Int,
    val playedAt: Long = System.currentTimeMillis(),
)

private fun PlayerEntity.toJson() = JSONObject().apply {
    put("coins", coins); put("day", day); put("totalServed", totalServed)
    put("perfectServed", perfectServed); put("equippedMug", equippedMug)
    put("bestDayCoins", bestDayCoins); put("bestCombo", bestCombo)
}

private fun JSONObject.toPlayer() = PlayerEntity(
    coins = optInt("coins", 30), day = optInt("day", 1), totalServed = optInt("totalServed", 0),
    perfectServed = optInt("perfectServed", 0), equippedMug = optString("equippedMug", "classic"),
    bestDayCoins = optInt("bestDayCoins", 0), bestCombo = optInt("bestCombo", 0),
)

private fun DayHistoryEntity.toJson() = JSONObject().apply {
    put("id", id); put("day", day); put("coins", coins); put("served", served)
    put("perfect", perfect); put("playedAt", playedAt)
}

private fun JSONObject.toHistory() = DayHistoryEntity(
    id = optLong("id", 0), day = getInt("day"), coins = getInt("coins"), served = getInt("served"),
    perfect = getInt("perfect"), playedAt = optLong("playedAt", System.currentTimeMillis()),
)

/**
 * Sauvegarde locale au format JSON (remplace l'ancienne base Room, retirée pour éviter la
 * dépendance à KSP — voir CHANGELOG 2.0.0). Un fichier par compte, dans `filesDir`.
 * Toute mutation est appliquée en mémoire puis persistée sur disque de façon synchrone,
 * sous verrou, pour rester simple et éviter toute course entre lectures/écritures.
 */
class GameDao(private val file: File) {
    private val lock = Mutex()

    private val playerFlow = MutableStateFlow<PlayerEntity?>(null)
    private val mugsFlow = MutableStateFlow<List<OwnedMugEntity>>(emptyList())
    private val decorFlow = MutableStateFlow<List<OwnedDecorEntity>>(emptyList())
    private val catsFlow = MutableStateFlow<List<CatMetEntity>>(emptyList())
    private val historyFlow = MutableStateFlow<List<DayHistoryEntity>>(emptyList())
    private var nextHistoryId = 1L

    init { load() }

    private fun load() {
        if (!file.exists()) return
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return
        root.optJSONObject("player")?.let { playerFlow.value = it.toPlayer() }
        mugsFlow.value = root.optJSONArray("ownedMugs")?.let { arr ->
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { OwnedMugEntity(it.getString("mugId"), it.optLong("boughtAt")) } }
        } ?: emptyList()
        decorFlow.value = root.optJSONArray("ownedDecor")?.let { arr ->
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { OwnedDecorEntity(it.getString("decorId"), it.optLong("boughtAt")) } }
        } ?: emptyList()
        catsFlow.value = root.optJSONArray("catsMet")?.let { arr ->
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { CatMetEntity(it.getString("catId"), it.optInt("timesServed"), it.optInt("hearts")) } }
        } ?: emptyList()
        historyFlow.value = root.optJSONArray("history")?.let { arr ->
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toHistory() }
        } ?: emptyList()
        nextHistoryId = (historyFlow.value.maxOfOrNull { it.id } ?: 0) + 1
    }

    private fun persist() {
        val root = JSONObject()
        playerFlow.value?.let { root.put("player", it.toJson()) }
        root.put("ownedMugs", JSONArray().apply { mugsFlow.value.forEach { put(JSONObject().apply { put("mugId", it.mugId); put("boughtAt", it.boughtAt) }) } })
        root.put("ownedDecor", JSONArray().apply { decorFlow.value.forEach { put(JSONObject().apply { put("decorId", it.decorId); put("boughtAt", it.boughtAt) }) } })
        root.put("catsMet", JSONArray().apply { catsFlow.value.forEach { put(JSONObject().apply { put("catId", it.catId); put("timesServed", it.timesServed); put("hearts", it.hearts) }) } })
        root.put("history", JSONArray().apply { historyFlow.value.forEach { put(it.toJson()) } })
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(root.toString())
        tmp.renameTo(file)
    }

    fun player(): Flow<PlayerEntity?> = playerFlow
    suspend fun playerNow(): PlayerEntity? = playerFlow.value
    suspend fun savePlayer(p: PlayerEntity) = lock.withLock { playerFlow.value = p; persist() }

    fun ownedMugs(): Flow<List<OwnedMugEntity>> = mugsFlow
    suspend fun addMug(m: OwnedMugEntity) = lock.withLock {
        if (mugsFlow.value.none { it.mugId == m.mugId }) { mugsFlow.value = mugsFlow.value + m; persist() }
    }

    fun ownedDecor(): Flow<List<OwnedDecorEntity>> = decorFlow
    suspend fun addDecor(d: OwnedDecorEntity) = lock.withLock {
        if (decorFlow.value.none { it.decorId == d.decorId }) { decorFlow.value = decorFlow.value + d; persist() }
    }

    fun catsMet(): Flow<List<CatMetEntity>> = catsFlow
    suspend fun catNow(id: String): CatMetEntity? = catsFlow.value.find { it.catId == id }
    suspend fun saveCat(c: CatMetEntity) = lock.withLock {
        catsFlow.value = catsFlow.value.filterNot { it.catId == c.catId } + c
        persist()
    }

    suspend fun addHistory(h: DayHistoryEntity) = lock.withLock {
        historyFlow.value = (listOf(h.copy(id = nextHistoryId++)) + historyFlow.value).take(20)
        persist()
    }
    /** Les plus récentes d'abord, comme l'ancienne requête `ORDER BY id DESC LIMIT 20`. */
    fun history(): Flow<List<DayHistoryEntity>> = historyFlow
}

class MeowchaDb private constructor(file: File) {
    private val dao = GameDao(file)
    fun dao() = dao

    companion object {
        private val instances = mutableMapOf<String, MeowchaDb>()

        /** Une sauvegarde par compte : chaque profil a son propre fichier. */
        fun fileName(username: String) = "meowcha_${username.lowercase()}.json"

        fun get(context: Context, username: String): MeowchaDb = synchronized(this) {
            instances.getOrPut(username.lowercase()) {
                MeowchaDb(File(context.filesDir, fileName(username)))
            }
        }
    }
}
