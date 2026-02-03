package com.example.pocketscholar.data.db

import kotlin.math.sqrt

/**
 * Cosine similarity and top-k selection for vector search over chunk embeddings.
 */
object VectorUtils {

    /**
     * Cosine similarity between two vectors: dot(a,b) / (||a|| * ||b||).
     * Returns value in [-1, 1]. Returns 0 if either vector has zero norm.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom <= 0f) 0f else (dot / denom)
    }

    /**
     * Returns the top [k] chunks most similar to [queryEmbedding] by cosine similarity.
     * Chunks with empty or mismatched embeddings are skipped. Returns fewer than [k] if not enough chunks.
     */
    fun topKBySimilarity(
        queryEmbedding: FloatArray,
        chunks: List<ChunkEntity>,
        k: Int
    ): List<ChunkEntity> {
        if (chunks.isEmpty() || k <= 0) return emptyList()
        val dim = queryEmbedding.size
        val scored = chunks.mapNotNull { chunk ->
            if (chunk.embedding.size != dim * 4) return@mapNotNull null
            val vec = EmbeddingBlob.bytesToFloatArray(chunk.embedding)
            val sim = cosineSimilarity(queryEmbedding, vec)
            chunk to sim
        }
        return scored
            .sortedByDescending { it.second }
            .take(k)
            .map { it.first }
    }
}
