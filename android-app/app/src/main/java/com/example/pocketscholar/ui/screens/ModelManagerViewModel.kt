package com.example.pocketscholar.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscholar.data.AvailableModels
import com.example.pocketscholar.data.DownloadStatus
import com.example.pocketscholar.data.ModelInfo
import com.example.pocketscholar.data.ModelRepository
import com.example.pocketscholar.engine.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelManagerUiState(
    val models: List<ModelInfo> = emptyList(),
    val downloadedModelIds: Set<String> = emptySet(),
    val activeModelId: String? = null,
    val downloadingModelId: String? = null,
    val downloadProgress: Int = 0,  // 0-100
    val isLoadingModel: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ModelManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    private val modelRepo = ModelRepository(application)

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                models = modelRepo.getAvailableModels(),
                downloadedModelIds = modelRepo.getDownloadedModelIds(),
                activeModelId = modelRepo.getActiveModelId()
            )
        }
        // Eğer devam eden indirme varsa progress takibini başlat
        val models = modelRepo.getAvailableModels()
        for (model in models) {
            if (modelRepo.isDownloading(model.id)) {
                _uiState.update { it.copy(downloadingModelId = model.id) }
                startProgressPolling(model.id)
                break
            }
        }
    }

    fun downloadModel(modelInfo: ModelInfo) {
        if (_uiState.value.downloadingModelId != null) {
            _uiState.update { it.copy(error = "Zaten bir indirme devam ediyor.") }
            return
        }

        viewModelScope.launch {
            try {
                val downloadId = withContext(Dispatchers.IO) {
                    modelRepo.downloadModel(modelInfo)
                }
                if (downloadId == -1L) {
                    _uiState.update {
                        it.copy(
                            error = "Bu model zaten indirilmiş.",
                            downloadedModelIds = modelRepo.getDownloadedModelIds()
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        downloadingModelId = modelInfo.id,
                        downloadProgress = 0
                    )
                }
                startProgressPolling(modelInfo.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "İndirme başlatılamadı: ${e.message}") }
            }
        }
    }

    fun cancelDownload() {
        val downloadingId = _uiState.value.downloadingModelId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                modelRepo.cancelDownload(downloadingId)
            }
            _uiState.update {
                it.copy(
                    downloadingModelId = null,
                    downloadProgress = 0
                )
            }
        }
    }

    fun activateModel(modelId: String) {
        _uiState.update { it.copy(isLoadingModel = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val modelPath = modelRepo.getModelPath(modelId)
                        ?: throw IllegalStateException("Model dosyası bulunamadı")

                    // Mevcut modeli kaldır
                    LlamaEngine.unload()
                    // Yeni modeli yükle
                    val success = LlamaEngine.loadModel(modelPath)
                    if (!success) throw IllegalStateException("Model yüklenemedi")

                    modelRepo.setActiveModelId(modelId)
                }
                _uiState.update {
                    it.copy(
                        activeModelId = modelId,
                        isLoadingModel = false,
                        successMessage = "Model başarıyla yüklendi!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        error = "Model yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (modelRepo.getActiveModelId() == modelId) {
                    LlamaEngine.unload()
                }
                modelRepo.deleteModel(modelId)
            }
            _uiState.update {
                it.copy(
                    downloadedModelIds = modelRepo.getDownloadedModelIds(),
                    activeModelId = modelRepo.getActiveModelId()
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun startProgressPolling(modelId: String) {
        viewModelScope.launch {
            while (true) {
                val progress = withContext(Dispatchers.IO) {
                    modelRepo.getDownloadProgress(modelId)
                }

                if (progress == null) {
                    // İndirme bilgisi yok — bitmiş veya kaybolmuş
                    break
                }

                when (progress.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PENDING, DownloadStatus.PAUSED -> {
                        _uiState.update { it.copy(downloadProgress = progress.percent) }
                    }
                    DownloadStatus.COMPLETED -> {
                        _uiState.update {
                            it.copy(
                                downloadingModelId = null,
                                downloadProgress = 100,
                                downloadedModelIds = modelRepo.getDownloadedModelIds(),
                                successMessage = "Model başarıyla indirildi!"
                            )
                        }
                        return@launch
                    }
                    DownloadStatus.FAILED -> {
                        _uiState.update {
                            it.copy(
                                downloadingModelId = null,
                                downloadProgress = 0,
                                error = "İndirme başarısız oldu. Lütfen internet bağlantınızı kontrol edin."
                            )
                        }
                        return@launch
                    }
                }
                delay(800) // Her 800ms'de bir sorgu
            }
        }
    }
}
