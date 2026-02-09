package com.example.pocketscholar.data

import android.util.Log
import com.example.pocketscholar.data.db.ChunkDao
import com.example.pocketscholar.data.db.ChunkEntity
import com.example.pocketscholar.data.db.ScoredChunk
import com.example.pocketscholar.data.db.VectorUtils
import com.example.pocketscholar.engine.EmbeddingEngine

private const val TAG = "VectorStoreRepository"

/**
 * Repository for semantic search over stored chunks using query embeddings.
 * Uses ChunkDao for storage and VectorUtils for cosine-similarity top-k.
 * Supports document filtering and similarity score thresholding.
 */
class VectorStoreRepository(
    private val chunkDao: ChunkDao,
    private val embeddingEngine: EmbeddingEngine
) {

    /**
     * Returns the top [topK] chunks most similar to [queryEmbedding] by cosine similarity.
     * Optionally filters by specific document IDs and applies minimum similarity threshold.
     * 
     * @param queryEmbedding The query embedding vector
     * @param topK Number of top results to return
     * @param documentIds Optional list of document IDs to search within. If null, searches all documents.
     * @param minSimilarity Minimum similarity threshold (0.0-1.0). Chunks below this are filtered out.
     */
    suspend fun searchSimilarWithScores(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        documentIds: List<String>? = null,
        minSimilarity: Float = VectorUtils.DEFAULT_MIN_SIMILARITY
    ): List<ScoredChunk> {
        val chunks = if (documentIds.isNullOrEmpty()) {
            chunkDao.getAll()
        } else {
            chunkDao.getByDocumentIds(documentIds)
        }
        
        val results = VectorUtils.topKBySimilarityWithScores(queryEmbedding, chunks, topK, minSimilarity)
        
        // Log scores for debugging
        if (results.isNotEmpty()) {
            Log.d(TAG, "Search results (top ${results.size}): ${results.map { 
                "doc=${it.chunk.documentId.take(8)}..., sim=${String.format("%.3f", it.similarity)}" 
            }}")
        } else {
            Log.d(TAG, "No chunks found above similarity threshold $minSimilarity")
        }
        
        return results
    }

    /**
     * HYBRID SEARCH: Combines embedding similarity with keyword matching.
     * This ensures proper nouns (like "Enkidu", "Shamhat") are found even if
     * the embedding model doesn't understand their semantic meaning.
     * 
     * @param queryText The query text to search
     * @param topK Number of top results to return
     * @param documentIds Optional list of document IDs to search within
     * @param minSimilarity Minimum combined score threshold
     */
    suspend fun searchSimilarWithScores(
        queryText: String,
        topK: Int = 5,
        documentIds: List<String>? = null,
        minSimilarity: Float = 0.1f // Lower threshold for hybrid
    ): List<ScoredChunk> {
        val chunks = if (documentIds.isNullOrEmpty()) {
            chunkDao.getAll()
        } else {
            chunkDao.getByDocumentIds(documentIds)
        }
        
        if (chunks.isEmpty()) {
            Log.d(TAG, "No chunks available in database")
            return emptyList()
        }
        
        val embedding = embeddingEngine.embed(queryText)
        
        // Use hybrid search: 50% embedding, 50% keyword
        val results = VectorUtils.hybridSearch(
            queryEmbedding = embedding,
            query = queryText,
            chunks = chunks,
            k = topK,
            minSimilarity = minSimilarity,
            embeddingWeight = 0.5f // Equal weight to embedding and keyword
        )
        
        // Log detailed results for debugging
        Log.d(TAG, "Hybrid search for: \"${queryText.take(40)}...\"")
        Log.d(TAG, "  Total chunks: ${chunks.size}, Results: ${results.size}")
        results.forEachIndexed { i, sc ->
            Log.d(TAG, "  [$i] score=${String.format("%.3f", sc.similarity)} page=${sc.chunk.pageNumber}")
            Log.d(TAG, "      text: ${sc.chunk.text.take(60)}...")
        }
        
        if (results.isEmpty()) {
            Log.w(TAG, "No results found. Query words might not appear in any chunks.")
        }
        
        return results
    }

    // ---- Backward compatible methods (for existing code) ----

    /**
     * Returns the top [topK] chunks most similar to [queryEmbedding] by cosine similarity.
     * @deprecated Use searchSimilarWithScores for better debugging and filtering
     */
    suspend fun searchSimilar(queryEmbedding: FloatArray, topK: Int = 5): List<ChunkEntity> {
        val chunks = chunkDao.getAll()
        return com.example.pocketscholar.data.db.VectorUtils.topKBySimilarity(queryEmbedding, chunks, topK)
    }

    /**
     * Embeds [queryText] and returns the top [topK] similar chunks. For RAG (Faz 3).
     * @deprecated Use searchSimilarWithScores for better debugging and filtering
     */
    suspend fun searchSimilar(queryText: String, topK: Int = 5): List<ChunkEntity> {
        val embedding = embeddingEngine.embed(queryText)
        return searchSimilar(embedding, topK)
    }
}