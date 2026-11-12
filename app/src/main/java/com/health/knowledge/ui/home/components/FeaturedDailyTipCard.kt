package com.health.knowledge.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.health.knowledge.R
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Featured Daily Health / Tibbe Nabawi Tip Card
 * Designed with a premium light mint/cream container, balanced text and image layout,
 * time and category badges, and dynamic CTA button.
 */
@Composable
fun PremiumDailyTipCard(
    article: Article,
    reminderEnabled: Boolean,
    reminderTimeText: String,
    onReadClick: () -> Unit,
    onNextTipClick: () -> Unit,
    onToggleReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTibbe = article.id < 0 || article.categories.contains("তিব্বে নববী")
    val badgeTitle = if (isTibbe) "🌿 আজকের তিব্বে নববী টিপস" else "💡 আজকের স্বাস্থ্য টিপস"
    val formattedTime = if (reminderTimeText.isNotBlank()) {
        reminderTimeText
    } else {
        "সকাল ০৮:০০ (AM)"
    }

    // Extract ingredient or medicine name
    val ingredientText = when {
        article.medicines.isNotEmpty() -> article.medicines.first()
        article.categories.isNotEmpty() -> article.categories.first().replace("_", " ")
        else -> "প্রাকৃতিক ভেষজ"
    }

    val ctaText = if (article.medicines.isNotEmpty()) {
        "🌿 ${article.medicines.take(2).joinToString(", ")} →"
    } else {
        "বিস্তারিত পড়ুন →"
    }

    val cardBg = Color(0xFFF1F8F3) // Soft mint/cream background
    val borderColor = Color(0xFFC8E6C9)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x1A004D2C)
            )
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = onePxBorder(borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Badge: তিব্বে নববী টিপস
                Surface(
                    color = Color(0xFF006B3C),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = badgeTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Right Badge: সময় (e.g. সকাল ০৮:০০ (AM))
                Surface(
                    color = Color(0xFFE0F2E9),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable(onClick = onToggleReminder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF006B3C),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formattedTime,
                            color = Color(0xFF006B3C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Content + Image Split Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Headline, Ingredient, Snippet, and CTA button
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Headline
                    Text(
                        text = article.title,
                        color = Color(0xFF004D2C),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )

                    // Herb / Ingredient tag
                    Text(
                        text = "🌱 ভেষজ/উপাদান: $ingredientText",
                        color = Color(0xFF2E8B57),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Short snippet
                    Text(
                        text = article.content.replace("\n", " ").trim(),
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // Bottom CTA Pill
                    Surface(
                        color = Color(0xFFE0F2E9),
                        shape = RoundedCornerShape(20.dp),
                        border = onePxBorder(Color(0xFFB8DEC5)),
                        modifier = Modifier.clickable(onClick = onReadClick)
                    ) {
                        Text(
                            text = ctaText,
                            color = Color(0xFF006B3C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Right: Botanical / Herb Product Image
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(onePxBorder(Color(0xFFB8DEC5)), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (article.photoPath.isNotBlank()) {
                        AsyncImage(
                            model = article.photoPath,
                            contentDescription = article.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // High quality authentic Kohl Surma herbal jar asset
                        Image(
                            painter = painterResource(id = R.drawable.tip_kohl_surma),
                            contentDescription = "ভেষজ পথ্য",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Refresh Button in bottom corner to rotate tip
                    IconButton(
                        onClick = onNextTipClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(topStart = 10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "পরবর্তী টিপস",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
