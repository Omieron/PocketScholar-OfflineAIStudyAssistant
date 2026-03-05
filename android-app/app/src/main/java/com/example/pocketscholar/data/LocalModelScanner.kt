package com.example.pocketscholar.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Cihazda zaten bulunan GGUF modellerini tarar.
 *
 * Aranan konumlar:
 * - app files: context.filesDir (özellikle model.gguf ve *.gguf)
 * - public Downloads klasörü: /sdcard/Download/*.gguf
 */
object LocalModelScanner {

    private const val TAG = "LocalModelScanner"

    enum class LocationType {
        APP_FILES,
        DOWNLOADS
    }

    data class LocalModelFile(
        val name: String,
        val path: String,
        val sizeBytes: Long,
        val locationType: LocationType
    )

    /**
     * Cihazda erişilebilir GGUF model dosyalarını döner.
     */
    fun findAvailableModels(context: Context): List<LocalModelFile> {
        val result = mutableListOf<LocalModelFile>()

        // 1) Uygulama dosyaları (filesDir) — RAG README'de önerilen konum
        scanDirectory(
            dir = context.filesDir,
            locationType = LocationType.APP_FILES,
            out = result
        )

        // 2) Ortak Downloads klasörü — kullanıcıların tarayıcıdan indirdiği modeller
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null) {
            scanDirectory(
                dir = downloadsDir,
                locationType = LocationType.DOWNLOADS,
                out = result
            )
        } else {
            Log.w(TAG, "Downloads directory is null; skipping external scan.")
        }

        return result.sortedBy { it.name.lowercase() }
    }

    private fun scanDirectory(
        dir: File,
        locationType: LocationType,
        out: MutableList<LocalModelFile>
    ) {
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles { file ->
            file.isFile && file.name.endsWith(".gguf", ignoreCase = true)
        } ?: return

        for (file in files) {
            out.add(
                LocalModelFile(
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    locationType = locationType
                )
            )
        }
    }
}

*/