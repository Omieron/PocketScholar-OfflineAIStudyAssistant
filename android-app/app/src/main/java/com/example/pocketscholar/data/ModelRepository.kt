package com.example.pocketscholar.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

private const val TAG = "ModelRepository"
private const val PREFS_NAME = "model_prefs"
private const val KEY_ACTIVE_MODEL_ID = "active_model_id"
private const val KEY_DOWNLOAD_ID_PREFIX = "download_id_"
private const val MODELS_DIR = "models"

/**
 * Model indirme, silme, aktif model yönetimi.
 * Modeller context.getExternalFilesDir(null)/models/ altına kaydedilir.
 */
class ModelRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // ── Model Listesi ──

    fun getAvailableModels(): List<ModelInfo> = AvailableModels.list

    // ── İndirilen Modeller ──

    fun getDownloadedModelIds(): Set<String> {
        val modelsDir = getModelsDir() ?: return emptySet()
        return AvailableModels.list
            .filter { File(modelsDir, it.fileName).exists() }
            .map { it.id }
            .toSet()
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val model = findModel(modelId) ?: return false
        val modelsDir = getModelsDir() ?: return false
        return File(modelsDir, model.fileName).exists()
    }

    fun getModelPath(modelId: String): String? {
        val model = findModel(modelId) ?: return null
        val modelsDir = getModelsDir() ?: return null
        val file = File(modelsDir, model.fileName)
        return if (file.exists()) file.absolutePath else null
    }

    // ── Aktif Model ──

    fun getActiveModelId(): String? = prefs.getString(KEY_ACTIVE_MODEL_ID, null)

    fun setActiveModelId(modelId: String) {
        prefs.edit().putString(KEY_ACTIVE_MODEL_ID, modelId).apply()
    }

    fun getActiveModelPath(): String? {
        val activeId = getActiveModelId() ?: return null
        return getModelPath(activeId)
    }

    /**
     * Aktif bir model yoksa, cihazda mevcut olan herhangi bir modeli bul.
     * Eski findModelFile() mantığını da destekler (model.gguf, Download klasörü).
     */
    fun findAnyAvailableModelPath(): String? {
        // 1) Aktif model varsa onu döndür
        getActiveModelPath()?.let { return it }

        // 2) İndirilmiş modellerden birini seç
        val downloadedIds = getDownloadedModelIds()
        if (downloadedIds.isNotEmpty()) {
            val firstId = downloadedIds.first()
            setActiveModelId(firstId)
            return getModelPath(firstId)
        }

        // 3) Eski yaklaşım: uygulama dizininde veya Download'da model.gguf ara
        val appDir = context.getExternalFilesDir(null)
        if (appDir != null) {
            val f = File(appDir, "model.gguf")
            if (f.isFile) return f.absolutePath
        }
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val f = File(downloadDir, "model.gguf")
        if (f.isFile) return f.absolutePath
        downloadDir.listFiles()?.firstOrNull {
            it.isFile && it.name.endsWith(".gguf", ignoreCase = true)
        }?.let { return it.absolutePath }

        return null
    }

    // ── İndirme ──

    /**
     * Modeli Hugging Face'den indirir. Download ID döner.
     */
    fun downloadModel(modelInfo: ModelInfo): Long {
        val modelsDir = getModelsDir() ?: throw IllegalStateException("Models dizini oluşturulamadı")

        // Eğer dosya zaten varsa indirme
        val targetFile = File(modelsDir, modelInfo.fileName)
        if (targetFile.exists()) {
            Log.w(TAG, "Model zaten mevcut: ${modelInfo.fileName}")
            return -1L
        }

        val request = DownloadManager.Request(Uri.parse(modelInfo.downloadUrl))
            .setTitle("${modelInfo.name} indiriliyor")
            .setDescription("PocketScholar AI model indirmesi")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "$MODELS_DIR/${modelInfo.fileName}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadId = downloadManager.enqueue(request)
        // Download ID'yi kaydet
        prefs.edit().putLong("${KEY_DOWNLOAD_ID_PREFIX}${modelInfo.id}", downloadId).apply()
        Log.d(TAG, "İndirme başladı: ${modelInfo.name}, downloadId=$downloadId")
        return downloadId
    }

    /**
     * Aktif indirme durumunu döner.
     * @return Pair(indirilmiş bayt, toplam bayt) veya null (indirme yok/bitti)
     */
    fun getDownloadProgress(modelId: String): DownloadProgress? {
        val downloadId = prefs.getLong("${KEY_DOWNLOAD_ID_PREFIX}$modelId", -1L)
        if (downloadId == -1L) return null

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                return when (status) {
                    DownloadManager.STATUS_RUNNING ->
                        DownloadProgress(downloaded, total, DownloadStatus.DOWNLOADING)
                    DownloadManager.STATUS_PENDING ->
                        DownloadProgress(0, total, DownloadStatus.PENDING)
                    DownloadManager.STATUS_PAUSED ->
                        DownloadProgress(downloaded, total, DownloadStatus.PAUSED)
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        clearDownloadId(modelId)
                        DownloadProgress(total, total, DownloadStatus.COMPLETED)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        clearDownloadId(modelId)
                        DownloadProgress(0, 0, DownloadStatus.FAILED)
                    }
                    else -> null
                }
            }
        }
        return null
    }

    fun isDownloading(modelId: String): Boolean {
        val progress = getDownloadProgress(modelId)
        return progress != null && (progress.status == DownloadStatus.DOWNLOADING || progress.status == DownloadStatus.PENDING)
    }

    fun cancelDownload(modelId: String) {
        val downloadId = prefs.getLong("${KEY_DOWNLOAD_ID_PREFIX}$modelId", -1L)
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
            clearDownloadId(modelId)
            // Yarım kalan dosyayı sil
            val model = findModel(modelId)
            if (model != null) {
                val modelsDir = getModelsDir()
                if (modelsDir != null) {
                    File(modelsDir, model.fileName).delete()
                }
            }
        }
    }

    // ── Silme ──

    fun deleteModel(modelId: String): Boolean {
        val model = findModel(modelId) ?: return false
        val modelsDir = getModelsDir() ?: return false
        val file = File(modelsDir, model.fileName)
        val deleted = file.delete()
        // Aktif model silindiyse temizle
        if (deleted && getActiveModelId() == modelId) {
            prefs.edit().remove(KEY_ACTIVE_MODEL_ID).apply()
        }
        Log.d(TAG, "Model silindi: ${model.fileName}, başarılı=$deleted")
        return deleted
    }

    // ── Yardımcı ──

    private fun findModel(modelId: String): ModelInfo? =
        AvailableModels.list.find { it.id == modelId }

    private fun getModelsDir(): File? {
        val appDir = context.getExternalFilesDir(null) ?: return null
        val modelsDir = File(appDir, MODELS_DIR)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    private fun clearDownloadId(modelId: String) {
        prefs.edit().remove("${KEY_DOWNLOAD_ID_PREFIX}$modelId").apply()
    }
}

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: DownloadStatus
) {
    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
}

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
}
