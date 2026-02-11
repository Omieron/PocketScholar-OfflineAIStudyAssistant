package com.example.pocketscholar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketscholar.data.Document

/*
 * Paul Rand Chat —
 * "Don't try to be original, just try to be good."
 *
 * Siyah/beyaz mesaj blokları, ince çizgiler, cesur tipografi.
 * Kullanıcı mesajı: siyah arka plan, beyaz yazı.
 * AI cevabı: beyaz arka plan, siyah yazı, sol kenarda teal çizgi.
 */

private val RandBlack = Color(0xFF1A1A1A)
private val RandWhite = Color(0xFFF8F7F4)
private val RandTeal = Color(0xFF2D9D94)
private val RandGrey = Color(0xFF9E9E9E)
private val RandLightGrey = Color(0xFFE8E6E1)

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.loadAvailableDocuments() }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RandWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Boş Durum: Rand tarzı — cesur metin ──
            if (uiState.messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Geometrik aksan
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(RandTeal, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Belgelerine\nSor.",
                            style = MaterialTheme.typography.displayLarge,
                            color = RandBlack
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aşağıya yazıp gönderin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RandGrey
                        )
                    }
                }
            }

            // ── Mesajlar ──
            items(uiState.messages, key = { it.id }) { msg ->
                RandMessageBubble(role = msg.role, text = msg.text)
            }

            // ── Düşünüyor göstergesi ──
            if (uiState.isThinking) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RandTeal
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "DÜŞÜNÜYOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp
                            ),
                            color = RandGrey
                        )
                    }
                }
            }
        }

        // ── Belge Seçici ──
        if (uiState.availableDocuments.isNotEmpty()) {
            RandDocumentSelector(
                documents = uiState.availableDocuments,
                selectedIds = uiState.selectedDocumentIds,
                onToggle = viewModel::toggleDocumentSelection,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // ── Giriş Alanı ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Sorunuzu yazın…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RandGrey.copy(alpha = 0.6f)
                    )
                },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(2.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RandBlack,
                    unfocusedBorderColor = RandLightGrey,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = RandTeal,
                    focusedTextColor = RandBlack,
                    unfocusedTextColor = RandBlack
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Gönder butonu — siyah daire
            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier
                    .size(48.dp)
                    .background(RandBlack, RoundedCornerShape(2.dp)),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = RandWhite
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gönder",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Mesaj Balonu — Rand: siyah/beyaz kontrast, geometrik köşeler
// ═══════════════════════════════════════════════════════════════
@Composable
private fun RandMessageBubble(
    role: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val isUser = role == "user"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI: Sol kenarda teal çizgi göstergesi
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(RandTeal)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    color = if (isUser) RandBlack else RandWhite,
                    shape = RoundedCornerShape(2.dp)
                )
                .then(
                    if (!isUser) Modifier.background(Color.Transparent) else Modifier
                )
                .padding(16.dp)
        ) {
            // Rol etiketi
            Text(
                text = if (isUser) "SEN" else "AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = if (isUser) RandGrey else RandTeal,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) RandWhite else RandBlack
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Belge Seçici — minimal yatay chip'ler
// ═══════════════════════════════════════════════════════════════
@Composable
private fun RandDocumentSelector(
    documents: List<Document>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = if (selectedIds.isEmpty()) "TÜM BELGELER" else "SEÇİLİ: ${selectedIds.size}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = RandGrey,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(documents, key = { it.id }) { doc ->
                val isSelected = doc.id in selectedIds
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(doc.id) },
                    label = {
                        Text(
                            text = doc.name.removeSuffix(".pdf"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RandBlack,
                        selectedLabelColor = RandWhite,
                        containerColor = Color.Transparent,
                        labelColor = RandBlack
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = RandLightGrey,
                        selectedBorderColor = RandBlack,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
