package com.example.pocketscholar.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a text chunk with its embedding vector (BLOB).
 * Used for vector search: query embedding vs chunk embeddings (cosine similarity).
 */
@Entity(
    tableName = "chunks",
    indices = [Index(value = ["documentId"])]
)
data class ChunkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageNumber: Int,
    val chunkIndex: Int,
    val text: String,
    /** Embedding vector as BLOB (e.g. 384 floats = 1536 bytes). */
    val embedding: ByteArray
) {
    override fun equals(other: Any?) = other is ChunkEntity && id == other.id && embedding.contentEquals(other.embedding)
    override fun hashCode() = 31 * id.hashCode() + embedding.contentHashCode()
}
