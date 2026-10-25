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

@Entity(tableName = "graphite_version_snapshots")
data class VersionSnapshotEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val versionTag: String,
    val titleIt: String,
    val author: String,
    val timestampLabel: String,
    val layerCount: Int,
    val nodeCount: Int,
    val diffSummaryIt: String,
    val serializedJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "graphite_offline_comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val author: String,
    val message: String,
    val artboardX: Float,
    val artboardY: Float,
    val createdAtLabel: String,
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "graphite_scheduled_posts")
data class ScheduledPostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channel: String,
    val dateTimeLabel: String,
    val captionText: String,
    val formatLabel: String,
    val watermarkEnabled: Boolean,
    val createdAt: Long = System.currentTimeMillis()
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

    // Version Snapshots
    @Query("SELECT * FROM graphite_version_snapshots ORDER BY createdAt DESC")
    fun getAllVersionSnapshots(): Flow<List<VersionSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionSnapshot(snapshot: VersionSnapshotEntity)

    // Offline Comments
    @Query("SELECT * FROM graphite_offline_comments ORDER BY createdAt DESC")
    fun getAllOfflineComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineComment(comment: CommentEntity)

    // Scheduled Posts
    @Query("SELECT * FROM graphite_scheduled_posts ORDER BY createdAt DESC")
    fun getAllScheduledPosts(): Flow<List<ScheduledPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPost(post: ScheduledPostEntity)
}

@Database(
    entities = [
        DocumentEntity::class,
        VersionSnapshotEntity::class,
        CommentEntity::class,
        ScheduledPostEntity::class
    ],
    version = 2,
    exportSchema = false
)
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
