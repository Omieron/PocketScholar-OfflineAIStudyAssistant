package com.example.pocketscholar.engine

import android.util.Log
import com.example.pocketscholar.data.VectorStoreRepository
import com.example.pocketscholar.data.db.ChunkEntity
import com.example.pocketscholar.data.db.ScoredChunk
import com.example.pocketscholar.data.db.VectorUtils

private const val TAG = "RagService"

/**
 * RAG pipeline: query → embedding → top-k chunks → prompt with context → LLM answer.
 * README_RAG.md §1.1–1.3.
 */
object RagService {

    // Instruct-style prompt - small models follow this better
    // Placeholders: %s = optional previous conversation block, %s = context (chunks), %s = current question
    private val RAG_PROMPT_TEMPLATE = """
### Instruction:
Answer the question using ONLY the information below. Be brief and precise.
%s

### Context:
%s

### Question:
%s

### Response:""".trimIndent()

    private const val CHUNK_SEPARATOR = "\n\n"
    // Context limit: ~1500 chars ≈ ~400 tokens, plus prompt template + query ≈ ~100 tokens = ~500 tokens total
    // This fits well within max_prompt_tokens = 1500 in llama_jni.cpp
    // Chunk context only; conversation has its own limit (plan §4)
    private const val MAX_CONTEXT_CHARS = 1500
    // Conversation history; if over limit we drop oldest (user, assistant) pairs
    private const val MAX_CONVERSATION_CHARS = 800
    // Fewer chunks = more focused context
    private const val DEFAULT_TOP_K = 5

    // When no RAG chunks found: still call LLM so user gets an answer (model runs)
    private val NO_CONTEXT_PROMPT_TEMPLATE = """
### Instruction:
Answer the question briefly.
%s

### Question:
%s

### Response:""".trimIndent()

    private const val NO_CONTEXT_PREFIX = "Belgelerde ilgili bilgi bulunamadı; genel yanıt: "

    /** System prompt when user has not selected any document: no chunks, remind that app is for PDFs. */
    private val SYSTEM_PROMPT_NO_SOURCE = """
### System:
This app is for answering from the user's PDFs, not general chat. Reply in 1-2 short sentences in Turkish. Be brief and polite. Do not ask the user questions about yourself. Do not repeat the same phrase.

### Instruction:
Answer the question in one or two short sentences only. Be brief and polite; do not ask questions back. Do not repeat yourself.
""".trimIndent()

    /** Fixed reminder appended when no document selected so user always sees it even if model drifts. */
    private const val NO_SOURCE_REMINDER = "\n\nBu uygulama yüklediğiniz PDF'lerle çalışmak içindir; belgeye dayalı yanıt için Belgeler'den kaynak seçin."

    private val NO_SOURCE_PROMPT_TEMPLATE = """
$SYSTEM_PROMPT_NO_SOURCE
%s

### Question:
%s

### Response:""".trimIndent()

    /**
     * Kaynak bilgisi: UI'da "Kaynak: sayfa 2, 4" gibi gösterilmek üzere.
     */
    data class RagSource(val documentId: String, val pageNumber: Int, val similarity: Float = 0f)

    /**
     * RAG cevabı: model çıktısı + kaynak chunk'ların (doc, sayfa) listesi.
     */
    data class RagResult(val answer: String, val sources: List<RagSource>)

    /**
     * Soruya RAG ile cevap üretir: vector store'dan top-k chunk alır, prompt şablonunda
     * context oluşturur (karakter limiti ile), LlamaEngine.prompt çağırır.
     * Multi-turn: [conversationHistory] verilirse prompt'a "Previous conversation" bölümü eklenir.
     *
     * @param query Kullanıcının sorduğu metin
     * @param vectorStore Vector store repository (embedding + chunk arama)
     * @param conversationHistory Son N tur (user, assistant) çiftleri; null/empty ise eklenmez
     * @param topK Alınacak chunk sayısı (varsayılan 10)
     * @param documentIds Belirli dokümanlarda arama yapmak için (opsiyonel, null = tüm dokümanlar)
     * @param skipRagContext true ise chunk araması yapılmaz; sadece system prompt ile kısa yanıt + "PDF'lerle çalışmak için kaynak seçin" hatırlatması
     * @param minSimilarity Minimum benzerlik eşiği (varsayılan 0.15)
     * @return Cevap metni + kaynak (documentId, pageNumber, similarity) listesi
     */
    suspend fun ask(
        query: String,
        vectorStore: VectorStoreRepository,
        conversationHistory: List<Pair<String, String>>? = null,
        topK: Int = DEFAULT_TOP_K,
        documentIds: List<String>? = null,
        skipRagContext: Boolean = false,
        minSimilarity: Float = VectorUtils.DEFAULT_MIN_SIMILARITY
    ): RagResult {
        if (skipRagContext) {
            Log.d(TAG, "No document selected: answering without RAG (system prompt only).")
            val conversationBlock = if (conversationHistory.isNullOrEmpty()) {
                ""
            } else {
                val formatted = formatConversationHistory(conversationHistory!!, MAX_CONVERSATION_CHARS)
                "\n### Previous conversation:\n$formatted\n"
            }
            val prompt = NO_SOURCE_PROMPT_TEMPLATE.format(conversationBlock, query)
            var answer = LlamaEngine.prompt(prompt) ?: "Cevap oluşturulamadı."
            answer = sanitizeResponse(answer)
            answer = truncateRepetition(answer)
            // Remove our reminder if model already echoed it so we append it only once
            val reminderText = "Bu uygulama yüklediğiniz PDF'lerle çalışmak içindir; belgeye dayalı yanıt için Belgeler'den kaynak seçin."
            answer = answer.replace(reminderText, "").trim()
            // Strict length cap for no-source path so repetitive/garbage output is never shown
            if (answer.length > 280) {
                val cut = answer.take(280)
                val lastEnd = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
                answer = if (lastEnd > 100) cut.take(lastEnd + 1) else cut.trimEnd() + "..."
            }
            return RagResult(answer.trim() + NO_SOURCE_REMINDER, emptyList())
        }

        val scoredChunks = vectorStore.searchSimilarWithScores(query, topK, documentIds, minSimilarity)
        
        // Log the search results for debugging
        Log.d(TAG, "Query: \"${query.take(50)}...\" -> Found ${scoredChunks.size} relevant chunks")
        scoredChunks.forEachIndexed { i, sc ->
            Log.d(TAG, "  [$i] sim=${String.format("%.3f", sc.similarity)} doc=${sc.chunk.documentId.take(16)}... page=${sc.chunk.pageNumber}")
            Log.d(TAG, "      text: ${sc.chunk.text.take(100)}...")
        }
        
        if (scoredChunks.isEmpty()) {
            Log.w(TAG, "No relevant chunks found for query. Calling LLM without RAG context.")
            val conversationBlock = if (conversationHistory.isNullOrEmpty()) {
                ""
            } else {
                val formatted = formatConversationHistory(conversationHistory!!, MAX_CONVERSATION_CHARS)
                "\n### Previous conversation:\n$formatted\n"
            }
            val noContextPrompt = NO_CONTEXT_PROMPT_TEMPLATE.format(conversationBlock, query)
            var answer = LlamaEngine.prompt(noContextPrompt) ?: "Cevap oluşturulamadı."
            answer = sanitizeResponse(answer)
            return RagResult("$NO_CONTEXT_PREFIX$answer", emptyList())
        }

        val chunks = scoredChunks.map { it.chunk }
        val context = buildContextWithLimit(chunks, MAX_CONTEXT_CHARS)
        val conversationBlock = if (conversationHistory.isNullOrEmpty()) {
            ""
        } else {
            val formatted = formatConversationHistory(conversationHistory!!, MAX_CONVERSATION_CHARS)
            "\n### Previous conversation:\n$formatted\n"
        }
        val prompt = RAG_PROMPT_TEMPLATE.format(conversationBlock, context, query)
        
        // Log the full context to debug what LLM sees
        Log.d(TAG, "=== CONTEXT SENT TO LLM ===")
        Log.d(TAG, context)
        Log.d(TAG, "=== END CONTEXT (${context.length} chars) ===")
        Log.d(TAG, "Prompt total length: ${prompt.length} chars")

        var answer = LlamaEngine.prompt(prompt) ?: "[Cevap oluşturulamadı.]"
        
        Log.d(TAG, "Raw LLM answer: ${answer.take(200)}...")
        
        // Sanitize: remove prompt fragments and detect repetition loops
        answer = sanitizeResponse(answer)
        
        val sources = scoredChunks
            .map { RagSource(it.chunk.documentId, it.chunk.pageNumber, it.similarity) }
            .distinctBy { it.documentId to it.pageNumber }

        return RagResult(answer.trim(), sources)
    }
    /**
     * Remove prompt fragments and detect/fix repetition loops.
     * Small LLMs sometimes echo prompts or get stuck repeating phrases.
     * Handles both leading echoes (prompt at start) and trailing echoes (prompt repeated after answer).
     */
    private fun sanitizeResponse(response: String): String {
        var cleaned = response.trim()

        // 0. Remove any line that looks like our prompt (### System:, ### Instruction:, etc.) so it never leaks to user
        val promptLikeLines = setOf(
            "this app is for answering from the user's pdfs, not general chat.",
            "reply in 1-2 short sentences in turkish. do not repeat the same phrase.",
            "answer the question in one or two short sentences only. do not repeat yourself."
        )
        cleaned = cleaned.lines()
            .filterNot { line ->
                val t = line.trim()
                t.startsWith("### ") || promptLikeLines.any { t.equals(it, ignoreCase = true) }
            }
            .joinToString("\n").trim()
        
        // 1. Remove prompt fragments only if they appear at the START (first 100 chars)
        val leadingFragments = listOf(
            "### Response:",
            "### Instruction:",
            "Answer the question using ONLY",
            "Context from documents:",
            "Based on the context above",
            "Answer based ONLY on these passages",
            "Question:",
            "Context:",
            "Answer briefly and directly:"
        )
        
        for (fragment in leadingFragments) {
            val searchArea = cleaned.take(100)
            val idx = searchArea.indexOf(fragment, ignoreCase = true)
            if (idx != -1) {
                val afterFragment = cleaned.substring(idx + fragment.length).trim()
                if (afterFragment.isNotEmpty()) {
                    cleaned = afterFragment
                }
            }
        }
        
        // 2. Remove "Answer:" or "A:" prefix if present at start
        if (cleaned.startsWith("Answer:", ignoreCase = true)) {
            cleaned = cleaned.substring(7).trim()
        }
        if (cleaned.startsWith("A:", ignoreCase = true)) {
            cleaned = cleaned.substring(2).trim()
        }
        
        // 3. Truncate at TRAILING prompt echoes (LLM repeats the prompt after answer)
        val trailingMarkers = listOf(
            "### System:", "### Instruction:", "### Context:", "### Question:", "### Response:",
            "### System ", "### Instruction ",
            "Q:", "Question:", "Context:", "Answer briefly",
            "Answer based ONLY", "Based on the context",
            "Answer the question using",
            "This app is for answering from the user's PDFs",
            "Do not repeat the same phrase"
        )
        for (marker in trailingMarkers) {
            // Only look after the first 30 chars (the answer should have started by then)
            val searchStart = 30.coerceAtMost(cleaned.length)
            val idx = cleaned.indexOf(marker, startIndex = searchStart, ignoreCase = true)
            if (idx != -1) {
                cleaned = cleaned.substring(0, idx).trim()
            }
        }
        
        // 4. Detect and truncate repetition loops
        cleaned = truncateRepetition(cleaned)
        
        // 5. Limit response length (prevent very long outputs)
        if (cleaned.length > 800) {
            val lastSentenceEnd = cleaned.take(800).lastIndexOfAny(charArrayOf('.', '!', '?'))
            if (lastSentenceEnd > 200) {
                cleaned = cleaned.take(lastSentenceEnd + 1)
            } else {
                cleaned = cleaned.take(800) + "..."
            }
        }
        
        return cleaned
    }
    
    /**
     * Detects repeating phrases ANYWHERE in the text and truncates before repetition starts.
     * Much more aggressive pattern detection for broken LLM outputs.
     */
    private fun truncateRepetition(text: String): String {
        if (text.length < 30) return text

        // 0. Detect repeated sentence/phrase (e.g. "1 January'da ne yapabiliriz?" repeated)
        val sentenceEnds = listOf(". ", "? ", "! ", ".\n", "?\n", "!\n")
        var pos = 0
        while (pos < text.length - 20) {
            val nextEnd = sentenceEnds.mapNotNull { text.indexOf(it, pos).takeIf { i -> i >= 0 } }.minOrNull() ?: break
            val sentence = text.substring(pos, nextEnd + 1).trim()
            if (sentence.length in 15..200) {
                val second = text.indexOf(sentence, nextEnd + 2)
                if (second != -1) {
                    Log.w(TAG, "Detected repeated sentence: '${sentence.take(50)}...'")
                    return text.substring(0, second).trim()
                }
            }
            pos = nextEnd + 1
        }

        // Split into lines for line-based repetition detection
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // 1. Check for any line that repeats 3+ times
        val lineCounts = lines.groupingBy { it }.eachCount()
        val repeatedLine = lineCounts.entries.find { it.value >= 3 && it.key.length >= 3 }
        if (repeatedLine != null) {
            Log.w(TAG, "Detected repeated line (${repeatedLine.value}x): '${repeatedLine.key.take(30)}...'")
            // Return only unique lines up to first repeat
            val result = StringBuilder()
            val seen = mutableSetOf<String>()
            for (line in lines) {
                if (line in seen && line == repeatedLine.key) break
                if (line !in seen) {
                    seen.add(line)
                    result.append(line).append(" ")
                }
            }
            return result.toString().trim().ifEmpty { lines.first() }
        }
        
        // 2. Check for short patterns (like `"The` repeating)
        for (patternLen in 3..20) {
            var i = 0
            while (i < text.length - patternLen * 3) {
                val pattern = text.substring(i, i + patternLen)
                if (pattern.isBlank()) { i++; continue }
                
                var count = 1
                var j = i + patternLen
                while (j + patternLen <= text.length && text.substring(j, j + patternLen) == pattern) {
                    count++
                    j += patternLen
                }
                
                if (count >= 3) {
                    Log.w(TAG, "Detected short pattern (${count}x): '${pattern.take(20)}'")
                    return text.substring(0, i).trim().ifEmpty { pattern.trim() }
                }
                i++
            }
        }
        
        // 3. Check for word-based repetition (same word 5+ times in a row)
        val words = text.split(Regex("\\s+"))
        for (i in 0 until words.size - 4) {
            val word = words[i]
            if (word.length < 2) continue
            var count = 1
            for (j in i + 1 until words.size) {
                if (words[j] == word) count++ else break
            }
            if (count >= 5) {
                Log.w(TAG, "Detected word repetition (${count}x): '$word'")
                return words.take(i).joinToString(" ").ifEmpty { word }
            }
        }
        
        return text
    }

    /**
     * Formats conversation history for the prompt: "User: ... Assistant: ..." per turn.
     * Keeps total length within [maxChars]: if over limit, drops oldest (user, assistant) pairs
     * so we never cut mid-message (plan §4: "mesaj sayısını azalt").
     */
    private fun formatConversationHistory(pairs: List<Pair<String, String>>, maxChars: Int): String {
        if (pairs.isEmpty()) return ""
        var usedPairs = pairs
        var result = buildString {
            for ((user, assistant) in usedPairs) {
                if (isNotEmpty()) append("\n\n")
                append("User: ").append(user.trim())
                append("\nAssistant: ").append(assistant.trim())
            }
        }
        while (result.length > maxChars && usedPairs.size > 1) {
            usedPairs = usedPairs.drop(1)
            result = buildString {
                for ((user, assistant) in usedPairs) {
                    if (isNotEmpty()) append("\n\n")
                    append("User: ").append(user.trim())
                    append("\nAssistant: ").append(assistant.trim())
                }
            }
        }
        if (result.length > maxChars) result = result.takeLast(maxChars).trimStart()
        return result
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
