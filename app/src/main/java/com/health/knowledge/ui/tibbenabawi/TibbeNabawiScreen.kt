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
import com.health.knowledge.ui.theme.SolaimanLipi
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
    val indexMode by viewModel.indexMode.collectAsState()
    val selectedHerb by viewModel.selectedHerb.collectAsState()
    val selectedDisease by viewModel.selectedDisease.collectAsState()

    val categories by viewModel.categories.collectAsState()
    val herbIndexList by viewModel.herbIndexList.collectAsState()
    val diseaseIndexList by viewModel.diseaseIndexList.collectAsState()
    val posts by viewModel.filteredPosts.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val bookmarkedCount by viewModel.bookmarkedCount.collectAsState()

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val emeraldPrimary = Color(0xFF1B6B46)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "তিব্বে নববী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
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
                            text = "ইবনুল কাইয়্যিম (রহ:) • মোট $totalCount টি পথ্য ও নির্দেশনা",
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
                    FilledTonalIconToggleButton(
                        checked = onlyBookmarked,
                        onCheckedChange = { viewModel.toggleOnlyBookmarked() },
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (onlyBookmarked) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "বুকমার্ক সক্রিয়")
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
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(fontFamily = SolaimanLipi)
        ) {
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
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        "রোগ, ভেষজ বা আরবি নাম দিয়ে খুঁজুন...",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "খুঁজুন",
                        tint = emeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = emeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // ── Index Mode Selector (সকল / ভেষজ সূচিপত্র / রোগের সূচিপত্র) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 1: All Posts
                FilterChip(
                    selected = indexMode == TibbeIndexMode.ALL,
                    onClick = { viewModel.setIndexMode(TibbeIndexMode.ALL) },
                    label = { Text("📋 সকল পোস্ট", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = emeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Herb Index
                FilterChip(
                    selected = indexMode == TibbeIndexMode.HERBS,
                    onClick = { viewModel.setIndexMode(TibbeIndexMode.HERBS) },
                    label = { Text("🌿 ভেষজ", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = emeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(0.9f)
                )

                // Tab 3: Disease Index
                FilterChip(
                    selected = indexMode == TibbeIndexMode.DISEASES,
                    onClick = { viewModel.setIndexMode(TibbeIndexMode.DISEASES) },
                    label = { Text("🩺 রোগ সূচি", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = emeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Secondary Horizontal Chips based on Selected Mode ────
            when (indexMode) {
                TibbeIndexMode.ALL -> {
                    // Category Filter Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item(key = "cat_all") {
                            val isSelected = selectedCategory == "সকল"
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelect("সকল") },
                                label = { Text("সকল (${totalCount})", fontSize = 12.sp) },
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
                                        "${getCategoryIcon(category)} $category",
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = emeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                TibbeIndexMode.HERBS -> {
                    // Herb Index Filter Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(herbIndexList, key = { "herb_${it.first}" }) { (herb, count) ->
                            val isSelected = selectedHerb == herb
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectHerb(herb) },
                                label = {
                                    Text(
                                        text = "🌿 $herb ($count)",
                                        fontSize = 12.sp,
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
                }

                TibbeIndexMode.DISEASES -> {
                    // Disease Index Filter Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(diseaseIndexList, key = { "disease_${it.first}" }) { (disease, count) ->
                            val isSelected = selectedDisease == disease
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectDisease(disease) },
                                label = {
                                    Text(
                                        text = "🩺 $disease ($count)",
                                        fontSize = 12.sp,
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
                }
            }

            // ── Active Filter Bar / Result Counter ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${posts.size}টি ফলাফল",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedHerb != null) {
                        Surface(
                            color = emeraldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ভেষজ: $selectedHerb ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldPrimary,
                                modifier = Modifier
                                    .clickable { viewModel.selectHerb(null) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (selectedDisease != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "রোগ: $selectedDisease ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .clickable { viewModel.selectDisease(null) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (searchQuery.isNotBlank() || selectedCategory != "সকল" || selectedHerb != null || selectedDisease != null || onlyBookmarked) {
                    Text(
                        text = "ফিল্টার রিসেট",
                        fontSize = 11.5.sp,
                        color = emeraldPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.clearAllFilters() }
                    )
                }
            }

            // ── List of Posts (Compact Height, Sleek Cards with Icons) ──
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
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = emeraldPrimary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌿", fontSize = 26.sp)
                            }
                        }
                        Text(
                            text = "কোনো ফলাফল পাওয়া যায়নি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "অন্য কোনো রোগ, উপাদান বা আরবি নাম দিয়ে চেষ্টা করুন।",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val isHerbMode = indexMode == TibbeIndexMode.HERBS || selectedHerb != null
                val groupedPosts = remember(posts, isHerbMode, selectedHerb) {
                    if (isHerbMode && selectedHerb == null) {
                        posts.groupBy { extractCanonicalHerb(it) }
                    } else {
                        emptyMap()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isHerbMode && selectedHerb == null) {
                        // Group by canonical herb (e.g. মধু, দুধ, মাংস, খেজুর, etc.)
                        groupedPosts.forEach { (herbGroup, groupPosts) ->
                            item(key = "group_header_$herbGroup") {
                                Surface(
                                    color = emeraldPrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "🌿 $herbGroup",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = emeraldPrimary
                                        )
                                        Text(
                                            text = "${groupPosts.size}টি নির্দেশনা",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = emeraldPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                            items(
                                items = groupPosts,
                                key = { it.id },
                                contentType = { "tibbe_compact_card" }
                            ) { post ->
                                TibbeNabawiCompactCard(
                                    post = post,
                                    emeraldPrimary = emeraldPrimary,
                                    displayHerbAsHeading = true,
                                    onClick = { onPostClick(post.id) },
                                    onToggleBookmark = { viewModel.toggleBookmark(post) }
                                )
                            }
                        }
                    } else {
                        if (selectedHerb != null) {
                            item(key = "selected_herb_banner") {
                                Surface(
                                    color = emeraldPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🌿", fontSize = 16.sp)
                                            Text(
                                                text = selectedHerb ?: "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = emeraldPrimary
                                            )
                                        }
                                        Text(
                                            text = "${posts.size}টি হাদিস ও সুন্নাহ নির্দেশনা",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = emeraldPrimary
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = posts,
                            key = { it.id },
                            contentType = { "tibbe_compact_card" }
                        ) { post ->
                            TibbeNabawiCompactCard(
                                post = post,
                                emeraldPrimary = emeraldPrimary,
                                displayHerbAsHeading = isHerbMode,
                                onClick = { onPostClick(post.id) },
                                onToggleBookmark = { viewModel.toggleBookmark(post) }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * Compact, sleek card design (height ~75dp) with leading icon,
 * category/Arabic pill, single-line title, and subtitle metadata.
 */
@Composable
private fun TibbeNabawiCompactCard(
    post: TibbeNabawiEntity,
    emeraldPrimary: Color,
    displayHerbAsHeading: Boolean = false,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val canonicalHerb = remember(post) { extractCanonicalHerb(post) }
    val cardIcon = remember(post.category, canonicalHerb, displayHerbAsHeading) {
        if (displayHerbAsHeading) {
            when (canonicalHerb) {
                "মধু" -> "🍯"
                "দুধ" -> "🥛"
                "মাংস" -> "🥩"
                "খেজুর" -> "🌴"
                "কালোজিরা" -> "🌿"
                "সুরমা (কোহল)" -> "👁️"
                "জলপাই ও তেল" -> "🫒"
                "যব ও বার্লি" -> "🌾"
                "লাউ" -> "🥬"
                "আঙ্গুর ও কিসমিস" -> "🍇"
                "ডালিম" -> "🍎"
                "ডুমুর" -> "🫐"
                "আদা" -> "🫚"
                "মেহেদি" -> "🌿"
                "সোনামুখী" -> "🌱"
                "সিরকা (ভিনেগার)" -> "🍶"
                "মাছ" -> "🐟"
                "ডিম" -> "🥚"
                "লেবু" -> "🍋"
                "শসা" -> "🥒"
                "রসুন" -> "🧄"
                "পেঁয়াজ" -> "🧅"
                "মেথি" -> "🌱"
                "সুগন্ধি ও মিস্ক" -> "🧴"
                else -> getCategoryIcon(post.category)
            }
        } else {
            getCategoryIcon(post.category)
        }
    }
    val diseasesText = remember(post.diseases) {
        if (post.diseases.isBlank()) ""
        else post.diseases.split(",").take(2).joinToString(", ")
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 1. Leading Category/Herb Icon Avatar ──────────────
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = emeraldPrimary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = cardIcon,
                        fontSize = 20.sp
                    )
                }
            }

            // ── 2. Middle Content (Pills, Title, Metadata) ────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top Tag Line (Herb name or Category + Arabic Name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val headingText = if (displayHerbAsHeading) "🌿 $canonicalHerb" else post.category
                    Text(
                        text = headingText,
                        fontFamily = SolaimanLipi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = emeraldPrimary
                    )

                    if (!post.arabicName.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = post.arabicName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Title (Single line, compact)
                Text(
                    text = post.title,
                    fontFamily = SolaimanLipi,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle Line: Food / Category & Disease
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val subLabel = if (displayHerbAsHeading) post.category else post.foodName.ifBlank { "পথ্য" }
                    val subPrefix = if (displayHerbAsHeading) "📁" else "🌱"
                    Text(
                        text = "$subPrefix $subLabel",
                        fontFamily = SolaimanLipi,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (diseasesText.isNotBlank()) {
                        Text(
                            text = " • 🩺 $diseasesText",
                            fontFamily = SolaimanLipi,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // ── 3. Trailing Bookmark & Chevron ────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "বুকমার্ক",
                        tint = if (post.isBookmarked) emeraldPrimary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getCategoryIcon(category: String): String {
    return when {
        category.contains("শাকসবজি") || category.contains("ভেষজ") -> "🌿"
        category.contains("ফলমূল") -> "🍎"
        category.contains("খাদ্য") || category.contains("পানীয়") -> "🍯"
        category.contains("তরল") || category.contains("পথ্য") -> "🥣"
        category.contains("মাংস") || category.contains("প্রাণিজ") -> "🥩"
        category.contains("তেল") || category.contains("সুগন্ধি") -> "🧴"
        category.contains("রোগভিত্তিক") -> "🩺"
        category.contains("চিকিৎসা") -> "🩹"
        category.contains("স্বাস্থ্যবিধি") -> "🏃"
        category.contains("আধ্যাত্মিক") || category.contains("দোয়া") || category.contains("রুকিয়াহ") -> "🤲"
        else -> "📖"
    }
}
