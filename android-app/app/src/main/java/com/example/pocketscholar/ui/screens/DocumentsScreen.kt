package com.example.pocketscholar.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketscholar.data.Document

/*
 * Paul Rand Belgeler —
 * "Simplicity is not the goal. It is the by-product of a good idea."
 *
 * İnce çizgi kartlar, cesur metin, geometrik aksan.
 * FAB: keskin köşeli siyah kare.
 */

private val RandBlack = Color(0xFF1A1A1A)
private val RandWhite = Color(0xFFF8F7F4)
private val RandTeal = Color(0xFF2D9D94)
private val RandGrey = Color(0xFF9E9E9E)
private val RandLightGrey = Color(0xFFE8E6E1)
private val RandRed = Color(0xFFD84315)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addDocument(it) }
    }

    val errorMessage = uiState.error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    val lastChunkCount = uiState.lastChunkCount
    LaunchedEffect(lastChunkCount) {
        lastChunkCount?.let { snackbarHostState.showSnackbar("$it chunk çıkarıldı"); viewModel.clearLastChunkCount() }
    }
    val chunksCleared = uiState.chunksCleared
    LaunchedEffect(chunksCleared) {
        if (chunksCleared) { snackbarHostState.showSnackbar("Tüm chunk'lar silindi."); viewModel.clearChunksClearedFlag() }
    }

    val embeddingWarning = uiState.embeddingWarning

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RandWhite,
        floatingActionButton = {
            // FAB: Rand tarzı — keskin siyah kare
            FloatingActionButton(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                containerColor = RandBlack,
                contentColor = RandWhite,
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "PDF ekle",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Başlık ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                // Geometrik aksan
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(RandTeal, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Belgeler.",
                    style = MaterialTheme.typography.displayMedium,
                    color = RandBlack
                )

                // Chunk temizle butonu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.clearAllChunks() }) {
                        Text(
                            "CHUNK TEMİZLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = RandGrey
                        )
                    }
                }
            }

            // ── Embedding Uyarısı ──
            if (embeddingWarning != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .border(1.dp, RandRed, RoundedCornerShape(2.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = RandRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = embeddingWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = RandRed,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )
                    IconButton(
                        onClick = { viewModel.clearEmbeddingWarning() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat",
                            modifier = Modifier.size(16.dp),
                            tint = RandRed
                        )
                    }
                }
            }

            // ── Boş Durum ──
            if (uiState.documents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.isLoading) "Kaydediliyor…" else "Henüz belge yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RandGrey,
                        textAlign = TextAlign.Center
                    )
                    if (!uiState.isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PDF yükleyip soru sormaya başlayın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RandGrey.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ── Belge Listesi ──
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        val isThisProcessing = uiState.processingDocumentId == doc.id
                        RandDocumentItem(
                            doc = doc,
                            isProcessing = isThisProcessing,
                            chunkProgress = if (isThisProcessing) uiState.processingChunkProgress else null,
                            onProcess = { viewModel.processDocument(doc.id) },
                            onDelete = { viewModel.removeDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Belge Kartı — Rand: ince alt çizgi, cesur isim, minimal butonlar
// ═══════════════════════════════════════════════════════════════
@Composable
private fun RandDocumentItem(
    doc: Document,
    isProcessing: Boolean,
    chunkProgress: Pair<Int, Int>?,
    onProcess: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Küçük teal kare gösterge
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(RandTeal, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))

            // Dosya adı
            Text(
                text = doc.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = RandBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Aksiyonlar
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = RandTeal
                )
            } else {
                // İşle butonu
                TextButton(onClick = onProcess) {
                    Text(
                        "İŞLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = RandTeal
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Kaldır",
                    modifier = Modifier.size(16.dp),
                    tint = RandGrey
                )
            }
        }

        // Embedding ilerleme
        if (isProcessing && chunkProgress != null) {
            val (current, total) = chunkProgress
            Text(
                text = "EMBEDDING: $current / $total",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp
                ),
                color = RandGrey,
                modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)
            )
        }

        // İnce ayraç çizgi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RandLightGrey)
        )
    }
}
