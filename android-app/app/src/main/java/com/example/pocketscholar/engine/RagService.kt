package com.example.pocketscholar.engine

import com.example.pocketscholar.data.VectorStoreRepository
import com.example.pocketscholar.data.db.ChunkEntity

/**
 * RAG pipeline: query → embedding → top-k chunks → prompt with context → LLM answer.
 * README_RAG.md §1.1–1.3.
 */
object RagService {

    private val RAG_PROMPT_TEMPLATE = """
Şu metne göre cevapla. Sadece verilen metinde yazanlara dayanarak kısa ve net cevap ver.

%s

Soru: %s

Cevap:""".trimIndent()

    private const val CHUNK_SEPARATOR = "\n---\n"
    private const val MAX_CONTEXT_CHARS = 3500
    private const val DEFAULT_TOP_K = 5

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
     * Chunk metinlerini [CHUNK_SEPARATOR] ile birleştirir; toplam uzunluk [maxChars]'ı geçmeyene kadar ekler.
     */
    private fun buildContextWithLimit(chunks: List<ChunkEntity>, maxChars: Int): String {
        if (chunks.isEmpty()) return "(Verilen metin yok.)"
        val sb = StringBuilder()
        for ((i, chunk) in chunks.withIndex()) {
            val part = chunk.text.trim()
            if (part.isEmpty()) continue
            val needSep = sb.isNotEmpty()
            val added = if (needSep) CHUNK_SEPARATOR + part else part
            if (sb.length + added.length > maxChars) {
                val spaceLeft = maxChars - sb.length - if (needSep) CHUNK_SEPARATOR.length else 0
                if (spaceLeft > 0) {
                    if (needSep) sb.append(CHUNK_SEPARATOR)
                    sb.append(part.take(spaceLeft))
                    if (spaceLeft < part.length) sb.append("…")
                }
                break
            }
            if (needSep) sb.append(CHUNK_SEPARATOR)
            sb.append(part)
        }
        return sb.toString()
    }
}
