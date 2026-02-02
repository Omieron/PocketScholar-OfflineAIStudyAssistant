package com.example.pocketscholar.data

/**
 * A text chunk from a PDF page, for embedding and vector search later.
 * documentId, pageNumber, chunkIndex identify position; text is the content.
 */
data class Chunk(
    val id: String,
    val documentId: String,
    val pageNumber: Int,
    val chunkIndex: Int,
    val text: String
)
