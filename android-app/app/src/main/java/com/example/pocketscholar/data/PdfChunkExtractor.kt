package com.example.pocketscholar.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.UUID

private const val CHUNK_SIZE_CHARS = 400
private const val CHUNK_OVERLAP_CHARS = 80

/**
 * Extracts text from a PDF and splits it into chunks for RAG.
 *
 * What it does:
 * 1. Opens the PDF at [path] (PdfBox-Android).
 * 2. Extracts text page by page via PDFTextStripper.getText.
 * 3. Splits each page into chunks of ~400 chars with 80-char overlap so the same
 *    sentence is not cut; overlap helps retrieval catch context that spans boundaries.
 * 4. Breaks at the last space in range so we don't cut mid-word.
 *
 * Smaller chunks (400) = more precise embedding match to the question.
 * Overlap (80) = less risk of "soru yanlış yerden yakalanıyor" (answer not split across chunks).
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
                val pageChunks = chunkTextWithOverlap(pageText, CHUNK_SIZE_CHARS, CHUNK_OVERLAP_CHARS)
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
     * Splits [text] into pieces of at most [size] characters with [overlap] between
     * consecutive chunks. Prefers to break at a space so we don't cut mid-word.
     * Overlap reduces the chance that the answer to a question is split across two chunks.
     */
    private fun chunkTextWithOverlap(text: String, size: Int, overlap: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + size).coerceAtMost(text.length)
            if (end < text.length) {
                val lastSpace = text.lastIndexOf(' ', end - 1)
                if (lastSpace >= start) end = lastSpace + 1
            }
            result.add(text.substring(start, end).trim())
            if (end >= text.length) break
            start = (end - overlap).coerceIn(0, text.length)
        }
        return result.filter { it.isNotEmpty() }
    }
}
