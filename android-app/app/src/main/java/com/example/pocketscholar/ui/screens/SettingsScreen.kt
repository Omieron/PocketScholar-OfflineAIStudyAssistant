package com.example.pocketscholar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketscholar.data.LocalModelScanner

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
    ) {
    val uiState by viewModel.uiState.collectAsState()
    val showModelPicker = remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            LlmModelCard(
                state = uiState,
                onScan = { viewModel.scanForModels() },
                onSelectClick = { showModelPicker.value = true },
                onClear = { viewModel.clearModel() }
            )

            EmbeddingModelCard(state = uiState)
        }
    }

    if (showModelPicker.value) {
        ModelPickerDialog(
            availableModels = uiState.availableLlmModels,
            onDismiss = { showModelPicker.value = false },
            onSelect = { file ->
                viewModel.selectAndLoadModel(file)
                showModelPicker.value = false
            }
        )
    }
}

@Composable
private fun LlmModelCard(
    state: SettingsUiState,
    onScan: () -> Unit,
    onSelectClick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null
                )
                Text(
                    text = "LLM Model (GGUF)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            val statusText = when (val s = state.llmModelStatus) {
                is LlmModelStatus.Loaded -> {
                    val name = s.path.substringAfterLast('/')
                    val mb = s.sizeBytes / (1024 * 1024)
                    "Yüklü: $name – ${mb} MB"
                }
                is LlmModelStatus.Error -> s.message
                LlmModelStatus.NotLoaded -> "Model yüklü değil"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onScan,
                    enabled = !state.isScanning
                ) {
                    Text(if (state.isScanning) "Taranıyor..." else "Cihazda Tara")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSelectClick,
                    enabled = state.availableLlmModels.isNotEmpty()
                ) {
                    Text("Model Seç")
                }

                IconButton(
                    onClick = onClear,
                    enabled = state.llmModelStatus is LlmModelStatus.Loaded
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Modeli kaldır"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddingModelCard(
    state: SettingsUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null
                )
                Text(
                    text = "Embedding Modeli",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            val text = when (val s = state.embeddingStatus) {
                is EmbeddingStatus.Loaded ->
                    "Embedding modeli yüklü (dim: ${s.dimension}, kaynak: ${s.source})"
                is EmbeddingStatus.Error ->
                    "Embedding modeli hatası: ${s.message}"
                EmbeddingStatus.NotFound ->
                    "Embedding modeli bulunamadı. PDF aramalarında semantik benzerlik kullanılamaz."
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = { /* TODO: show README_RAG summary dialog */ }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Detaylı kurulum rehberi")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerDialog(
    availableModels: List<LocalModelScanner.LocalModelFile>,
    onDismiss: () -> Unit,
    onSelect: (LocalModelScanner.LocalModelFile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cihazdaki modeller") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (availableModels.isEmpty()) {
                    Text("Cihazda .gguf uzantılı model dosyası bulunamadı.")
                } else {
                    availableModels.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val mb = file.sizeBytes / (1024 * 1024)
                                Text(
                                    text = "${file.locationType} • ${mb} MB",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(onClick = { onSelect(file) }) {
                                Text("Seç")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

