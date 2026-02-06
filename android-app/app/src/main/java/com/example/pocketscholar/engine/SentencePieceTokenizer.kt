package com.example.pocketscholar.engine

import android.content.Context
import android.util.Log
import com.sentencepiece.Model
import com.sentencepiece.Scoring
import com.sentencepiece.SentencePieceAlgorithm
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

private const val TAG = "SentencePieceTokenizer"
private const val MODEL_ASSET = "sentencepiece.model"

/**
 * SentencePiece tokenizer for EmbeddingGemma and similar models.
 * Converts text strings to INT32 token IDs for TFLite models.
 * Uses SentencePiece4J (pure Java implementation, no JNI).
 */
class SentencePieceTokenizer(private val context: Context) {

    private var model: Model? = null
    private var algorithm: SentencePieceAlgorithm? = null
    private var isInitialized: Boolean = false

    init {
        try {
            val modelFile = copyAssetToCache(MODEL_ASSET)
            if (modelFile != null && modelFile.exists()) {
                model = Model.parseFrom(Paths.get(modelFile.absolutePath))
                algorithm = SentencePieceAlgorithm(true, Scoring.HIGHEST_SCORE)
                isInitialized = true
                Log.i(TAG, "SentencePiece tokenizer loaded successfully")
            } else {
                Log.w(TAG, "No $MODEL_ASSET found in assets")
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError loading SentencePiece model. Device heap may be too small. Consider using a STRING-input embedding model instead.", e)
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SentencePiece tokenizer", e)
            isInitialized = false
        }
    }

    fun isLoaded(): Boolean = isInitialized && model != null && algorithm != null

    /**
     * Tokenizes text and returns (token_ids, actual_length).
     * Adds special tokens if needed (e.g., <s> and </s> for some models).
     */
    fun tokenizeToIds(text: String, maxSeqLen: Int): Pair<IntArray, Int> {
        if (!isLoaded() || text.isBlank()) {
            return IntArray(maxSeqLen) { 0 } to 0
        }

        return try {
            val tokenList = model!!.encodeNormalized(text, algorithm!!)
            val tokens = tokenList.map { it.toInt() }.toIntArray()
            val actualLen = tokens.size.coerceAtMost(maxSeqLen)
            val padded = padOrTruncate(tokens, maxSeqLen, 0) // 0 is typically pad token
            padded to actualLen
        } catch (e: Exception) {
            Log.e(TAG, "Tokenization failed", e)
            IntArray(maxSeqLen) { 0 } to 0
        }
    }

    fun attentionMask(actualLen: Int, maxSeqLen: Int): IntArray {
        return IntArray(maxSeqLen) { if (it < actualLen) 1 else 0 }
    }

    fun segmentIds(maxSeqLen: Int): IntArray = IntArray(maxSeqLen) { 0 }

    private fun padOrTruncate(ids: IntArray, maxLen: Int, padValue: Int): IntArray {
        return when {
            ids.size >= maxLen -> ids.copyOf(maxLen)
            else -> ids + IntArray(maxLen - ids.size) { padValue }
        }
    }

    private fun copyAssetToCache(assetName: String): File? {
        return try {
            context.assets.open(assetName).use { input ->
                val out = File(context.cacheDir, assetName)
                FileOutputStream(out).use { output ->
                    input.copyTo(output)
                }
                out
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy $assetName to cache", e)
            null
        }
    }
}
