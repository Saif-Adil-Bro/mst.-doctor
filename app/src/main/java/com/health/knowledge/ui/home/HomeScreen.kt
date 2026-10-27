package com.health.knowledge.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.domain.model.Article

val categories = listOf(
    "চোখ" to "👁️",
    "পেট_হজম" to "🫃",
    "ঘুম" to "😴",
    "হরমোন" to "💊",
    "কিডনি" to "🫘",
    "রক্ত_হৃদয়" to "❤️",
    "মাথা_ব্রেন" to "🧠",
    "ত্বক_চর্ম" to "✨",
    "পানি" to "💧",
    "শ্বাস_ফুসফুস" to "🫁",
    "নারী_স্বাস্থ্য" to "🌸",
    "আয়ুর্বেদ" to "🌿"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (Int) -> Unit,
    onAdminClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val bookmarked by viewModel.bookmarkedArticles.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(bottom = 8.dp)
            ) {
                // App Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌿 স্বাস্থ্য জ্ঞান",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCount টি স্বাস্থ্যতথ্য",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "নতুন যোগ করুন",
                            tint = Color.White,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .padding(4.dp)
                        )
                    }
                }

                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == "home",
                    onClick = { viewModel.onTabSelected("home") },
                    icon = { Icon(if (activeTab == "home") Icons.Filled.Home else Icons.Outlined.Home, null) },
                    label = { Text("হোম") }
                )
                NavigationBarItem(
                    selected = activeTab == "search",
                    onClick = { viewModel.onTabSelected("search") },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("সার্চ") }
                )
                NavigationBarItem(
                    selected = activeTab == "bookmarks",
                    onClick = { viewModel.onTabSelected("bookmarks") },
                    icon = { Icon(if (activeTab == "bookmarks") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null) },
                    label = { Text("সেভ করা") }
                )
            }
        }
    ) { paddingValues ->
        when (activeTab) {
            "bookmarks" -> {
                ArticleList(
                    articles = bookmarked,
                    onArticleClick = onArticleClick,
                    onBookmarkToggle = { id, state -> viewModel.toggleBookmark(id, state) },
                    emptyMessage = "কোনো সেভ করা আর্টিকেল নেই",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Category chips
                    if (searchQuery.isBlank()) {
                        item {
                            CategoryChips(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = viewModel::onCategorySelected
                            )
                        }
                    }

                    // Results header
                    item {
                        val headerText = when {
                            selectedCategory != null -> "\"$selectedCategory\" বিষয়ক: ${articles.size}টি"
                            searchQuery.isNotBlank() -> "\"$searchQuery\" এর ফলাফল: ${articles.size}টি"
                            else -> "সকল স্বাস্থ্যতথ্য"
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (articles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "\"$searchQuery\" এর জন্য কোনো ফলাফল পাওয়া যায়নি",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(articles, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                searchQuery = searchQuery,
                                onClick = { onArticleClick(article.id) },
                                onBookmarkToggle = { viewModel.toggleBookmark(article.id, article.isBookmarked) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("রোগ, উপাদান বা বিষয় লিখুন...") },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, null)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun CategoryChips(
    categories: List<Pair<String, String>>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("সব") },
                leadingIcon = { Text("🌿") }
            )
        }
        items(categories) { (category, emoji) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(if (selectedCategory == category) null else category) },
                label = { Text(category.replace("_", " ")) },
                leadingIcon = { Text(emoji) }
            )
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    searchQuery: String,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (article.syncedFromFirebase)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // New badge for Firebase synced
                if (article.syncedFromFirebase) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "নতুন",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Title
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Content preview
            Text(
                text = article.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Tags row
            if (article.categories.isNotEmpty() || article.medicines.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(article.categories.take(3)) { cat ->
                        TagChip(text = cat.replace("_", " "), color = MaterialTheme.colorScheme.primaryContainer)
                    }
                    items(article.medicines.take(2)) { med ->
                        TagChip(text = "💊 $med", color = MaterialTheme.colorScheme.tertiaryContainer ?: MaterialTheme.colorScheme.secondaryContainer)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (article.reactions > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("❤️", fontSize = 12.sp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = article.reactions.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun ArticleList(
    articles: List<Article>,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 16.dp)) {
        if (articles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(articles, key = { it.id }) { article ->
                ArticleCard(
                    article = article,
                    searchQuery = "",
                    onClick = { onArticleClick(article.id) },
                    onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) }
                )
            }
        }
    }
}
