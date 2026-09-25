package com.meowcha.game.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Compte local stocké sur le téléphone (le mot de passe n'est jamais stocké en clair). */
data class AccountEntity(
    val username: String,
    val displayName: String,
    val avatarCat: String,
    val salt: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
)

private fun AccountEntity.toJson() = JSONObject().apply {
    put("username", username); put("displayName", displayName); put("avatarCat", avatarCat)
    put("salt", salt); put("passwordHash", passwordHash); put("createdAt", createdAt); put("lastLogin", lastLogin)
}

private fun JSONObject.toAccount() = AccountEntity(
    username = getString("username"), displayName = getString("displayName"), avatarCat = getString("avatarCat"),
    salt = getString("salt"), passwordHash = getString("passwordHash"),
    createdAt = optLong("createdAt", System.currentTimeMillis()), lastLogin = optLong("lastLogin", System.currentTimeMillis()),
)

/** Comptes locaux stockés en JSON (voir CHANGELOG 2.0.0 : remplace l'ancienne base Room). */
class AccountDao(private val file: File) {
    private val lock = Mutex()
    private var accounts: List<AccountEntity> = load()

    private fun load(): List<AccountEntity> {
        if (!file.exists()) return emptyList()
        val arr = runCatching { JSONArray(file.readText()) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).toAccount() }
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(JSONArray().apply { accounts.forEach { put(it.toJson()) } }.toString())
        tmp.renameTo(file)
    }

    suspend fun all(): List<AccountEntity> = lock.withLock { accounts.sortedByDescending { it.lastLogin } }

    suspend fun find(username: String): AccountEntity? = lock.withLock { accounts.find { it.username == username } }

    suspend fun insert(a: AccountEntity) = lock.withLock {
        require(accounts.none { it.username == a.username }) { "compte déjà existant" }
        accounts = accounts + a
        persist()
    }

    suspend fun touch(username: String, time: Long) = lock.withLock {
        accounts = accounts.map { if (it.username == username) it.copy(lastLogin = time) else it }
        persist()
    }

    suspend fun count(): Int = lock.withLock { accounts.size }
}

class AccountsDb private constructor(file: File) {
    private val dao = AccountDao(file)
    fun dao() = dao

    companion object {
        @Volatile private var instance: AccountsDb? = null
        fun get(context: Context): AccountsDb = instance ?: synchronized(this) {
            instance ?: AccountsDb(File(context.filesDir, "meowcha_accounts.json")).also { instance = it }
        }
    }
}

sealed interface AuthResult {
    data class Success(val account: AccountEntity) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AccountRepository(private val context: Context) {
    private val dao = AccountsDb.get(context).dao()
    private val prefs = context.getSharedPreferences("meowcha_session", Context.MODE_PRIVATE)

    suspend fun accounts() = dao.all()

    suspend fun currentSession(): AccountEntity? =
        prefs.getString(KEY_USER, null)?.let { dao.find(it) }

    suspend fun register(rawName: String, password: String, confirm: String, avatarCat: String): AuthResult {
        val display = rawName.trim()
        val username = display.lowercase()
        if (display.length !in 3..16) return AuthResult.Error("Le pseudo doit faire entre 3 et 16 caractères")
        if (!username.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            return AuthResult.Error("Lettres, chiffres, - et _ seulement dans le pseudo")
        }
        if (password.length < 4) return AuthResult.Error("Le mot de passe doit faire au moins 4 caractères")
        if (password != confirm) return AuthResult.Error("Les mots de passe ne correspondent pas")
        if (dao.find(username) != null) return AuthResult.Error("Ce pseudo est déjà pris sur ce téléphone")

        // Le tout premier compte récupère la sauvegarde de la version 1.0
        if (dao.count() == 0) adoptLegacySave(username)

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        val account = AccountEntity(username, display, avatarCat, salt, hash(password, salt))
        dao.insert(account)
        saveSession(username)
        return AuthResult.Success(account)
    }

    suspend fun login(rawName: String, password: String): AuthResult {
        val account = dao.find(rawName.trim().lowercase())
            ?: return AuthResult.Error("Aucun compte avec ce pseudo")
        if (hash(password, account.salt) != account.passwordHash) return AuthResult.Error("Mot de passe incorrect")
        dao.touch(account.username, System.currentTimeMillis())
        saveSession(account.username)
        return AuthResult.Success(account)
    }

    fun logout() = prefs.edit().remove(KEY_USER).apply()

    private fun saveSession(username: String) = prefs.edit().putString(KEY_USER, username).apply()

    /**
     * Ancienne sauvegarde v1.0 (avant les comptes) : elle vivait dans une base SQLite Room.
     * Depuis la 2.0.0 les sauvegardes sont au format JSON (voir Database.kt) — les deux formats
     * ne sont pas compatibles, donc cette adoption ne peut plus rapatrier l'ancien fichier.
     * Impact : un compte créé sur un appareil qui a encore une sauvegarde v1.0 non migrée
     * repart de zéro plutôt que de la récupérer.
     */
    private fun adoptLegacySave(username: String) {}

    private fun hash(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        var bytes = (salt + password).toByteArray()
        repeat(20_000) { bytes = md.digest(bytes + salt.toByteArray()) }
        return bytes.toHex()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_USER = "current_user"
    }
}
