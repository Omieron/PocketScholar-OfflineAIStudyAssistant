package com.example.pocketscholar.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscholar.data.Document
import com.example.pocketscholar.data.DocumentRepository
import com.example.pocketscholar.data.db.AppDatabase
import com.example.pocketscholar.data.VectorStoreRepository
import com.example.pocketscholar.engine.EmbeddingEngine
import com.example.pocketscholar.engine.LlamaEngine
import com.example.pocketscholar.engine.RagService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String,
    val role: String, // "user" | "assistant"
    val text: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    // Document selection
    val availableDocuments: List<Document> = emptyList(),
    val selectedDocumentIds: Set<String> = emptySet()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val documentRepo = DocumentRepository(application)
    private val vectorStore = VectorStoreRepository(
        chunkDao = AppDatabase.getInstance(application).chunkDao(),
        embeddingEngine = EmbeddingEngine(application)
    )

    init {
        loadAvailableDocuments()
    }

    /** Load documents from repository */
    fun loadAvailableDocuments() {
        val docs = documentRepo.getDocuments()
        _uiState.update { it.copy(availableDocuments = docs) }
    }

    /** Toggle document selection for filtering */
    fun toggleDocumentSelection(docId: String) {
        _uiState.update { state ->
            val newSelection = if (docId in state.selectedDocumentIds) {
                state.selectedDocumentIds - docId
            } else {
                state.selectedDocumentIds + docId
            }
            state.copy(selectedDocumentIds = newSelection)
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            text = text
        )
        
        // Get selected document IDs (null means search all)
        val selectedIds = _uiState.value.selectedDocumentIds.toList().ifEmpty { null }
        
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            val replyId = "assistant_${System.currentTimeMillis()}"
            val replyText = withContext(Dispatchers.IO) {
                try {
                    // If no model is loaded, do not call RAG at all — no sources, no junk output
                    if (!LlamaEngine.isModelLoaded()) {
                        return@withContext "Model henüz yüklenmedi. Cevap üretmek için Ayarlar > LLM Model bölümünden cihazınızdaki bir GGUF dosyasını seçip modeli yükleyin."
                    }
                    val result = RagService.ask(
                        query = text,
                        vectorStore = vectorStore,
                        documentIds = selectedIds
                    )
                    var answer = result.answer
                    val isModelNotLoaded = answer.contains("Model not loaded", ignoreCase = true) || answer.contains("Call loadModel() first")
                    if (isModelNotLoaded) {
                        answer = "Model henüz yüklenmedi. Cevap üretmek için Ayarlar > LLM Model bölümünden cihazınızdaki bir GGUF dosyasını seçip modeli yükleyin."
                    }
                    val sourceLine = if (!isModelNotLoaded && result.sources.isNotEmpty()) {
                        val pages = result.sources.map { it.pageNumber }.distinct().sorted()
                        "\n\nKaynak: sayfa ${pages.joinToString(", ")}"
                    } else ""
                    answer + sourceLine
                } catch (e: Exception) {
                    "Cevap oluşturulamadı: ${e.message ?: "bilinmeyen hata"}"
                }
            }
            val assistantMsg = ChatMessage(id = replyId, role = "assistant", text = replyText)
            _uiState.update {
                it.copy(
                    messages = it.messages + assistantMsg,
                    isThinking = false
                )
            }
        }
    }
}

