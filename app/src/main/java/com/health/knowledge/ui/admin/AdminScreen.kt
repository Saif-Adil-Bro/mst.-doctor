package com.health.knowledge.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.home.categories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val managedArticles by viewModel.managedArticles.collectAsState()
    val manageQuery by viewModel.manageQuery.collectAsState()
    val manageFilter by viewModel.manageFilter.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingArticle by remember { mutableStateOf<Article?>(null) }
    var deletingArticle by remember { mutableStateOf<Article?>(null) }

    LaunchedEffect(uiState.isSuccess, uiState.error) {
        if (uiState.isSuccess || uiState.error != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearState()
        }
    }

    // Edit / Review Dialog
    editingArticle?.let { article ->
        EditArticleDialog(
            article = article,
            onDismiss = { editingArticle = null },
            onSave = { newTitle, newContent, newMeds, newCats ->
                viewModel.updateArticle(article, newTitle, newContent, newMeds, newCats)
                editingArticle = null
            }
        )
    }

    // Global Delete Confirmation Dialog
    deletingArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { deletingArticle = null },
            title = { Text("গ্লোবালি ডিলিট নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে \"${article.title.take(60)}...\" পোস্টটি সব ডিভাইস থেকে গ্লোবালি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArticle(article)
                        deletingArticle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingArticle = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("🛠️ অ্যাডমিন কন্ট্রোল প্যানেল", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.lockAdminMode()
                                onBack()
                            }
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "লক অ্যাডমিন")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("➕ নতুন পোস্ট") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📋 ম্যানেজ (${managedArticles.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("🔑 সেটিংস") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Status Banner
            uiState.message?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            uiState.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when (selectedTab) {
                0 -> AddPostTab(
                    isLoading = uiState.isLoading,
                    isSuccess = uiState.isSuccess,
                    onSubmit = { title, content, meds, cats, isPinned ->
                        viewModel.submitArticle(title, content, meds, cats, isPinned)
                    }
                )
                1 -> ManagePostsTab(
                    articles = managedArticles,
                    searchQuery = manageQuery,
                    selectedFilter = manageFilter,
                    onQueryChange = viewModel::onManageQueryChange,
                    onFilterChange = viewModel::onManageFilterChange,
                    onPinToggle = viewModel::togglePin,
                    onEditClick = { editingArticle = it },
                    onDeleteClick = { deletingArticle = it }
                )
                2 -> AdminSettingsTab(
                    onChangePassword = { newPass -> viewModel.changePassword(newPass) },
                    onLockAdmin = {
                        viewModel.lockAdminMode()
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddPostTab(
    isLoading: Boolean,
    isSuccess: Boolean,
    onSubmit: (String, String, String, List<String>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var medicines by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var isPinned by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            title = ""
            content = ""
            medicines = ""
            selectedCategories = emptySet()
            isPinned = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("শিরোনাম *") },
                placeholder = { Text("যেমন: চোখে ব্যথার সমাধান") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("বিস্তারিত বিষয়বস্তু *") },
                placeholder = { Text("সমস্যা ও সমাধান বিস্তারিত লিখুন...") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = medicines,
                onValueChange = { medicines = it },
                label = { Text("ভেষজ/উপাদান (কমা দিয়ে আলাদা করুন)") },
                placeholder = { Text("যেমন: হলুদ,আদা,মধু") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📌 হোমপেজে পিন করে রাখুন", fontWeight = FontWeight.SemiBold)
                        Text("পিন করলে সবার উপরে দেখাবে", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        }

        item {
            Text("বিভাগ নির্বাচন করুন:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            val chunkedCategories = categories.chunked(2)
            chunkedCategories.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (category, emoji) ->
                        FilterChip(
                            selected = selectedCategories.contains(category),
                            onClick = {
                                selectedCategories = if (selectedCategories.contains(category)) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            },
                            label = { Text("$emoji ${category.replace("_", " ")}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        item {
            Button(
                onClick = {
                    onSubmit(title, content, medicines, selectedCategories.toList(), isPinned)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("প্রকাশ করুন (গ্লোবাল সিঙ্ক)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ManagePostsTab(
    articles: List<Article>,
    searchQuery: String,
    selectedFilter: String,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onPinToggle: (Article) -> Unit,
    onEditClick: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("ম্যানেজ করার জন্য পোস্ট খুঁজুন...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { onFilterChange("all") },
                    label = { Text("সব পোস্ট") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "pinned",
                    onClick = { onFilterChange("pinned") },
                    label = { Text("📌 পিন করা") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "firebase",
                    onClick = { onFilterChange("firebase") },
                    label = { Text("☁️ নতুন যোগ করা") }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(articles, key = { it.id }) { article ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (article.isPinned) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "📌 পিন করা",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Text(
                                    "📅 ${article.date} • ❤️ ${article.reactions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = article.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(10.dp))

                        // Action Buttons: Pin, Review/Edit, Global Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPinToggle(article) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (article.isPinned) "আনপিন" else "পিন", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { onEditClick(article) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("এডিট/রিভিউ", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { onDeleteClick(article) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ডিলিট", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditArticleDialog(
    article: Article,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(article.title) }
    var content by remember { mutableStateOf(article.content) }
    var medicines by remember { mutableStateOf(article.medicines.joinToString(",")) }
    var cats by remember { mutableStateOf(article.categories.joinToString(",")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ পোস্ট রিভিউ ও এডিট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("বিস্তারিত") },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                OutlinedTextField(
                    value = medicines,
                    onValueChange = { medicines = it },
                    label = { Text("ভেষজ/উপাদান (কমা দিয়ে)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cats,
                    onValueChange = { cats = it },
                    label = { Text("বিভাগ (কমা দিয়ে)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, content, medicines, cats) }) {
                Text("সেভ ও আপডেট করুন")
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
private fun AdminSettingsTab(
    onChangePassword: (String) -> Boolean,
    onLockAdmin: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🔑 অ্যাডমিন পাসওয়ার্ড পরিবর্তন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "ডিফল্ট পাসওয়ার্ড: admin123\nহোমপেজের হেডারে ৫ বার ক্লিক করলে এই পাসওয়ার্ড দিয়ে অ্যাডমিন মোড খুলবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (onChangePassword(newPassword)) {
                            newPassword = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("পাসওয়ার্ড আপডেট করুন")
                }
            }
        }

        OutlinedButton(
            onClick = onLockAdmin,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("অ্যাডমিন মোড লক করে বের হন")
        }
    }
}
