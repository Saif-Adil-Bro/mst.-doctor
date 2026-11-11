package com.health.knowledge.ui.tibbenabawi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import com.health.knowledge.ui.theme.onePxBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TibbeNabawiScreen(
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit,
    viewModel: TibbeNabawiViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val onlyBookmarked by viewModel.onlyBookmarked.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val posts by viewModel.filteredPosts.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val bookmarkedCount by viewModel.bookmarkedCount.collectAsState()

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Emerald theme accent for Tibbe Nabawi
    val emeraldPrimary = Color(0xFF1B6B46)
    val emeraldContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "তিব্বে নববী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = emeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "হাদিস ও সুন্নাহ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = emeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ইবনুল কাইয়্যিম (রহ:) • মোট $totalCount টি চিকিৎসা ও পথ্য",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে যান")
                    }
                },
                actions = {
                    // Bookmark filter quick toggle
                    FilledTonalIconToggleButton(
                        checked = onlyBookmarked,
                        onCheckedChange = { viewModel.toggleOnlyBookmarked() },
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (onlyBookmarked) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "বুকমার্ক ফিল্টার সক্রিয়")
                        } else {
                            BadgedBox(
                                badge = {
                                    if (bookmarkedCount > 0) {
                                        Badge { Text(bookmarkedCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "বুকমার্ক ফিল্টার")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Search Bar ───────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        "রোগ, উপাদান বা আরবি নাম দিয়ে খুঁজুন...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "খুঁজুন",
                        tint = emeraldPrimary
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = emeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // ── Category Chips ───────────────────────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "cat_all") {
                    val isSelected = selectedCategory == "সকল"
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelect("সকল") },
                        label = {
                            Text(
                                text = "সকল (${totalCount})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = emeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(categories, key = { "cat_$it" }) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelect(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = emeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ── Active Results Count Bar ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (onlyBookmarked) "সংরক্ষিত পোস্ট (${posts.size})" else "${posts.size} টি পথ্য ও নির্দেশনা পাওয়া গেছে",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (searchQuery.isNotBlank() || selectedCategory != "সকল" || onlyBookmarked) {
                    Text(
                        text = "ফিল্টার রিসেট",
                        fontSize = 12.sp,
                        color = emeraldPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            viewModel.onSearchQueryChange("")
                            viewModel.onCategorySelect("সকল")
                            if (onlyBookmarked) viewModel.toggleOnlyBookmarked()
                        }
                    )
                }
            }

            // ── List of Posts (Smooth Scrolling with Key & ContentType) ────────
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = emeraldPrimary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = emeraldPrimary,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxSize()
                            )
                        }
                        Text(
                            text = "কোনো ফলাফল পাওয়া যায়নি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "অন্য কোনো রোগ, উপাদান বা আরবি নাম দিয়ে চেষ্টা করুন।",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = posts,
                        key = { it.id },
                        contentType = { "tibbe_card" }
                    ) { post ->
                        TibbeNabawiCard(
                            post = post,
                            emeraldPrimary = emeraldPrimary,
                            onClick = { onPostClick(post.id) },
                            onToggleBookmark = { viewModel.toggleBookmark(post) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TibbeNabawiCard(
    post: TibbeNabawiEntity,
    emeraldPrimary: Color,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val diseaseList = remember(post.diseases) {
        if (post.diseases.isBlank()) emptyList()
        else post.diseases.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = onePxBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Top Header Row (Category, Arabic Name, Bookmark) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Category Badge
                    Surface(
                        color = emeraldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = post.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = emeraldPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Arabic Name Badge if available
                    if (!post.arabicName.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = post.arabicName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Bookmark icon button
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "বুকমার্ক",
                        tint = if (post.isBookmarked) emeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Title ──
            Text(
                text = post.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ── Food / Herb Highlight if present ──
            if (post.foodName.isNotBlank() && post.foodName != post.title) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌿 উপাদান: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = emeraldPrimary
                    )
                    Text(
                        text = post.foodName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!post.taste.isNullOrBlank()) {
                        Text(
                            text = " • স্বাদ: ${post.taste}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Benefits Snippet ──
            val previewText = post.benefits ?: post.details
            if (previewText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Diseases Chips ──
            if (diseaseList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    diseaseList.take(3).forEach { disease ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = disease,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (diseaseList.size > 3) {
                        Text(
                            text = "+${diseaseList.size - 3} টি",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Read more indicator
                    Text(
                        text = "বিস্তারিত →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = emeraldPrimary
                    )
                }
            }
        }
    }
}
