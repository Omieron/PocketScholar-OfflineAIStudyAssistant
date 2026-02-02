package com.example.pocketscholar.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.UUID

private const val CHUNK_SIZE_CHARS = 500

/**
 * Extracts text from a PDF and splits it into chunks for RAG.
 *
 * What it does:
 * 1. Opens the PDF at [path] (PdfBox-Android).
 * 2. Extracts text page by page via PDFTextStripper.getText.
 * 3. Splits each page text into ~500-char chunks; breaks at the last space in range
 *    so we don't cut mid-word (lastIndexOf(' ', end - 1)).
 * 4. Produces a [Chunk] per piece: documentId, pageNumber, chunkIndex, text.
 *
 * Result: list of chunks to be embedded and stored in the vector store later.
 */
object PdfChunkExtractor {

    fun extract(path: String, documentId: String): List<Chunk> {
        val file = File(path)
        if (!file.exists()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        PDDocument.load(file).use { pdDoc ->
            val pageCount = pdDoc.numberOfPages
            val stripper = PDFTextStripper()

            for (page in 1..pageCount) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(pdDoc) ?: ""
                val pageChunks = chunkText(pageText, CHUNK_SIZE_CHARS)
                pageChunks.forEachIndexed { index, text ->
                    chunks.add(
                        Chunk(
                            id = UUID.randomUUID().toString(),
                            documentId = documentId,
                            pageNumber = page,
                            chunkIndex = chunks.size,
                            text = text.trim()
                        )
                    )
                }
            }
        }
        return chunks
    }

    /**
     * Splits [text] into pieces of at most [size] characters.
     * Prefers to break at a space so we don't cut in the middle of a word:
     * within [start, start+size] we look for the last space and break there.
     */
    private fun chunkText(text: String, size: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + size).coerceAtMost(text.length)
            if (end < text.length) {
                // lastIndexOf(char, fromIndex) = find last space going backward from fromIndex
                val lastSpace = text.lastIndexOf(' ', end - 1)
                if (lastSpace >= start) end = lastSpace + 1
            }
            result.add(text.substring(start, end))
            start = end
        }
        return result
    }
}
