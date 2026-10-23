package com.example.data

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

@Entity(tableName = "graphite_documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artboardWidth: Int,
    val artboardHeight: Int,
    val layerCount: Int,
    val nodeCount: Int,
    val demoId: String?,
    val serializedJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM graphite_documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM graphite_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM graphite_documents")
    suspend fun getDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Query("DELETE FROM graphite_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class GraphiteDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: GraphiteDatabase? = null

        fun getInstance(context: Context): GraphiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GraphiteDatabase::class.java,
                    "graphite_mobile_studio.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
