package com.health.knowledge.ui.home

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.health.knowledge.domain.model.Article
import com.health.knowledge.util.UserPreferencesManager

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onArticleClick: (Int) -> Unit,
    onAdminClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val displayedArticles by viewModel.displayedArticles.collectAsState()
    val hasMoreArticles by viewModel.hasMoreArticles.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()

    val bookmarked by viewModel.bookmarkedArticles.collectAsState()
    val readingHistory by viewModel.readingHistoryArticles.collectAsState()
    val notedArticles by viewModel.notedArticles.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val savedSubTab by viewModel.savedSubTab.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()

    val sortOrder by viewModel.sortOrder.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val dailyTipArticle by viewModel.dailyTipArticle.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val herbalIndex by viewModel.herbalIndex.collectAsState()
    val herbSearchQuery by viewModel.herbSearchQuery.collectAsState()
    val selectedHerb by viewModel.selectedHerb.collectAsState()
    val selectedMultiCategories by viewModel.selectedMultiCategories.collectAsState()
    val advancedFilteredArticles by viewModel.advancedFilteredArticles.collectAsState()

    val context = LocalContext.current
    var showUserPostDialog by remember { mutableStateOf(false) }

    // Hoisted LazyListStates preserved across DetailScreen / AdminScreen navigation
    val homeListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(
            firstVisibleItemIndex = viewModel.savedScrollIndex,
            firstVisibleItemScrollOffset = viewModel.savedScrollOffset
        )
    }
    val bookmarkListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(
            firstVisibleItemIndex = viewModel.savedBookmarkScrollIndex,
            firstVisibleItemScrollOffset = viewModel.savedBookmarkScrollOffset
        )
    }

    // Continuously persist scroll offset to ViewModel
    LaunchedEffect(homeListState) {
        snapshotFlow {
            homeListState.firstVisibleItemIndex to homeListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.saveHomeScrollPosition(index, offset)
        }
    }

    // Instant progressive load when nearing the end of displayed list
    LaunchedEffect(homeListState, displayedArticles.size) {
        snapshotFlow {
            val lastVisible = homeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = homeListState.layoutInfo.totalItemsCount
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 6) {
                viewModel.loadMoreArticles()
            }
        }
    }

    LaunchedEffect(bookmarkListState) {
        snapshotFlow {
            bookmarkListState.firstVisibleItemIndex to bookmarkListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.saveBookmarkScrollPosition(index, offset)
        }
    }

    // 5-click detector on Header Title for Hidden Admin Mode
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var articleToDelete by remember { mutableStateOf<Article?>(null) }

    if (showPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onUnlock = { password ->
                val ok = viewModel.unlockAdmin(password)
                if (ok) {
                    showPasswordDialog = false
                    Toast.makeText(context, "🔓 অ্যাডমিন মোড আনলক হয়েছে!", Toast.LENGTH_SHORT).show()
                }
                ok
            }
        )
    }

    if (showThemeDialog) {
        ThemeAndReminderDialog(
            currentThemeMode = themeMode,
            reminderEnabled = dailyReminderEnabled,
            onSelectTheme = { viewModel.setThemeMode(it) },
            onToggleReminder = { enabled -> viewModel.setDailyReminderEnabled(context, enabled) },
            onDismiss = { showThemeDialog = false }
        )
    }

    articleToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = { Text("পোস্ট ডিলিট নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে \"${target.title.take(50)}...\" পোস্টটি গ্লোবালি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArticle(target)
                        articleToDelete = null
                        Toast.makeText(context, "পোস্টটি গ্লোবালি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showUserPostDialog) {
        UserSubmitPostDialog(
            onDismiss = { showUserPostDialog = false },
            onSubmit = { title, content, meds, cats, photoUrl ->
                viewModel.submitUserPost(title, content, meds, cats, photoUrl) { success, msg ->
                    showUserPostDialog = false
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 12.dp)
            ) {
                // App Title + Hidden 5-Click Admin Trigger + Theme & Admin Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime > 2500L) {
                                    tapCount = 1
                                } else {
                                    tapCount++
                                }
                                lastTapTime = now
                                if (tapCount >= 5) {
                                    tapCount = 0
                                    if (!isAdminMode) {
                                        showPasswordDialog = true
                                    } else {
                                        onAdminClick()
                                    }
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌿 স্বাস্থ্য জ্ঞান",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isAdminMode) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$totalCount টি স্বাস্থ্যতথ্য • অফলাইন ও অনলাইন",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Reading Theme & Reminder Switcher Button
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                when (themeMode) {
                                    UserPreferencesManager.THEME_DARK -> Icons.Default.DarkMode
                                    UserPreferencesManager.THEME_SEPIA -> Icons.Default.MenuBook
                                    UserPreferencesManager.THEME_LIGHT -> Icons.Default.LightMode
                                    else -> Icons.Default.Palette
                                },
                                contentDescription = "থিম ও রিমাইন্ডার",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Show Admin Controls ONLY when Admin Mode is unlocked
                        if (isAdminMode) {
                            FilledTonalButton(
                                onClick = onAdminClick,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ম্যানেজ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.lockAdmin()
                                    Toast.makeText(context, "🔒 অ্যাডমিন মোড বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "লক অ্যাডমিন",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Search Bar (shown on Home tab)
                if (activeTab == "home") {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClear = viewModel::clearSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == "home") {
                ExtendedFloatingActionButton(
                    onClick = { showUserPostDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("পোস্ট করুন", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    icon = { Icon(if (activeTab == "search") Icons.Filled.Eco else Icons.Outlined.Eco, null) },
                    label = { Text("ভেষজ ও সার্চ") }
                )
                NavigationBarItem(
                    selected = activeTab == "bookmarks",
                    onClick = { viewModel.onTabSelected("bookmarks") },
                    icon = { Icon(if (activeTab == "bookmarks") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null) },
                    label = { Text("সেভ ও নোট") }
                )
            }
        }
    ) { paddingValues ->
        Crossfade(targetState = activeTab, label = "home_tab_crossfade") { tab ->
            when (tab) {
                "search" -> {
                    HerbalIndexTabContent(
                        herbalIndex = herbalIndex,
                        herbSearchQuery = herbSearchQuery,
                        selectedHerb = selectedHerb,
                        selectedMultiCategories = selectedMultiCategories,
                        filteredArticles = advancedFilteredArticles,
                        isAdminMode = isAdminMode,
                        onHerbQueryChange = viewModel::onHerbSearchQueryChange,
                        onSelectHerb = viewModel::onSelectHerb,
                        onToggleMultiCategory = viewModel::onToggleMultiCategory,
                        onClearFilters = viewModel::clearAdvancedFilters,
                        onArticleClick = onArticleClick,
                        onBookmarkToggle = { id, state -> viewModel.toggleBookmark(id, state) },
                        onReactionClick = { viewModel.toggleReaction(it) },
                        onPinToggle = { viewModel.togglePin(it) },
                        onDeleteClick = { articleToDelete = it },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                "bookmarks" -> {
                    SavedAndHistoryTabContent(
                        savedSubTab = savedSubTab,
                        bookmarkedArticles = bookmarked,
                        historyArticles = readingHistory,
                        notedArticles = notedArticles,
                        listState = bookmarkListState,
                        isAdminMode = isAdminMode,
                        onSubTabChange = viewModel::onSavedSubTabChange,
                        onArticleClick = onArticleClick,
                        onBookmarkToggle = { id, state -> viewModel.toggleBookmark(id, state) },
                        onReactionClick = { viewModel.toggleReaction(it) },
                        onPinToggle = { viewModel.togglePin(it) },
                        onDeleteClick = { articleToDelete = it },
                        onClearHistory = {
                            viewModel.clearReadingHistory()
                            Toast.makeText(context, "🕒 পঠিত হিস্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        onExportBackup = viewModel::exportUserBackup,
                        onImportBackup = viewModel::importUserBackup,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                else -> {
                    if (isInitialLoading && searchQuery.isBlank() && selectedCategory == null) {
                        InitialDatabaseLoadingView(modifier = Modifier.padding(paddingValues))
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                viewModel.refreshFromCloud { newAdded ->
                                    val msg = if (newAdded > 0) {
                                        "☁️ $newAdded টি নতুন পোস্ট ক্লাউড থেকে যুক্ত হয়েছে!"
                                    } else {
                                        "✅ সব তথ্য ও রিঅ্যাকশন আপ-টু-ডেট আছে"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            LazyColumn(
                                state = homeListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                // Category chips + Daily Tip Card (when not searching)
                                if (searchQuery.isBlank()) {
                                    item(key = "category_chips", contentType = "header_chips") {
                                        CategoryChips(
                                            categories = categories,
                                            selectedCategory = selectedCategory,
                                            onCategorySelected = viewModel::onCategorySelected
                                        )
                                    }

                                    if (selectedCategory == null) {
                                        dailyTipArticle?.let { tip ->
                                            item(key = "daily_tip_card", contentType = "daily_tip") {
                                                DailyHealthTipCard(
                                                    article = tip,
                                                    reminderEnabled = dailyReminderEnabled,
                                                    onReadClick = { onArticleClick(tip.id) },
                                                    onNextTipClick = viewModel::nextDailyTip,
                                                    onToggleReminder = { showThemeDialog = true }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Results header + Sort chips (জনপ্রিয় / নতুন / পুরাতন)
                                item(key = "results_header", contentType = "header_text") {
                                    val headerText = when {
                                        selectedCategory != null -> "\"${selectedCategory!!.replace("_", " ")}\": ${articles.size}টি"
                                        searchQuery.isNotBlank() -> "\"$searchQuery\": ${articles.size}টি"
                                        else -> "সকল স্বাস্থ্যতথ্য (${articles.size}টি)"
                                    }
                                    SortAndResultsHeader(
                                        headerText = headerText,
                                        sortOrder = sortOrder,
                                        onSortChange = viewModel::onSortOrderChange
                                    )
                                }

                                if (articles.isEmpty() && searchQuery.isNotBlank()) {
                                    item(key = "empty_state") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(48.dp),
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
                                    items(
                                        items = displayedArticles,
                                        key = { it.id },
                                        contentType = { "article_card" }
                                    ) { article ->
                                        ArticleCard(
                                            article = article,
                                            isAdminMode = isAdminMode,
                                            onClick = { onArticleClick(article.id) },
                                            onBookmarkToggle = { viewModel.toggleBookmark(article.id, article.isBookmarked) },
                                            onReactionClick = { viewModel.toggleReaction(article) },
                                            onPinToggle = { viewModel.togglePin(article) },
                                            onDeleteClick = { articleToDelete = article }
                                        )
                                    }

                                    if (hasMoreArticles) {
                                        item(key = "loading_more_posts") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "পরবর্তী পোস্টগুলো লোড হচ্ছে...",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onUnlock: (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("🔐 অ্যাডমিন মোড আনলক", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("অ্যাডমিন প্যানেল চালু করতে পাসওয়ার্ড দিন:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text("পাসওয়ার্ড") },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        "❌ ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onUnlock(password)) {
                        isError = true
                    }
                }
            ) {
                Text("আনলক করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
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
            focusedTextColor = Color(0xFF1A1A1A),
            unfocusedTextColor = Color(0xFF1A1A1A),
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
    isAdminMode: Boolean,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onReactionClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val hasCdnImage = remember(article.photoPath) {
        article.photoPath.startsWith("http://") || article.photoPath.startsWith("https://")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (article.isPinned) 4.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                article.isPinned -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                article.syncedFromFirebase -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cover Photo / Thumbnail from CDN URL (auto-fit screen width)
            if (hasCdnImage) {
                AsyncImage(
                    model = remember(article.photoPath) {
                        ImageRequest.Builder(context)
                            .data(article.photoPath)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = article.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(10.dp))
            }

            // Header row: Pinned / New / Note badges + Bookmark / Admin actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (article.isPinned) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "📌 পিন করা",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (article.syncedFromFirebase) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "নতুন",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    if (article.userNote.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "📓 নোট",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdminMode) {
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "পিন করুন",
                                tint = if (article.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "ডিলিট করুন",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

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

            // Lightweight Row instead of nested LazyRow for 60/120fps smooth scrolling
            if (article.categories.isNotEmpty() || article.medicines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    article.categories.take(3).forEach { cat ->
                        TagChip(text = cat.replace("_", " "), color = MaterialTheme.colorScheme.primaryContainer)
                    }
                    article.medicines.take(2).forEach { med ->
                        TagChip(text = "💊 $med", color = MaterialTheme.colorScheme.secondaryContainer)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Footer: Date + Interactive Per-IP Reaction Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${article.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (article.isReacted)
                        Color(0xFFFFEBEE)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onReactionClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (article.isReacted) "❤️" else "🤍",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = article.reactions.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (article.isReacted) FontWeight.Bold else FontWeight.Normal,
                            color = if (article.isReacted) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
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
    listState: LazyListState,
    isAdminMode: Boolean,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onReactionClick: (Article) -> Unit,
    onPinToggle: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (articles.isEmpty()) {
            item(key = "empty_bookmarks") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(
                items = articles,
                key = { it.id },
                contentType = { "article_card" }
            ) { article ->
                ArticleCard(
                    article = article,
                    isAdminMode = isAdminMode,
                    onClick = { onArticleClick(article.id) },
                    onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) },
                    onReactionClick = { onReactionClick(article) },
                    onPinToggle = { onPinToggle(article) },
                    onDeleteClick = { onDeleteClick(article) }
                )
            }
        }
    }
}

@Composable
fun InitialDatabaseLoadingView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🌿", fontSize = 44.sp)
                }
            }

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(38.dp)
            )

            Text(
                text = "স্বাস্থ্য জ্ঞান প্রস্তুত হচ্ছে...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "প্রথমবার অ্যাপ চালু করায় তথ্যগুলো অফলাইনে সাজানো হচ্ছে। অনুগ্রহ করে কয়েক সেকেন্ড অপেক্ষা করুন...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserSubmitPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, medicines: String, categories: List<String>, photoUrl: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var medicines by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var selectedCats by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✍️", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "নতুন স্বাস্থ্যতথ্য পোস্ট করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "ℹ️ আপনার পোস্টটি পর্যালোচনার জন্য জমা হবে। অ্যাডমিন অনুমোদন (Approve) করার পর এটি সবার কাছে প্রকাশিত হবে।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("শিরোনাম *") },
                            placeholder = { Text("যেমন: গ্যাস্ট্রিক দূর করার সহজ ঘরোয়া উপায়") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("বিস্তারিত বিবরণ ও সমাধান *") },
                            placeholder = { Text("কীভাবে ব্যবহার করবেন, নিয়মাবলি ইত্যাদি বিস্তারিত লিখুন...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 4,
                            maxLines = 8
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = medicines,
                            onValueChange = { medicines = it },
                            label = { Text("ভেষজ বা উপাদান (ঐচ্ছিক)") },
                            placeholder = { Text("কমা দিয়ে লিখুন, যেমন: মধু, আদা, লেবু") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = photoUrl,
                            onValueChange = { photoUrl = it },
                            label = { Text("কভার ফটো CDN লিংক (ঐচ্ছিক)") },
                            placeholder = { Text("https://example.com/image.jpg") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        Text(
                            "ক্যাটাগরি নির্বাচন করুন:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { (cat, emoji) ->
                                val selected = selectedCats.contains(cat)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedCats = if (selected) selectedCats - cat else selectedCats + cat
                                    },
                                    label = { Text("$emoji ${cat.replace("_", " ")}") }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        isSubmitting = true
                        onSubmit(title, content, medicines, selectedCats.toList(), photoUrl)
                    },
                    enabled = title.isNotBlank() && content.isNotBlank() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("জমা হচ্ছে...")
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("পোস্ট জমা দিন (Submit)")
                    }
                }
            }
        }
    }
}
