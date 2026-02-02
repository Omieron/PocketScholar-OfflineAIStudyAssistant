package com.example.pocketscholar.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscholar.data.Document
import com.example.pocketscholar.data.DocumentRepository
import com.example.pocketscholar.data.PdfChunkExtractor
import com.example.pocketscholar.data.copyPdfToAppStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingDocumentId: String? = null,
    val lastChunkCount: Int? = null
)

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DocumentRepository(application)

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        _uiState.update { it.copy(documents = repo.getDocuments()) }
    }

    fun addDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val doc = copyPdfToAppStorage(getApplication(), uri)
            _uiState.update {
                it.copy(isLoading = false)
            }
            doc?.let {
                repo.addDocument(it)
                loadDocuments()
            } ?: _uiState.update { state ->
                state.copy(error = "Could not save PDF")
            }
        }
    }

    fun removeDocument(id: String) {
        repo.removeDocument(id)
        loadDocuments()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun processDocument(docId: String) {
        val doc = repo.getDocuments().find { it.id == docId } ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(processingDocumentId = docId, error = null, lastChunkCount = null)
            }
            try {
                val chunks = withContext(Dispatchers.IO) {
                    PdfChunkExtractor.extract(doc.path, doc.id)
                }
                _uiState.update {
                    it.copy(
                        processingDocumentId = null,
                        lastChunkCount = chunks.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        processingDocumentId = null,
                        error = "PDF işlenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearLastChunkCount() {
        _uiState.update { it.copy(lastChunkCount = null) }
    }
}
