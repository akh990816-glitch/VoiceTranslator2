package com.example.voicetranslator2


import android.content.Context
import androidx.room.*

// 데이터베이스 테이블 구조 정의
@Entity(tableName = "translation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val original: String,
    val translated: String,
    val langCode: String,
    val timestamp: Long
)

// DB 접근 인터페이스
@Dao
interface HistoryDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Insert
    suspend fun insert(history: HistoryEntity)
}

// 데이터베이스 인스턴스 생성
@Database(entities = [HistoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "history_db"
                ).build()
                instance = inst
                inst
            }
        }
    }
}