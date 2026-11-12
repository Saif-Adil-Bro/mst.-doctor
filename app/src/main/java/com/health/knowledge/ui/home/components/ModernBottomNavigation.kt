package com.health.knowledge.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.ui.theme.onePxBorder

/**
 * Modern Bottom Navigation Bar with 3 destinations:
 * 1. 🏠 হোম
 * 2. 🌿 ভেষজ ও সার্চ
 * 3. 🔖 পোস্ট ও নোট
 *
 * Implements smooth active pill indicator, responsive touch targets,
 * and safe-area padding for Android navigation bar.
 */
@Composable
fun ModernHomeBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldPrimary = Color(0xFF006B3C)
    val inactiveColor = Color(0xFF6B7280)
    val activePillColor = Color(0xFFE0F2E9)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = Color(0x1F000000)),
        color = Color.White,
        border = onePxBorder(Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: হোম
            BottomNavItem(
                label = "হোম",
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home,
                isSelected = activeTab == "home",
                onClick = { onTabSelected("home") },
                activeColor = emeraldPrimary,
                inactiveColor = inactiveColor,
                pillColor = activePillColor,
                modifier = Modifier.weight(1f)
            )

            // Tab 2: ভেষজ ও সার্চ
            BottomNavItem(
                label = "ভেষজ ও সার্চ",
                activeIcon = Icons.Filled.Eco,
                inactiveIcon = Icons.Outlined.Eco,
                isSelected = activeTab == "search",
                onClick = { onTabSelected("search") },
                activeColor = emeraldPrimary,
                inactiveColor = inactiveColor,
                pillColor = activePillColor,
                modifier = Modifier.weight(1f)
            )

            // Tab 3: পোস্ট ও নোট (সেভ ও নোট)
            BottomNavItem(
                label = "পোস্ট ও নোট",
                activeIcon = Icons.Filled.Bookmark,
                inactiveIcon = Icons.Outlined.BookmarkBorder,
                isSelected = activeTab == "bookmarks",
                onClick = { onTabSelected("bookmarks") },
                activeColor = emeraldPrimary,
                inactiveColor = inactiveColor,
                pillColor = activePillColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    pillColor: Color,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "bottom_nav_icon_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Active Pill Indicator behind Icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) pillColor else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) activeIcon else inactiveIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = label,
                color = iconColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
