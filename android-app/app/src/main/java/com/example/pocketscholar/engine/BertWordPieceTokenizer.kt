package com.example.pocketscholar.engine

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "BertWordPieceTokenizer"
private const val VOCAB_ASSET = "vocab.txt"

/**
 * BERT-style WordPiece tokenizer for INT32 TFLite embedding models.
 * Load vocab.txt from assets (one token per line; line index = token id).
 * For 384-dim models (e.g. MiniLM), use vocab from bert-base-uncased or the model's repo.
 */
class BertWordPieceTokenizer(context: Context) {

    private val vocab: List<String> = loadVocab(context)
    private val tokenToId: Map<String, Int> = vocab.mapIndexed { index, token -> token to index }.toMap()

    private val padId: Int get() = tokenToId["[PAD]"] ?: 0
    private val unkId: Int get() = tokenToId["[UNK]"] ?: (tokenToId["[unk]"] ?: 100)
    private val clsId: Int get() = tokenToId["[CLS]"] ?: 101
    private val sepId: Int get() = tokenToId["[SEP]"] ?: 102

    /** Returns (input_ids, actual_length_before_padding). */
    fun tokenizeToIds(text: String, maxSeqLen: Int): Pair<IntArray, Int> {
        val tokens = mutableListOf<String>()
        tokens.add("[CLS]")
        for (word in basicTokenize(text)) {
            tokens.addAll(wordpieceTokenize(word))
        }
        tokens.add("[SEP]")
        val ids = tokens.map { tokenToId[it] ?: unkId }.toIntArray()
        val actualLen = ids.size.coerceAtMost(maxSeqLen)
        val padded = padOrTruncate(ids, maxSeqLen, padId)
        return padded to actualLen
    }

    fun attentionMask(actualLen: Int, maxSeqLen: Int): IntArray {
        return IntArray(maxSeqLen) { if (it < actualLen) 1 else 0 }
    }

    fun segmentIds(maxSeqLen: Int): IntArray = IntArray(maxSeqLen) { 0 }

    fun isLoaded(): Boolean = vocab.isNotEmpty()

    private fun loadVocab(context: Context): List<String> {
        return try {
            context.assets.open(VOCAB_ASSET).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readLines()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No $VOCAB_ASSET in assets; INT32 embedding will fail until vocab is added.")
            emptyList()
        }
    }

    private fun basicTokenize(text: String): List<String> {
        val cleaned = text.trim().lowercase()
        if (cleaned.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < cleaned.length) {
            when {
                cleaned[i].isLetterOrDigit() || cleaned[i] == '\'' -> {
                    val start = i
                    while (i < cleaned.length && (cleaned[i].isLetterOrDigit() || cleaned[i] == '\'')) i++
                    out.add(cleaned.substring(start, i))
                }
                else -> i++
            }
        }
        return out
    }

    private fun wordpieceTokenize(word: String): List<String> {
        if (word in tokenToId) return listOf(word)
        val result = mutableListOf<String>()
        var remaining = word
        var isFirst = true
        while (remaining.isNotEmpty()) {
            var found = false
            for (len in remaining.length downTo 1) {
                val sub = if (isFirst) remaining.take(len) else "##" + remaining.take(len)
                if (tokenToId.containsKey(sub)) {
                    result.add(sub)
                    remaining = remaining.drop(len)
                    isFirst = false
                    found = true
                    break
                }
            }
            if (!found) {
                result.add("[UNK]")
                break
            }
        }
        return result.ifEmpty { listOf("[UNK]") }
    }

    private fun padOrTruncate(ids: IntArray, maxLen: Int, padValue: Int): IntArray {
        return when {
            ids.size >= maxLen -> ids.copyOf(maxLen)
            else -> ids + IntArray(maxLen - ids.size) { padValue }
        }
    }
}
