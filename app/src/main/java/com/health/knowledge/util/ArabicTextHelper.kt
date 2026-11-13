package com.health.knowledge.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.ui.theme.SolaimanLipi
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Intelligent detector and formatter for Arabic and Bengali multilingual typography.
 * Enforces:
 * 1. Standalone Arabic components/paragraphs: centered alignment & RTL text direction.
 * 2. Mixed Arabic + Bengali or standard Bengali: start alignment (LTR) in accordance with Bengali formatting.
 */
object ArabicTextHelper {
    // Unicode ranges for Arabic scripts (0600-06FF, 0750-077F, 08A0-08FF, FB50-FDFF, FE70-FEFF)
    private val ARABIC_REGEX = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    private val BENGALI_REGEX = Regex("[\\u0980-\\u09FF]")

    /**
     * Checks if a paragraph or component contains Arabic characters and DOES NOT contain Bengali characters.
     */
    fun isStandaloneArabic(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val hasArabic = ARABIC_REGEX.containsMatchIn(trimmed)
        val hasBengali = BENGALI_REGEX.containsMatchIn(trimmed)
        return hasArabic && !hasBengali
    }

    /**
     * Checks if text contains any Arabic characters.
     */
    fun containsArabic(text: String): Boolean {
        return ARABIC_REGEX.containsMatchIn(text)
    }

    /**
     * Splits multi-line body content into individual paragraphs for smart per-paragraph directional styling.
     */
    fun splitIntoParagraphs(content: String): List<String> {
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        return normalized.split(Regex("\n{2,}"))
            .flatMap { block ->
                val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.any { isStandaloneArabic(it) }) {
                    lines
                } else {
                    listOf(block.trim())
                }
            }
            .filter { it.isNotBlank() }
    }
}

/**
 * Standalone Arabic Component Renderer with Center Alignment, RTL layout, and Islamic styling.
 */
@Composable
fun ArabicStandaloneBlock(
    arabicText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    textColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = arabicText.trim(),
                modifier = Modifier.fillMaxWidth(),
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.6f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = textColor,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    textDirection = TextDirection.Rtl
                )
            )
        }
    }
}

/**
 * Smart paragraph-by-paragraph renderer for article bodies.
 * Automatically identifies standalone Arabic paragraphs and renders them centered with RTL,
 * while maintaining Bengali LTR start alignment for standard Bengali and mixed bilingual text.
 */
@Composable
fun SmartMultilingualBody(
    content: String,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val paragraphs = remember(content) {
        ArabicTextHelper.splitIntoParagraphs(content)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        paragraphs.forEach { paragraph ->
            if (ArabicTextHelper.isStandaloneArabic(paragraph)) {
                ArabicStandaloneBlock(
                    arabicText = paragraph,
                    fontSize = (fontSizeSp + 3f).sp
                )
            } else {
                Text(
                    text = ArticleTextFormatter.formatBengaliParagraphs(paragraph),
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = SolaimanLipi,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.50f).sp,
                    color = textColor,
                    textAlign = TextAlign.Start,
                    style = TextStyle(
                        fontFamily = SolaimanLipi,
                        textDirection = TextDirection.Ltr
                    )
                )
            }
        }
    }
}
