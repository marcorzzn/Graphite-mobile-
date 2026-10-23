package com.example.data

import com.example.engine.DemoArtworkFactory
import com.example.engine.ProceduralEngine
import com.example.model.GraphiteDocumentModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GraphiteRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<GraphiteDocumentModel>> = dao.getAllDocuments().map { entities ->
        entities.mapNotNull { entity ->
            ProceduralEngine.fromGraphiteJson(entity.serializedJson)
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

    suspend fun loadDocumentById(id: String): GraphiteDocumentModel? {
        val entity = dao.getDocumentById(id) ?: return null
        return ProceduralEngine.fromGraphiteJson(entity.serializedJson)
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteDocumentById(id)
    }
}
