package com.example.pocketscholar.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Copies a PDF from [uri] into app storage and returns a [Document], or null on failure.
 */
suspend fun copyPdfToAppStorage(context: Context, uri: Uri): Document? = withContext(Dispatchers.IO) {
    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            cursor.getString(nameIndex) ?: "document_${System.currentTimeMillis()}.pdf"
        } else {
            "document_${System.currentTimeMillis()}.pdf"
        }
    } ?: "document_${System.currentTimeMillis()}.pdf"

    val dir = File(context.filesDir, "documents").also { if (!it.exists()) it.mkdirs() }
    val ext = name.substringAfterLast('.', "pdf")
    val id = UUID.randomUUID().toString()
    val file = File(dir, "${id}.$ext")

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Document(id = id, name = name, path = file.absolutePath)
    } catch (e: Exception) {
        null
    }
}
