package com.example.pocketscholar.data.db

import java.nio.ByteBuffer

/**
 * Converts FloatArray (embedding vector) to/from BLOB for Room storage.
 * 384 floats = 1536 bytes.
 */
object EmbeddingBlob {

    fun floatArrayToBytes(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * 4)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun bytesToFloatArray(value: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(value)
        return FloatArray(value.size / 4) { buffer.getFloat() }
    }
}
