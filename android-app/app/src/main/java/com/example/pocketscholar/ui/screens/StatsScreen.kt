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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Paul Rand İstatistikler —
 * "The role of the designer is that of a good, thoughtful host."
 *
 * Minimal, geometrik kutu kartlar. Sayılar büyük, etiketler küçük.
 */

private val RandBlack = Color(0xFF1A1A1A)
private val RandWhite = Color(0xFFF8F7F4)
private val RandTeal = Color(0xFF2D9D94)
private val RandGrey = Color(0xFF9E9E9E)
private val RandLightGrey = Color(0xFFE8E6E1)

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RandWhite)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // ── Başlık ──
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(RandTeal, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "İstatistik.",
            style = MaterialTheme.typography.displayMedium,
            color = RandBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sistem durumu ve performans metrikleri.",
            style = MaterialTheme.typography.bodyMedium,
            color = RandGrey
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Metrik Kartları ──
        RandStatBox(label = "BELLEK", value = "—", accent = RandTeal)
        Spacer(modifier = Modifier.height(16.dp))
        RandStatBox(label = "PİL", value = "—", accent = RandBlack)
        Spacer(modifier = Modifier.height(16.dp))
        RandStatBox(label = "MODEL", value = "—", accent = RandTeal)

        Spacer(modifier = Modifier.height(40.dp))

        // Yakında etiketi
        Text(
            text = "YAKINDA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            ),
            color = RandGrey.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Metrik Kutusu — Rand: Geometrik kutu, büyük değer, küçük etiket
// ═══════════════════════════════════════════════════════════════
@Composable
private fun RandStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol: renk göstergesi
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = RandGrey
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = RandBlack
            )
        }
    }
}
