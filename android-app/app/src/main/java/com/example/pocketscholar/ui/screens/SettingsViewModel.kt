package com.example.pocketscholar.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscholar.data.LocalModelScanner
import com.example.pocketscholar.data.LocalModelScanner.LocalModelFile
import com.example.pocketscholar.engine.EmbeddingEngine
import com.example.pocketscholar.engine.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ayarlar ekranı için ViewModel:
 * - LLM (GGUF) modelinin yüklenme ve seçim durumu
 * - Embedding (TFLite) modelinin kurulum ve yüklenme durumu
 * - Cihazda hazır bulunan GGUF dosyalarının listesi
 */
data class SettingsUiState(
    val llmModelStatus: LlmModelStatus = LlmModelStatus.NotLoaded,
    val availableLlmModels: List<LocalModelFile> = emptyList(),
    val embeddingStatus: EmbeddingStatus = EmbeddingStatus.NotFound,
    val isScanning: Boolean = false,
    val isLoadingModel: Boolean = false,
    val error: String? = null
)

sealed interface LlmModelStatus {
    data class Loaded(val path: String, val sizeBytes: Long) : LlmModelStatus
    data class Error(val message: String) : LlmModelStatus
    data object NotLoaded : LlmModelStatus
}

sealed interface EmbeddingStatus {
    data class Loaded(val dimension: Int, val source: String) : EmbeddingStatus
    data class Error(val message: String) : EmbeddingStatus
    data object NotFound : EmbeddingStatus
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val embeddingEngine = EmbeddingEngine(application)

    init {
        refreshStatus()
    }

    /**
     * LLM ve embedding model durumunu yeniler.
     * - LLM: LlamaEngine + persisted modelPath
     * - Embedding: EmbeddingEngine.isModelLoaded() + modelInfo()
     */
    fun refreshStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val appContext = getApplication<Application>()

            // --- LLM durumu ---
            val persistedPath = LlamaEngine.currentModelPath
                ?: LlamaEngine.getPersistedModelPath(appContext)

            val llmStatus = if (persistedPath.isNullOrBlank()) {
                LlmModelStatus.NotLoaded
            } else {
                val file = File(persistedPath)
                if (file.exists() && file.isFile) {
                    LlmModelStatus.Loaded(path = persistedPath, sizeBytes = file.length())
                } else {
                    LlmModelStatus.Error(
                        "Kayıtlı model yolu bulunamadı veya artık mevcut değil: $persistedPath"
                    )
                }
            }

            // --- Embedding durumu ---
            val embeddingStatus = if (embeddingEngine.isModelLoaded()) {
                val info = embeddingEngine.modelInfo()
                if (info != null) {
                    EmbeddingStatus.Loaded(
                        dimension = info.dimension,
                        source = "Asset (${info.fileName})"
                    )
                } else {
                    EmbeddingStatus.Error("Embedding modeli yüklü görünüyor ama model bilgisi okunamadı.")
                }
            } else {
                EmbeddingStatus.NotFound
            }

            _uiState.update {
                it.copy(
                    llmModelStatus = llmStatus,
                    embeddingStatus = embeddingStatus
                )
            }
        }
    }

    /**
     * Cihazda hazır bulunan GGUF modellerini tarar ve state'e yazar.
     */
    fun scanForModels() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            val models = withContext(Dispatchers.IO) {
                LocalModelScanner.findAvailableModels(getApplication())
            }
            _uiState.update {
                it.copy(
                    availableLlmModels = models,
                    isScanning = false
                )
            }
        }
    }

    /**
     * Settings ekranından seçilen yerel GGUF modelini yükler.
     */
    fun selectAndLoadModel(modelFile: LocalModelFile) {
        if (_uiState.value.isLoadingModel) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModel = true, error = null) }
            val success = withContext(Dispatchers.IO) {
                LlamaEngine.loadModel(getApplication(), modelFile.path)
            }
            if (success) {
                _uiState.update {
                    it.copy(
                        llmModelStatus = LlmModelStatus.Loaded(
                            path = modelFile.path,
                            sizeBytes = modelFile.sizeBytes
                        ),
                        isLoadingModel = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        llmModelStatus = LlmModelStatus.Error("Model yüklenemedi: ${modelFile.name}"),
                        isLoadingModel = false,
                        error = "Model yüklenemedi. Dosya bozuk olabilir veya JNI başlatılamadı."
                    )
                }
            }
        }
    }

    /**
     * Aktif modeli kaldırır ve persisted bilgiyi temizler.
     */
    fun clearModel() {
        viewModelScope.launch(Dispatchers.IO) {
            val appContext = getApplication<Application>()
            LlamaEngine.unloadAndReset()
            LlamaEngine.clearPersistedModel(appContext)
            _uiState.update {
                it.copy(llmModelStatus = LlmModelStatus.NotLoaded)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

