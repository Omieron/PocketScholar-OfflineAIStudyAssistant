package com.example.pocketscholar.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

private const val MODEL_ASSET = "embedding_model.tflite"
const val EMBEDDING_DIM = 512
private const val TAG = "EmbeddingEngine"

/**
 * Embeds text into a fixed-size vector for vector search.
 * Placeholder: returns zero vectors until a TFLite embedding model is added to assets.
 * Pipeline (chunk -> embed -> save to Room) runs; vector search will work once real embeddings are used.
 *
 * To enable real embeddings: add Universal Sentence Encoder TFLite to assets as [MODEL_ASSET]
 * and integrate TFLite TextEmbedder (org.tensorflow:tensorflow-lite-task-text) in [embed].
 */
class EmbeddingEngine(private val context: Context) {

    private var dim: Int = EMBEDDING_DIM

    init {
        val modelFile = copyAssetToCache(MODEL_ASSET)
        if (modelFile != null) {
            Log.d(TAG, "Model file found; TFLite integration TODO. Using zero vectors.")
        } else {
            Log.w(TAG, "No $MODEL_ASSET in assets; using zero vectors. Add USE Lite .tflite for real embeddings.")
        }
    }

    fun embeddingDimension(): Int = dim

    /**
     * Returns embedding vector for [text]. Placeholder: zero vector of [embeddingDimension].
     */
    fun embed(text: String): FloatArray {
        if (text.isBlank()) return FloatArray(dim) { 0f }
        return FloatArray(dim) { 0f }
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
            null
        }
    }
}
