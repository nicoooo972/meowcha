package com.meowcha.game.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.security.MessageDigest
import java.security.SecureRandom

/** Compte local stocké sur le téléphone (le mot de passe n'est jamais stocké en clair). */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val avatarCat: String,
    val salt: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY lastLogin DESC")
    suspend fun all(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE username = :username")
    suspend fun find(username: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(a: AccountEntity)

    @Query("UPDATE accounts SET lastLogin = :time WHERE username = :username")
    suspend fun touch(username: String, time: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

@Database(entities = [AccountEntity::class], version = 1, exportSchema = false)
abstract class AccountsDb : RoomDatabase() {
    abstract fun dao(): AccountDao

    companion object {
        @Volatile private var instance: AccountsDb? = null
        fun get(context: Context): AccountsDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AccountsDb::class.java, "meowcha_accounts.db")
                .build().also { instance = it }
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

    private fun adoptLegacySave(username: String) {
        val legacy = context.getDatabasePath("meowcha.db")
        if (!legacy.exists()) return
        val target = context.getDatabasePath(MeowchaDb.fileName(username))
        for (suffix in listOf("", "-wal", "-shm")) {
            val from = java.io.File(legacy.path + suffix)
            if (from.exists()) from.renameTo(java.io.File(target.path + suffix))
        }
    }

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
