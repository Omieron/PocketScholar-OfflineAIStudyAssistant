package com.example.pocketscholar.data

import com.example.pocketscholar.data.db.ChunkDao
import com.example.pocketscholar.data.db.ChunkEntity
import com.example.pocketscholar.engine.EmbeddingEngine

/**
 * Repository for semantic search over stored chunks using query embeddings.
 * Uses ChunkDao for storage and VectorUtils for cosine-similarity top-k.
 */
class VectorStoreRepository(
    private val chunkDao: ChunkDao,
    private val embeddingEngine: EmbeddingEngine
) {

    /**
     * Returns the top [topK] chunks most similar to [queryEmbedding] by cosine similarity.
     */
    suspend fun searchSimilar(queryEmbedding: FloatArray, topK: Int = 5): List<ChunkEntity> {
        val chunks = chunkDao.getAll()
        return com.example.pocketscholar.data.db.VectorUtils.topKBySimilarity(queryEmbedding, chunks, topK)
    }

    /**
     * Embeds [queryText] and returns the top [topK] similar chunks. For RAG (Faz 3).
     */
    suspend fun searchSimilar(queryText: String, topK: Int = 5): List<ChunkEntity> {
        val embedding = embeddingEngine.embed(queryText)
        return searchSimilar(embedding, topK)
    }
}