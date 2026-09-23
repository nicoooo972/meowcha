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
import kotlinx.coroutines.flow.Flow

/** Progression globale de la joueuse (une seule ligne, id = 0). */
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 0,
    val coins: Int = 30,
    val day: Int = 1,
    val totalServed: Int = 0,
    val perfectServed: Int = 0,
    val equippedMug: String = "classic",
    val bestDayCoins: Int = 0,
)

/** Mugs achetés dans la boutique. */
@Entity(tableName = "owned_mugs")
data class OwnedMugEntity(
    @PrimaryKey val mugId: String,
    val boughtAt: Long = System.currentTimeMillis(),
)

/** Album des chats clients : combien de fois servis et leur affection. */
@Entity(tableName = "cats_met")
data class CatMetEntity(
    @PrimaryKey val catId: String,
    val timesServed: Int = 0,
    val hearts: Int = 0,
)

/** Historique des journées au café. */
@Entity(tableName = "day_history")
data class DayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val coins: Int,
    val served: Int,
    val perfect: Int,
    val playedAt: Long = System.currentTimeMillis(),
)

@Dao
interface GameDao {
    @Query("SELECT * FROM player WHERE id = 0")
    fun player(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player WHERE id = 0")
    suspend fun playerNow(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayer(p: PlayerEntity)

    @Query("SELECT * FROM owned_mugs")
    fun ownedMugs(): Flow<List<OwnedMugEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMug(m: OwnedMugEntity)

    @Query("SELECT * FROM cats_met")
    fun catsMet(): Flow<List<CatMetEntity>>

    @Query("SELECT * FROM cats_met WHERE catId = :id")
    suspend fun catNow(id: String): CatMetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCat(c: CatMetEntity)

    @Insert
    suspend fun addHistory(h: DayHistoryEntity)

    @Query("SELECT * FROM day_history ORDER BY id DESC LIMIT 20")
    fun history(): Flow<List<DayHistoryEntity>>
}

@Database(
    entities = [PlayerEntity::class, OwnedMugEntity::class, CatMetEntity::class, DayHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MeowchaDb : RoomDatabase() {
    abstract fun dao(): GameDao

    companion object {
        @Volatile private var instance: MeowchaDb? = null

        fun get(context: Context): MeowchaDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MeowchaDb::class.java, "meowcha.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
