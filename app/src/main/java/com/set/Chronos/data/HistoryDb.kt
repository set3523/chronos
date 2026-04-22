package com.set.Chronos.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val timestamp: Long,
    val data: String  // HistoryRecord JSON
)

@Dao
interface HistoryDao {
    // Historymenu에서 관찰용 — UI 자동 갱신
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE timestamp = :ts LIMIT 1")
    suspend fun getByTs(ts: Long): HistoryEntity?

    // 증분 백업용
    @Query("SELECT * FROM history WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getSince(since: Long): List<HistoryEntity>

    // 현재 DB의 최대 timestamp (증분 복원용)
    @Query("SELECT MAX(timestamp) FROM history")
    suspend fun getMaxTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HistoryEntity>)
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = true)
abstract class ChronosDb : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: ChronosDb? = null
        fun get(context: Context): ChronosDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ChronosDb::class.java,
                "chronos.db"
            ).build().also { instance = it }
        }
    }
}