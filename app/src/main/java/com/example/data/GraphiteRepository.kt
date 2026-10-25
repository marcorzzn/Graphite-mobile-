package com.example.data

import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import com.example.model.GraphiteDocumentModel
import com.example.model.OfflineCommentThread
import com.example.model.SocialPostScheduleItem
import com.example.model.VersionSnapshotItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GraphiteRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<GraphiteDocumentModel>> = dao.getAllDocuments().map { entities ->
        entities.mapNotNull { entity ->
            ProceduralEngine.fromGraphiteJson(entity.serializedJson)
        }
    }

    val allVersionSnapshots: Flow<List<VersionSnapshotItem>> = dao.getAllVersionSnapshots().map { list ->
        list.map { e ->
            VersionSnapshotItem(
                id = e.id,
                versionTag = e.versionTag,
                titleIt = e.titleIt,
                author = e.author,
                timestampLabel = e.timestampLabel,
                layerCount = e.layerCount,
                nodeCount = e.nodeCount,
                diffSummaryIt = e.diffSummaryIt,
                serializedJson = e.serializedJson
            )
        }
    }

    val allOfflineComments: Flow<List<OfflineCommentThread>> = dao.getAllOfflineComments().map { list ->
        list.map { e ->
            OfflineCommentThread(
                id = e.id,
                author = e.author,
                message = e.message,
                artboardX = e.artboardX,
                artboardY = e.artboardY,
                createdAtLabel = e.createdAtLabel,
                isResolved = e.isResolved
            )
        }
    }

    val allScheduledPosts: Flow<List<SocialPostScheduleItem>> = dao.getAllScheduledPosts().map { list ->
        list.map { e ->
            SocialPostScheduleItem(
                id = e.id,
                title = e.title,
                channel = e.channel,
                dateTimeLabel = e.dateTimeLabel,
                captionText = e.captionText,
                formatLabel = e.formatLabel,
                watermarkEnabled = e.watermarkEnabled
            )
        }
    }

    suspend fun ensureSeeded(): GraphiteDocumentModel {
        val count = dao.getDocumentCount()
        if (count == 0) {
            val fountain = DemoArtworkFactory.createIsometricFountain()
            val seasons = DemoArtworkFactory.createChangingSeasons()
            val starter = DemoArtworkFactory.createStarterDocument()
            saveDocument(seasons)
            saveDocument(starter)
            saveDocument(fountain)
            saveVersionSnapshot(
                doc = fountain,
                versionTag = "v1.0",
                titleIt = "Baseline Architettura Iniziale",
                author = "Marco Razzano (Lead)",
                diffSummaryIt = "Snapshot iniziale del progetto (6 livelli • 8 nodi DAG)"
            )
            saveOfflineComment(
                documentId = fountain.id,
                author = "Elena V. (Art Director)",
                message = "Ottimo bilanciamento cromatico sulla vasca isometrica! Verificare variante Hover.",
                artboardX = 540f,
                artboardY = 480f,
                createdAtLabel = "Oggi 10:15"
            )
            return fountain
        }
        return DemoArtworkFactory.createIsometricFountain()
    }

    suspend fun saveDocument(doc: GraphiteDocumentModel) {
        val updated = doc.copy(updatedAt = System.currentTimeMillis())
        val json = ProceduralEngine.toGraphiteJson(updated)
        val totalNodes = updated.layers.sumOf { it.nodes.size + 1 }
        val entity = DocumentEntity(
            id = updated.id,
            name = updated.name,
            artboardWidth = updated.artboardWidth,
            artboardHeight = updated.artboardHeight,
            layerCount = updated.layers.size,
            nodeCount = totalNodes,
            demoId = updated.demoId,
            serializedJson = json,
            updatedAt = updated.updatedAt
        )
        dao.upsertDocument(entity)
    }

    suspend fun saveVersionSnapshot(
        doc: GraphiteDocumentModel,
        versionTag: String,
        titleIt: String,
        author: String,
        diffSummaryIt: String
    ) {
        val json = ProceduralEngine.toGraphiteJson(doc)
        val entity = VersionSnapshotEntity(
            id = "snap-${System.currentTimeMillis()}",
            documentId = doc.id,
            versionTag = versionTag,
            titleIt = titleIt,
            author = author,
            timestampLabel = "Salvato in locale",
            layerCount = doc.layers.size,
            nodeCount = doc.layers.sumOf { it.nodes.size },
            diffSummaryIt = diffSummaryIt,
            serializedJson = json
        )
        dao.insertVersionSnapshot(entity)
    }

    suspend fun saveOfflineComment(
        documentId: String,
        author: String,
        message: String,
        artboardX: Float,
        artboardY: Float,
        createdAtLabel: String,
        id: String = "cmt-${System.currentTimeMillis()}",
        isResolved: Boolean = false
    ) {
        dao.upsertOfflineComment(
            CommentEntity(
                id = id,
                documentId = documentId,
                author = author,
                message = message,
                artboardX = artboardX,
                artboardY = artboardY,
                createdAtLabel = createdAtLabel,
                isResolved = isResolved
            )
        )
    }

    suspend fun saveScheduledPost(post: SocialPostScheduleItem) {
        dao.insertScheduledPost(
            ScheduledPostEntity(
                id = post.id,
                title = post.title,
                channel = post.channel,
                dateTimeLabel = post.dateTimeLabel,
                captionText = post.captionText,
                formatLabel = post.formatLabel,
                watermarkEnabled = post.watermarkEnabled
            )
        )
    }

    suspend fun loadDocumentById(id: String): GraphiteDocumentModel? {
        val entity = dao.getDocumentById(id) ?: return null
        return ProceduralEngine.fromGraphiteJson(entity.serializedJson)
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteDocumentById(id)
    }
}
