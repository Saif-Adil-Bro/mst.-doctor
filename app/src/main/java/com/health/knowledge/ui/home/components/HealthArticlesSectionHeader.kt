package com.health.knowledge.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Section Heading + Modern Sort Chips (✨ নতুন, 🔥 জনপ্রিয়, 🏆 পুরাতন)
 */
@Composable
fun HealthArticlesSectionHeader(
    title: String,
    currentSort: String,
    onSortChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldPrimary = Color(0xFF006B3C)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section Title with weight to prevent pushing chips off screen on 360dp devices
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp)
        )

        // Filter / Sort Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sortOptions = listOf(
                Triple("newest", "✨", "নতুন"),
                Triple("popular", "🔥", "জনপ্রিয়"),
                Triple("oldest", "🏆", "পুরাতন")
            )

            for ((key, emoji, label) in sortOptions) {
                val isSelected = currentSort == key

                Surface(
                    onClick = { onSortChange(key) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) emeraldPrimary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else onePxBorder(MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(text = emoji, fontSize = 11.sp)
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
