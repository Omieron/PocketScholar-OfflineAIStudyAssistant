package com.example.pocketscholar.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Kalıcı ayarlar: LLM model yolu, son yükleme zamanı, embedding model kaynağı.
 * Tüm ViewModel'ler (SettingsViewModel, ModelManagerViewModel vb.) bu repodan okuyup yazabilir.
 */
class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── LLM (GGUF) ──
    fun getLlmModelPath(): String? {
        var path = prefs.getString(KEY_LLM_MODEL_PATH, null)
        if (path == null) {
            // Eski LlamaEngine prefs'ten tek seferlik taşıma
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LEGACY_KEY_MODEL_PATH, null)
            if (!legacy.isNullOrBlank()) {
                setLlmModelPath(legacy)
                path = legacy
            }
        }
        return path
    }

    fun setLlmModelPath(path: String?) {
        prefs.edit().putString(KEY_LLM_MODEL_PATH, path).apply()
    }

    fun getLlmModelLastLoadedAt(): Long = prefs.getLong(KEY_LLM_MODEL_LAST_LOADED_AT, 0L)

    fun setLlmModelLastLoadedAt(timestampMs: Long) {
        prefs.edit().putLong(KEY_LLM_MODEL_LAST_LOADED_AT, timestampMs).apply()
    }

    fun clearLlmModel() {
        prefs.edit()
            .remove(KEY_LLM_MODEL_PATH)
            .remove(KEY_LLM_MODEL_LAST_LOADED_AT)
            .apply()
    }

    // ── Embedding (TFLite) kaynağı: "asset" | "file" (ileride dış dosya desteği için)
    fun getEmbeddingModelSource(): String =
        prefs.getString(KEY_EMBEDDING_MODEL_SOURCE, VALUE_EMBEDDING_SOURCE_ASSET) ?: VALUE_EMBEDDING_SOURCE_ASSET

    fun setEmbeddingModelSource(source: String) {
        prefs.edit().putString(KEY_EMBEDDING_MODEL_SOURCE, source).apply()
    }

    companion object {
        private const val PREFS_NAME = "pocketscholar_settings"
        private const val LEGACY_PREFS_NAME = "llama_engine_prefs"
        private const val LEGACY_KEY_MODEL_PATH = "model_path"
        const val KEY_LLM_MODEL_PATH = "llm_model_path"
        const val KEY_LLM_MODEL_LAST_LOADED_AT = "llm_model_last_loaded_at"
        const val KEY_EMBEDDING_MODEL_SOURCE = "embedding_model_source"

        const val VALUE_EMBEDDING_SOURCE_ASSET = "asset"
        const val VALUE_EMBEDDING_SOURCE_FILE = "file"
    }
}
