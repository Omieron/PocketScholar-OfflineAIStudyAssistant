package com.example.pocketscholar.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketscholar.data.ModelInfo
import com.example.pocketscholar.data.ModelTier

/*
 * Paul Rand felsefesi:
 * ─ "Simplicity is not the goal. It is the by-product of a good idea and modest expectations."
 * ─ Cesur geometrik formlar, sınırlı renk, güçlü tipografi, bol boşluk.
 * ─ Her öğe bir amaç taşır; dekoratif hiçbir şey yok.
 */

// ── Rand Renk Paleti ── ──────────────────────────────────────────
// Siyah + Beyaz + tek bir vurgu rengi (Teal) — Rand'ın IBM logosu gibi
private val RandBlack = Color(0xFF1A1A1A)
private val RandWhite = Color(0xFFF8F7F4)   // Sıcak beyaz (kağıt hissi)
private val RandAccent = Color(0xFF2D9D94)  // Teal — tek vurgu rengi
private val RandAccentLight = Color(0xFFE0F5F3)
private val RandGrey = Color(0xFF9E9E9E)
private val RandLightGrey = Color(0xFFEEECE8)
private val RandDanger = Color(0xFFD84315)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    // Mesajlar
    val error = uiState.error
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    val success = uiState.successMessage
    LaunchedEffect(success) {
        success?.let { snackbarHostState.showSnackbar(it); viewModel.clearSuccessMessage() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RandWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Başlık Bloğu ──
            item { RandHeader() }

            // ── Model Listesi — tier'a göre ──
            val modelsByTier = uiState.models.groupBy { it.tier }
            for (tier in ModelTier.entries) {
                val models = modelsByTier[tier] ?: continue

                item { TierDivider(tier) }

                items(models, key = { it.id }) { model ->
                    RandModelCard(
                        model = model,
                        isDownloaded = model.id in uiState.downloadedModelIds,
                        isActive = model.id == uiState.activeModelId,
                        isDownloading = model.id == uiState.downloadingModelId,
                        downloadProgress = if (model.id == uiState.downloadingModelId) uiState.downloadProgress else 0,
                        isLoadingModel = uiState.isLoadingModel && model.id == uiState.activeModelId,
                        canDownload = uiState.downloadingModelId == null,
                        onDownload = { viewModel.downloadModel(model) },
                        onCancelDownload = { viewModel.cancelDownload() },
                        onActivate = { viewModel.activateModel(model.id) },
                        onDelete = { viewModel.deleteModel(model.id) },
                        onInfoClick = { uriHandler.openUri(model.huggingFaceUrl) }
                    )
                }
            }

            // Alt boşluk
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Başlık — Rand tarzı: cesur tipografi, geometrik aksan
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun RandHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Geometrik aksan — küçük kare
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(RandAccent, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Model\nSeç.",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            ),
            color = RandBlack
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cihazına uygun modeli indir, etkinleştir.",
            style = MaterialTheme.typography.bodyMedium.copy(
                letterSpacing = 0.sp
            ),
            color = RandGrey
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Tier Ayraç — ince çizgi + etiket (Rand: az öğe, çok anlam)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TierDivider(tier: ModelTier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 8.dp)
    ) {
        // İnce çizgi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RandBlack.copy(alpha = 0.12f))
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = tier.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 11.sp
            ),
            color = RandGrey
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Model Kartı — Rand: sade dikdörtgen, cesur tipografi, tek aksan rengi
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun RandModelCard(
    model: ModelInfo,
    isDownloaded: Boolean,
    isActive: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    isLoadingModel: Boolean,
    canDownload: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onInfoClick: () -> Unit
) {
    // Aktif kart: sol kenarında teal aksan çizgisi
    val borderModifier = if (isActive) {
        Modifier.border(
            width = 3.dp,
            color = RandAccent,
            shape = RoundedCornerShape(2.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(borderModifier)
                .background(
                    if (isActive) RandAccentLight else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
                .padding(20.dp)
                .animateContentSize()
        ) {
            // ── Satır 1: İsim + Durum göstergesi + Info butonu ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Sol: Model ismi (cesur)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = RandBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sağ: Durum göstergesi
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(RandAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // ℹ butonu — minimal daire
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Hugging Face",
                        modifier = Modifier.size(18.dp),
                        tint = RandGrey
                    )
                }
            }

            // ── Satır 2: Açıklama ──
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = RandGrey,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Satır 3: Meta bilgiler — minimal etiketler ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MetaLabel(label = model.sizeLabel)
                MetaLabel(label = model.ramRequirement)
            }

            // ── İndirme progress ──
            if (isDownloading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = RandAccent,
                    trackColor = RandLightGrey
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%$downloadProgress",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = RandBlack
                    )
                    OutlinedButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RandDanger
                        )
                    ) {
                        Text(
                            "İptal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // ── Aksiyon butonları ──
            if (!isDownloading) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isDownloaded) {
                        // İNDİR — siyah dolgu buton (Rand: güçlü kontrast)
                        Button(
                            onClick = onDownload,
                            enabled = canDownload,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RandBlack,
                                contentColor = RandWhite,
                                disabledContainerColor = RandLightGrey,
                                disabledContentColor = RandGrey
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "İNDİR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                    } else if (isActive) {
                        // AKTİF etiketi
                        Text(
                            text = "AKTİF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp
                            ),
                            color = RandAccent,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        // Sil butonu — minimal
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                modifier = Modifier.size(16.dp),
                                tint = RandGrey
                            )
                        }
                    } else {
                        // İndirilmiş, aktif değil
                        if (isLoadingModel) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = RandAccent
                            )
                        } else {
                            // ETKİNLEŞTİR — teal dolgu buton
                            Button(
                                onClick = onActivate,
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RandAccent,
                                    contentColor = RandWhite
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    "ETKİNLEŞTİR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    modifier = Modifier.size(16.dp),
                                    tint = RandGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Meta Etiketi — sade, güçlü
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun MetaLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        ),
        color = RandBlack.copy(alpha = 0.5f)
    )
}
