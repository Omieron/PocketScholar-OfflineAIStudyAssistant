package com.example.pocketscholar.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val role: String, // "user" | "assistant"
    val text: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            // Placeholder: RAG/LLM will be called here once the model is wired in
            val placeholderReply = ChatMessage(
                id = "assistant_${System.currentTimeMillis()}",
                role = "assistant",
                text = "Cevap burada görünecek. (Model + RAG bağlanınca gerçek yanıt gelecek.)"
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + placeholderReply,
                    isThinking = false
                )
            }
        }
    }
}
