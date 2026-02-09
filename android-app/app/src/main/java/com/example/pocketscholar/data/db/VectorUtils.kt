package com.example.pocketscholar.data.db

import kotlin.math.sqrt

/**
 * Cosine similarity and top-k selection for vector search over chunk embeddings.
 */
/**
 * A chunk paired with its similarity score for ranking and filtering.
 */
data class ScoredChunk(val chunk: ChunkEntity, val similarity: Float)

object VectorUtils {

    // Minimum similarity threshold: chunks below this score are filtered out
    // Lowered to 0.15 to allow broader matches for general queries
    const val DEFAULT_MIN_SIMILARITY = 0.15f

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

    /**
     * Returns the top [k] chunks with their similarity scores.
     * Applies minimum similarity threshold to filter out irrelevant results.
     * This is the preferred method for RAG as it enables debugging and re-ranking.
     */
    fun topKBySimilarityWithScores(
        queryEmbedding: FloatArray,
        chunks: List<ChunkEntity>,
        k: Int,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): List<ScoredChunk> {
        if (chunks.isEmpty() || k <= 0) return emptyList()
        val dim = queryEmbedding.size
        val scored = chunks.mapNotNull { chunk ->
            if (chunk.embedding.size != dim * 4) return@mapNotNull null
            val vec = EmbeddingBlob.bytesToFloatArray(chunk.embedding)
            val sim = cosineSimilarity(queryEmbedding, vec)
            // Filter out chunks below minimum similarity threshold
            if (sim < minSimilarity) return@mapNotNull null
            ScoredChunk(chunk, sim)
        }
        return scored
            .sortedByDescending { it.similarity }
            .take(k)
    }

    /**
     * Keyword-based search: scores chunks by how many query words appear in the text.
     * This is a fallback/complement to embedding search for proper nouns and specific terms.
     * 
     * @param query The search query
     * @param chunks All available chunks
     * @param k Number of top results to return
     * @return List of chunks with keyword match scores (normalized 0-1)
     */
    fun topKByKeywordMatch(
        query: String,
        chunks: List<ChunkEntity>,
        k: Int
    ): List<ScoredChunk> {
        if (chunks.isEmpty() || k <= 0 || query.isBlank()) return emptyList()
        
        // Extract meaningful words from query (3+ chars, lowercase)
        val queryWords = query.lowercase()
            .split(Regex("[\\s,.!?;:'\"()\\[\\]{}]+"))
            .filter { it.length >= 3 }
            .toSet()
        
        if (queryWords.isEmpty()) return emptyList()
        
        val scored = chunks.mapNotNull { chunk ->
            val textLower = chunk.text.lowercase()
            var matchCount = 0
            var totalMatchLength = 0
            
            for (word in queryWords) {
                if (textLower.contains(word)) {
                    matchCount++
                    // Bonus for exact word match (not just substring)
                    if (textLower.contains(Regex("\\b${Regex.escape(word)}\\b"))) {
                        matchCount++
                    }
                    totalMatchLength += word.length
                }
            }
            
            if (matchCount == 0) return@mapNotNull null
            
            // Normalize score: consider both match count and text coverage
            val matchRatio = matchCount.toFloat() / (queryWords.size * 2) // max 2 points per word
            val coverageBonus = (totalMatchLength.toFloat() / chunk.text.length).coerceAtMost(0.3f)
            val score = (matchRatio + coverageBonus).coerceAtMost(1f)
            
            ScoredChunk(chunk, score)
        }
        
        return scored
            .sortedByDescending { it.similarity }
            .take(k)
    }

    /**
     * Hybrid search: combines embedding similarity with keyword matching.
     * Returns chunks that are found by either method, with combined scoring.
     * 
     * @param queryEmbedding The query embedding vector
     * @param query The original query text (for keyword matching)
     * @param chunks All available chunks
     * @param k Number of top results to return
     * @param minSimilarity Minimum embedding similarity threshold
     * @param embeddingWeight Weight for embedding score (0-1), rest goes to keyword
     * @return Combined and re-ranked results
     */
    fun hybridSearch(
        queryEmbedding: FloatArray,
        query: String,
        chunks: List<ChunkEntity>,
        k: Int,
        minSimilarity: Float = 0.0f, // Lower threshold for hybrid
        embeddingWeight: Float = 0.6f
    ): List<ScoredChunk> {
        if (chunks.isEmpty() || k <= 0) return emptyList()
        
        val dim = queryEmbedding.size
        val keywordWeight = 1f - embeddingWeight
        
        // Score all chunks by both methods
        val combinedScores = mutableMapOf<String, Pair<ChunkEntity, Float>>()
        
        // Embedding scores
        for (chunk in chunks) {
            if (chunk.embedding.size != dim * 4) continue
            val vec = EmbeddingBlob.bytesToFloatArray(chunk.embedding)
            val embeddingSim = cosineSimilarity(queryEmbedding, vec)
            combinedScores[chunk.id] = chunk to (embeddingSim * embeddingWeight)
        }
        
        // Add keyword scores
        val queryWords = query.lowercase()
            .split(Regex("[\\s,.!?;:'\"()\\[\\]{}]+"))
            .filter { it.length >= 3 }
            .toSet()
        
        for (chunk in chunks) {
            val textLower = chunk.text.lowercase()
            var matchCount = 0
            
            for (word in queryWords) {
                if (textLower.contains(word)) {
                    matchCount++
                    if (textLower.contains(Regex("\\b${Regex.escape(word)}\\b"))) {
                        matchCount++
                    }
                }
            }
            
            if (matchCount > 0) {
                val keywordScore = (matchCount.toFloat() / (queryWords.size * 2)).coerceAtMost(1f)
                val existing = combinedScores[chunk.id]
                if (existing != null) {
                    combinedScores[chunk.id] = existing.first to (existing.second + keywordScore * keywordWeight)
                } else {
                    combinedScores[chunk.id] = chunk to (keywordScore * keywordWeight)
                }
            }
        }
        
        // Filter and sort
        return combinedScores.values
            .filter { it.second >= minSimilarity }
            .sortedByDescending { it.second }
            .take(k)
            .map { ScoredChunk(it.first, it.second) }
    }
}
