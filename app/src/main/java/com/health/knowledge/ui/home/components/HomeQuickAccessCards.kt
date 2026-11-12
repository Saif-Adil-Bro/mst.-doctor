package com.health.knowledge.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Two-column responsive quick access cards:
 * Card 1: হাদীস প্ল্যানার (with next alarm time and quick navigation)
 * Card 2: প্রেগন্যান্সি (আগে ও পরে, with mother-baby silhouette watermark)
 */
@Composable
fun HomeQuickAccessCards(
    activeHabitCount: Int,
    nextPostCountdownText: String?,
    onOpenHabitPlanner: () -> Unit,
    onOpenPregnancyPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Card: হ্যাবিট প্ল্যানার
        Card(
            onClick = onOpenHabitPlanner,
            modifier = Modifier
                .weight(1f)
                .height(98.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F3)),
            border = onePxBorder(Color(0xFFC8E6C9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                // Top Row: Clock icon + Forward Arrow button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF006B3C),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = "হ্যাবিট প্ল্যানার",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE0F2E9),
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "যান",
                                tint = Color(0xFF006B3C),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Title and Subtitle grouped closely together without gap
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "হ্যাবিট প্ল্যানার",
                        color = Color(0xFF1F2937),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val rawAlarmSubtitle = when {
                        !nextPostCountdownText.isNullOrBlank() -> nextPostCountdownText
                        else -> "সকাল ০৮:০০"
                    }
                    val alarmSubtitle = rawAlarmSubtitle.removePrefix("পরের অ্যালার্ম: ")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF006B3C),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = alarmSubtitle,
                            color = Color(0xFF006B3C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        // Right Card: প্রেগন্যান্সি
        Card(
            onClick = onOpenPregnancyPlanner,
            modifier = Modifier
                .weight(1f)
                .height(98.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2ED)),
            border = onePxBorder(Color(0xFFFFD9CC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mother-Child silhouette watermark in bottom right
                MotherChildWatermark(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = 38.dp, height = 38.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    // Top Row: Pregnancy icon + Forward Arrow button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF004D2C),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🤰", fontSize = 15.sp)
                            }
                        }

                        Surface(
                            color = Color(0xFFFFE0D6),
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "যান",
                                    tint = Color(0xFFD84315),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Title and Subtitle grouped closely together without gap
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "প্রেগন্যান্সি",
                            color = Color(0xFF1F2937),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "আগে ও পরে",
                            color = Color(0xFF6B7280),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws a soft silhouette watermark of mother and baby in the bottom right corner.
 */
@Composable
private fun MotherChildWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Mother head
        drawCircle(
            color = Color(0xFFD84315).copy(alpha = 0.12f),
            radius = w * 0.18f,
            center = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.35f)
        )

        // Mother body profile arc
        val motherBody = Path().apply {
            moveTo(w * 0.60f, h)
            cubicTo(
                w * 0.55f, h * 0.70f,
                w * 0.65f, h * 0.50f,
                w * 0.75f, h * 0.50f
            )
            cubicTo(
                w * 0.90f, h * 0.50f,
                w * 0.98f, h * 0.75f,
                w, h
            )
            close()
        }
        drawPath(motherBody, color = Color(0xFFD84315).copy(alpha = 0.10f))

        // Baby head
        drawCircle(
            color = Color(0xFFD84315).copy(alpha = 0.12f),
            radius = w * 0.11f,
            center = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.62f)
        )
    }
}
