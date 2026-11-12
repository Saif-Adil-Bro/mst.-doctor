package com.health.knowledge.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Premium Dark Emerald Green Islamic Header with watermark arch,
 * app branding, notification icon with unread badge, and floating search bar.
 */
@Composable
fun HomePremiumHeader(
    totalCount: Int,
    searchQuery: String,
    searchPlaceholder: String = "রোগ, উপাদান বা বিষয় লিখুন...",
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    isAdminMode: Boolean,
    onAdminManageClick: () -> Unit,
    onAdminLockClick: () -> Unit,
    onTitleMultiTap: () -> Unit,
    showSearchBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val emeraldTop = Color(0xFF005832)
    val emeraldBottom = Color(0xFF004627)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(emeraldTop, emeraldBottom)
                )
            )
            .statusBarsPadding()
    ) {
        // Islamic geometric arch watermark in background
        IslamicArchWatermark(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 220.dp, height = 140.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 18.dp)
        ) {
            // Top Bar Row: Logo + App Name + Subtitle + Notification Bell & Admin Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Logo + App Name + Subtitle (with 5-click hidden admin detection preserved)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onTitleMultiTap()
                        }
                ) {
                    // Leaf / Botanical Icon
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌿", fontSize = 22.sp)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "স্বাস্থ্য জ্ঞান",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                            if (isAdminMode) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFD4AF37),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color(0xFF00381E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$totalCount টি স্বাস্থ্যতথ্য • অফলাইন ও অনলাইন",
                            color = Color(0xFFD0EBD8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Right: Action Buttons (Notification Bell with Unread Dot, Admin Quick Toggles)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdminMode) {
                        FilledTonalButton(
                            onClick = onAdminManageClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF006B3C)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ম্যানেজ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onAdminLockClick,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "লক",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(onClick = onNotificationClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "সেটিংস",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (showSearchBar) {
                Spacer(Modifier.height(14.dp))

                // Modern Rounded Search Bar
                ModernHomeSearchBar(
                    query = searchQuery,
                    placeholder = searchPlaceholder,
                    onQueryChange = onSearchQueryChange,
                    onClear = onClearSearch,
                    onFilterClick = onFilterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Prominent white rounded search bar with search icon, clear button,
 * subtle vertical divider, and filter icon.
 */
@Composable
fun ModernHomeSearchBar(
    query: String,
    placeholder: String = "রোগ, উপাদান বা বিষয় লিখুন...",
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x33000000)),
        shape = RoundedCornerShape(26.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "সার্চ",
                tint = Color(0xFF006B3C),
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF1F2937),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "মুছুন",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Subtle vertical divider before filter button
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFFE0E0E0))
            )

            // Filter button
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "ফিল্টার ও ভেষজ তালিকা",
                    tint = Color(0xFF006B3C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Draws an elegant Islamic dome / arch silhouette watermark.
 */
@Composable
fun IslamicArchWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val archPath = Path().apply {
            // Center main dome
            moveTo(width * 0.45f, height)
            lineTo(width * 0.45f, height * 0.45f)
            cubicTo(
                width * 0.45f, height * 0.20f,
                width * 0.65f, height * 0.02f,
                width * 0.75f, 0f
            )
            cubicTo(
                width * 0.85f, height * 0.02f,
                width * 1.05f, height * 0.20f,
                width * 1.05f, height * 0.45f
            )
            lineTo(width * 1.05f, height)
            close()
        }

        // Inner decorative arch
        val innerArch = Path().apply {
            moveTo(width * 0.52f, height)
            lineTo(width * 0.52f, height * 0.50f)
            cubicTo(
                width * 0.52f, height * 0.28f,
                width * 0.68f, height * 0.12f,
                width * 0.75f, height * 0.08f
            )
            cubicTo(
                width * 0.82f, height * 0.12f,
                width * 0.98f, height * 0.28f,
                width * 0.98f, height * 0.50f
            )
            lineTo(width * 0.98f, height)
            close()
        }

        drawPath(
            path = archPath,
            color = Color.White.copy(alpha = 0.06f),
            style = Stroke(width = 2.dp.toPx())
        )

        drawPath(
            path = innerArch,
            color = Color.White.copy(alpha = 0.04f)
        )
    }
}
