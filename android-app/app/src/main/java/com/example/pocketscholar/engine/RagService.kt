package com.example.pocketscholar.engine

import com.example.pocketscholar.data.VectorStoreRepository
import com.example.pocketscholar.data.db.ChunkEntity

/**
 * RAG pipeline: query → embedding → top-k chunks → prompt with context → LLM answer.
 * README_RAG.md §1.1–1.3.
 */
object RagService {

    private val RAG_PROMPT_TEMPLATE = """
The following text passages are the most relevant parts found for the question. Answer based ONLY on these passages.

IMPORTANT RULES:
1. If the question contains words like "all", "list", "tell me about", "describe", then describe ALL relevant information from the text completely. List everything, not just one item.
2. DO NOT REPEAT the same information. If information appears in multiple passages, mention it only once.
3. End your answer with complete sentences. Do not write incomplete sentences.

%s

Question: %s

Answer:""".trimIndent()

    private const val CHUNK_SEPARATOR = "\n---\n"
    // Context limit: max_prompt_tokens=500 in llama_jni, minus ~150 tokens for template+query ≈ 350 tokens context.
    // 350 tokens × 1.5 chars/token (Turkish) ≈ 525 characters. Increased from 600 to fit more chunks for "list all" queries.
    private const val MAX_CONTEXT_CHARS = 800
    // More chunks = better chance to include the right passage; context still capped by MAX_CONTEXT_CHARS.
    // With 400-char chunks + 80 overlap, ~1-2 chunks fit in 600 chars, so top_k=7 gives us choice.
    private const val DEFAULT_TOP_K = 7

    /**
     * Kaynak bilgisi: UI'da "Kaynak: sayfa 2, 4" gibi gösterilmek üzere.
     */
    data class RagSource(val documentId: String, val pageNumber: Int)

    /**
     * RAG cevabı: model çıktısı + kaynak chunk'ların (doc, sayfa) listesi.
     */
    data class RagResult(val answer: String, val sources: List<RagSource>)

    /**
     * Soruya RAG ile cevap üretir: vector store'dan top-k chunk alır, prompt şablonunda
     * context oluşturur (karakter limiti ile), LlamaEngine.prompt çağırır.
     *
     * @param query Kullanıcının sorduğu metin
     * @param vectorStore Vector store repository (embedding + chunk arama)
     * @param topK Alınacak chunk sayısı (varsayılan 5)
     * @return Cevap metni + kaynak (documentId, pageNumber) listesi
     */
    suspend fun ask(
        query: String,
        vectorStore: VectorStoreRepository,
        topK: Int = DEFAULT_TOP_K
    ): RagResult {
        val chunks = vectorStore.searchSimilar(query, topK)
        val context = buildContextWithLimit(chunks, MAX_CONTEXT_CHARS)
        val prompt = RAG_PROMPT_TEMPLATE.format(context, query)

        val answer = LlamaEngine.prompt(prompt) ?: "[Cevap oluşturulamadı.]"
        val sources = chunks
            .map { RagSource(it.documentId, it.pageNumber) }
            .distinctBy { it.documentId to it.pageNumber }

        return RagResult(answer.trim(), sources)
    }

    /**
     * Chunk metinlerini [CHUNK_SEPARATOR] ile birleştirir; overlap kısımlarını temizler.
     * Overlap nedeniyle chunk'lar birbirine karışmasın diye, önceki chunk'ın sonu ile yeni chunk'ın
     * başı arasındaki tekrar eden kısmı çıkarır.
     */
    private fun buildContextWithLimit(chunks: List<ChunkEntity>, maxChars: Int): String {
        if (chunks.isEmpty()) return "(Verilen metin yok.)"
        val sb = StringBuilder()
        var previousChunkEnd: String = ""
        
        for ((i, chunk) in chunks.withIndex()) {
            var part = chunk.text.trim()
            if (part.isEmpty()) continue
            
            // Remove overlap: if previous chunk's end matches new chunk's beginning, skip the overlap
            if (sb.isNotEmpty() && previousChunkEnd.isNotEmpty()) {
                val overlapRemoved = removeOverlap(previousChunkEnd, part)
                if (overlapRemoved != part) {
                    // Overlap found and removed
                    part = overlapRemoved
                }
            }
            
            val needSep = sb.isNotEmpty()
            val added = if (needSep) CHUNK_SEPARATOR + part else part
            
            if (sb.length + added.length > maxChars) {
                val spaceLeft = maxChars - sb.length - if (needSep) CHUNK_SEPARATOR.length else 0
                // Only add partial chunk if we can find a sentence boundary, otherwise skip it entirely
                if (spaceLeft > 100) {
                    if (needSep) sb.append(CHUNK_SEPARATOR)
                    val truncated = if (spaceLeft < part.length) {
                        val lastSentenceEnd = part.take(spaceLeft).lastIndexOfAny(listOf(". ", "! ", "? ", ".\n", "!\n", "?\n", ".\r\n", "!\r\n", "?\r\n"))
                        if (lastSentenceEnd > spaceLeft * 0.75) {
                            part.take(lastSentenceEnd + 1).trim()
                        } else {
                            null
                        }
                    } else {
                        part
                    }
                    if (truncated != null) {
                        sb.append(truncated)
                        previousChunkEnd = truncated
                        if (truncated.length < part.length) sb.append("…")
                    }
                }
                break
            }
            
            if (needSep) sb.append(CHUNK_SEPARATOR)
            sb.append(part)
            // Store last ~100 chars of this chunk for overlap detection with next chunk
            previousChunkEnd = if (part.length > 100) part.takeLast(100) else part
        }
        return sb.toString()
    }
    
    /**
     * Removes overlap between previous chunk's end and new chunk's beginning.
     * Looks for matching text at the boundary (typically 50-100 chars overlap).
     */
    private fun removeOverlap(previousEnd: String, newChunk: String): String {
        if (previousEnd.isEmpty() || newChunk.isEmpty()) return newChunk
        
        // Check for overlap: look for previousEnd's suffix in newChunk's beginning
        // Overlap is typically 50-100 chars, so check last 100 chars of previousEnd
        val checkLength = minOf(100, previousEnd.length, newChunk.length)
        val previousSuffix = previousEnd.takeLast(checkLength)
        
        // Try to find where previousSuffix matches newChunk's beginning
        // Start from longest match (most of overlap) down to shortest (at least 20 chars)
        for (matchLen in checkLength downTo 20) {
            val suffixToMatch = previousSuffix.takeLast(matchLen)
            if (newChunk.startsWith(suffixToMatch)) {
                // Found overlap - return newChunk without the overlapping part
                return newChunk.substring(matchLen).trim()
            }
        }
        
        return newChunk
    }
}
