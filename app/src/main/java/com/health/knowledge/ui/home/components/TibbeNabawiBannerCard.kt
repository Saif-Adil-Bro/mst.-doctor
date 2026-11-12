package com.health.knowledge.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Tibbe Nabawi Horizontal Promotional Banner Card
 * Displays dynamic actual count badge, botanical icon, and subtitle.
 */
@Composable
fun TibbeNabawiBannerCard(
    count: Int,
    onOpenTibbeNabawi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldPrimary = Color(0xFF006B3C)
    val cardBg = Color(0xFFF1F8F3)
    val borderColor = Color(0xFFC8E6C9)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x14004D2C))
            .clickable(onClick = onOpenTibbeNabawi),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = onePxBorder(borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botanical Leaf Box
                Surface(
                    color = Color(0xFFE0F2E9),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🌿", fontSize = 22.sp)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "তিব্বে নববী",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF1F2937)
                        )

                        // Dynamic actual count pill badge
                        Surface(
                            color = emeraldPrimary,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "${count}টি পণ্য",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "হাদীস ও সুন্নাহভিত্তিক স্বাস্থ্য, ভেষজ ও রোগের চিকিৎসা",
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Arrow Button
            Surface(
                color = Color(0xFFE0F2E9),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "তিব্বে নববী দেখুন",
                        tint = emeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
