package com.example.pocketscholar.engine

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel

private const val MODEL_ASSET = "embedding_model.tflite"
const val EMBEDDING_DIM = 384 // INT32 model default; auto-detected when model loads
private const val TAG = "EmbeddingEngine"

/**
 * Embeds text into a fixed-size vector for vector search.
 * Uses Google Play Services TFLite (InterpreterApi).
 * If the model expects INT32 input (token IDs), uses BertWordPieceTokenizer with vocab.txt.
 */
class EmbeddingEngine(private val context: Context) {

    private var dim: Int = EMBEDDING_DIM
    private var interpreter: InterpreterApi? = null
    private val modelFile: File? = copyAssetToCache(MODEL_ASSET)
    private var initFailed: Boolean = false

    private var useInt32Input: Boolean = false
    private var maxSeqLen: Int = 128
    private var tokenizer: BertWordPieceTokenizer? = null

    init {
        if (modelFile == null) {
            Log.w(TAG, "No $MODEL_ASSET in assets; using zero vectors.")
        } else {
            TfLite.initialize(context)
                .addOnSuccessListener { tryCreateInterpreter() }
                .addOnFailureListener { e ->
                    initFailed = true
                    Log.e(TAG, "TfLite.initialize failed", e)
                }
        }
    }

    private fun tryCreateInterpreter() {
        val file = modelFile ?: return
        try {
            val buffer = file.inputStream().use { fis ->
                fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            }
            val options = InterpreterApi.Options().setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
            val interp = InterpreterApi.create(buffer, options)
            
            // Auto-detect dimension from output tensor
            if (interp.outputTensorCount > 0) {
                val outputTensor = interp.getOutputTensor(0)
                val shape = outputTensor.shape()
                dim = if (shape.size >= 2) shape[shape.size - 1].toInt() else shape[0].toInt()
            }

            // Detect INT32 input (token IDs) vs STRING input
            if (interp.inputTensorCount > 0) {
                val input0 = interp.getInputTensor(0)
                useInt32Input = (input0.dataType() == DataType.INT32)
                val shape = input0.shape()
                if (shape.isNotEmpty()) maxSeqLen = shape.last().toInt().coerceAtLeast(1)
                if (useInt32Input) {
                    tokenizer = BertWordPieceTokenizer(context)
                    if (!tokenizer!!.isLoaded())
                        Log.w(TAG, "INT32 model but vocab.txt missing; embeddings will fail until vocab is added.")
                    else
                        Log.i(TAG, "TFLite INT32 model; using WordPiece tokenizer, maxSeqLen=$maxSeqLen")
                }
            }
            
            interpreter = interp
            Log.i(TAG, "TFLite model loaded via Play Services; dim=$dim")
        } catch (e: Exception) {
            initFailed = true
            Log.e(TAG, "Failed to load TFLite model", e)
        }
    }

    private fun ensureInterpreter() {
        if (interpreter != null || initFailed) return
        try {
            Tasks.await(TfLite.initialize(context))
            if (interpreter == null) tryCreateInterpreter()
        } catch (e: Exception) {
            initFailed = true
            Log.e(TAG, "ensureInterpreter failed", e)
        }
    }

    fun embeddingDimension(): Int = dim

    fun isModelLoaded(): Boolean = interpreter != null

    fun embed(text: String): FloatArray {
        if (text.isBlank()) return FloatArray(dim) { 0f }
        ensureInterpreter()
        val interp = interpreter
        if (interp == null) {
            return FloatArray(dim) { 0f }
        }
        return try {
            val result = runInference(interp, text)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Embedding inference failed", e)
            FloatArray(dim) { 0f }
        }
    }

    private fun runInference(interp: InterpreterApi, text: String): FloatArray {
        val numInputs = interp.inputTensorCount
        val outputTensor = interp.getOutputTensor(0)
        val shape = outputTensor.shape()
        val outDim = if (shape.size >= 2) shape[shape.size - 1].toInt() else shape[0].toInt()
        val output = Array(1) { FloatArray(outDim) }

        val inputs: Array<Any> = if (useInt32Input && tokenizer != null && tokenizer!!.isLoaded()) {
            val (inputIds, actualLen) = tokenizer!!.tokenizeToIds(text, maxSeqLen)
            val inputIds2D = Array(1) { inputIds }
            if (numInputs >= 3) {
                val mask2D = Array(1) { tokenizer!!.attentionMask(actualLen, maxSeqLen) }
                val segment2D = Array(1) { tokenizer!!.segmentIds(maxSeqLen) }
                arrayOf(inputIds2D, mask2D, segment2D)
            } else {
                arrayOf(inputIds2D)
            }
        } else {
            when {
                numInputs >= 3 -> arrayOf(arrayOf(text), arrayOf(""), arrayOf(""))
                else -> arrayOf(arrayOf(text))
            }
        }

        interp.runForMultipleInputsOutputs(inputs, mapOf(0 to output))
        return output[0]
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








