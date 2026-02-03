package com.example.pocketscholar.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscholar.data.Document
import com.example.pocketscholar.data.DocumentRepository
import com.example.pocketscholar.data.PdfChunkExtractor
import com.example.pocketscholar.data.copyPdfToAppStorage
import com.example.pocketscholar.data.db.AppDatabase
import com.example.pocketscholar.data.db.ChunkEntity
import com.example.pocketscholar.data.db.EmbeddingBlob
import com.example.pocketscholar.engine.EmbeddingEngine
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
    val lastChunkCount: Int? = null,
    val chunksCleared: Boolean = false,
    /** Shown when embedding model failed to load; all chunk embeddings will be zeros. */
    val embeddingWarning: String? = null
)

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DocumentRepository(application)
    private val chunkDao = AppDatabase.getInstance(application).chunkDao()
    private val embeddingEngine = EmbeddingEngine(application)

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
        // Warm up embedding engine on background; warn if model failed (all zeros).
        viewModelScope.launch(Dispatchers.IO) {
            val warmup = embeddingEngine.embed("warmup")
            if (!embeddingEngine.isModelLoaded() || warmup.all { it == 0f }) {
                _uiState.update {
                    it.copy(embeddingWarning = "Embedding model yüklenemedi. Chunk'lar sıfır vektörle kaydedilecek; semantic arama çalışmaz. Logcat'te 'EmbeddingEngine' veya 'TFSentencepieceTokenizeOp' arayın. README'deki uyumlu modeli kullanın.")
                }
            }
        }
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
                val count = withContext(Dispatchers.IO) {
                    val chunks = PdfChunkExtractor.extract(doc.path, doc.id)
                    val entities = chunks.map { chunk ->
                        val embedding = embeddingEngine.embed(chunk.text)
                        ChunkEntity(
                            id = chunk.id,
                            documentId = chunk.documentId,
                            pageNumber = chunk.pageNumber,
                            chunkIndex = chunk.chunkIndex,
                            text = chunk.text,
                            embedding = EmbeddingBlob.floatArrayToBytes(embedding)
                        )
                    }
                    chunkDao.deleteByDocumentId(docId)
                    chunkDao.insertAll(entities)
                    entities.size
                }
                _uiState.update {
                    it.copy(processingDocumentId = null, lastChunkCount = count)
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

    /** Deletes all chunks from the vector store (e.g. to re-process with a new embedding model). */
    fun clearAllChunks() {
        viewModelScope.launch(Dispatchers.IO) {
            chunkDao.deleteAll()
            _uiState.update { it.copy(chunksCleared = true) }
        }
    }

    fun clearChunksClearedFlag() {
        _uiState.update { it.copy(chunksCleared = false) }
    }

    fun clearEmbeddingWarning() {
        _uiState.update { it.copy(embeddingWarning = null) }
    }
}
