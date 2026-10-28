package com.health.knowledge.ui.admin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.home.categories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val analytics by viewModel.analyticsState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingArticle by remember { mutableStateOf<Article?>(null) }
    var deletingArticle by remember { mutableStateOf<Article?>(null) }

    LaunchedEffect(uiState.isSuccess, uiState.error) {
        if (uiState.isSuccess || uiState.error != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearState()
        }
    }

    // Edit / Review Dialog (includes Cover Photo CDN URL & Source)
    editingArticle?.let { article ->
        EditArticleDialog(
            article = article,
            onDismiss = { editingArticle = null },
            onSave = { newTitle, newContent, newMeds, newCats, newPhotoUrl, newSource ->
                viewModel.updateArticle(article, newTitle, newContent, newMeds, newCats, newPhotoUrl, newSource)
                editingArticle = null
            }
        )
    }

    // Global Delete Confirmation Dialog
    deletingArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { deletingArticle = null },
            title = { Text("ডিলিট নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে \"${article.title.take(60)}...\" পোস্টটি ডিলিট করতে চান?") },
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

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    edgePadding = 8.dp
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
                        text = { Text("📊 অ্যানালিটিক্স") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("🔑 সেটিংস ও ব্যাকআপ") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Status Banner
            uiState.message?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    onSubmit = { title, content, meds, cats, photoUrl, isPinned, source ->
                        viewModel.submitArticle(title, content, meds, cats, photoUrl, isPinned, source)
                    },
                    onSaveDraft = { title, content, meds, cats, photoUrl, isPinned, source ->
                        viewModel.saveAsDraft(title, content, meds, cats, photoUrl, isPinned, source)
                    }
                )
                1 -> ManagePostsTab(
                    articles = managedArticles,
                    searchQuery = manageQuery,
                    selectedFilter = manageFilter,
                    draftCount = analytics.draftCount,
                    pendingCount = analytics.pendingReviewCount,
                    onQueryChange = viewModel::onManageQueryChange,
                    onFilterChange = viewModel::onManageFilterChange,
                    onPinToggle = viewModel::togglePin,
                    onPublishDraft = viewModel::publishDraft,
                    onApproveClick = viewModel::approveArticle,
                    onEditClick = { editingArticle = it },
                    onDeleteClick = { deletingArticle = it }
                )
                2 -> AdminAnalyticsTab(analytics = analytics)
                3 -> AdminSettingsTab(
                    isLoading = uiState.isLoading,
                    onChangePasswordGlobal = { newPass, newRec ->
                        viewModel.changePasswordGlobal(newPass, newRec)
                    },
                    onExportBackup = viewModel::exportFullBackup,
                    onImportBackup = viewModel::importFullBackup,
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
    onSubmit: (String, String, String, List<String>, String, Boolean, String) -> Unit,
    onSaveDraft: (String, String, String, List<String>, String, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var medicines by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var isPinned by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            title = ""
            content = ""
            medicines = ""
            source = ""
            photoUrl = ""
            selectedCategories = emptySet()
            isPinned = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("🖼️ কভার ফটো / থাম্বনেইল CDN লিংক (ঐচ্ছিক)") },
                placeholder = { Text("https://example.com/image.jpg") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Image, null) },
                trailingIcon = {
                    if (photoUrl.isNotBlank()) {
                        IconButton(onClick = { photoUrl = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl.trim())
                            .crossfade(true)
                            .build(),
                        contentDescription = "কভার প্রিভিউ",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 200.dp)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("বিস্তারিত বিষয়বস্তু *") },
                placeholder = { Text("সমস্যা ও সমাধান বিস্তারিত লিখুন...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("📚 তথ্যসূত্র / সোর্স (ঐচ্ছিক)") },
                placeholder = { Text("যেমন: আয়ুর্বেদ শাস্ত্র / ডা. পরামর্শ / রিসার্চ জার্নাল") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.MenuBook, null) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSaveDraft(title, content, medicines, selectedCategories.toList(), photoUrl, isPinned, source)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EditNote, null)
                    Spacer(Modifier.width(6.dp))
                    Text("ড্রাফট সেভ", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onSubmit(title, content, medicines, selectedCategories.toList(), photoUrl, isPinned, source)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp),
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
                        Text("প্রকাশ করুন", fontWeight = FontWeight.SemiBold)
                    }
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
    draftCount: Int,
    pendingCount: Int,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onPinToggle: (Article) -> Unit,
    onPublishDraft: (Article) -> Unit,
    onApproveClick: (Article) -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    selected = selectedFilter == "pending",
                    onClick = { onFilterChange("pending") },
                    label = { Text("⏳ পেন্ডিং রিভিউ ($pendingCount)") }
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
                    label = { Text("☁️ ক্লাউড পোস্ট") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "drafts",
                    onClick = { onFilterChange("drafts") },
                    label = { Text("📝 ড্রাফটস ($draftCount)") }
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (article.isDraft)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        else if (!article.isReviewed)
                            Color(0xFFFFF8E1)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!article.isReviewed && !article.isDraft) {
                                    Surface(
                                        color = Color(0xFFFFB300),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "⏳ পেন্ডিং রিভিউ",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (article.isDraft) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "📝 ড্রাফট",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
                                    "📅 ${article.date} • ❤️ ${article.reactions} • ✅ ${article.verifiedCount}",
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

                        if (article.source.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "📚 সূত্র: ${article.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Action Buttons: Approve / Publish Draft / Pin, Review/Edit, Global Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!article.isReviewed && !article.isDraft) {
                                Button(
                                    onClick = { onApproveClick(article) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.weight(1.2f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("অনুমোদন", style = MaterialTheme.typography.labelMedium)
                                }
                            } else if (article.isDraft) {
                                Button(
                                    onClick = { onPublishDraft(article) },
                                    modifier = Modifier.weight(1.1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("পাবলিশ", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onPinToggle(article) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (article.isPinned) "আনপিন" else "পিন", style = MaterialTheme.typography.labelMedium)
                                }
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
private fun AdminAnalyticsTab(analytics: AdminAnalyticsState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Summary Metric Grid
        item {
            Text("📊 সার্বিক পরিসংখ্যান", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "সক্রিয় পোস্ট",
                    value = "${analytics.totalActivePosts}",
                    icon = "📚",
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "মোট রিঅ্যাকশন",
                    value = "${analytics.totalReactions}",
                    icon = "❤️",
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "পরীক্ষিত ভেরিফাই",
                    value = "${analytics.totalVerifications}",
                    icon = "✅",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "পেন্ডিং রিভিউ",
                    value = "${analytics.pendingReviewCount}",
                    icon = "⏳",
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "ড্রাফট পোস্ট",
                    value = "${analytics.draftCount}",
                    icon = "📝",
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "পিন করা",
                    value = "${analytics.pinnedCount}",
                    icon = "📌",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Category Distribution Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🏷️ বিভাগ অনুযায়ী পোস্টের বণ্টন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val maxCatCount = (analytics.categoryDistribution.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    analytics.categoryDistribution.forEach { (cat, count) ->
                        val fraction = (count.toFloat() / maxCatCount.toFloat()).coerceIn(0.05f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat.replace("_", " "),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$count টি পোস্ট",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Top Reacted Posts Leaderboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🔥 সর্বোচ্চ রিঅ্যাকশন পাওয়া পোস্টসমূহ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    analytics.topReactedPosts.forEachIndexed { index, art ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${index + 1}  ${art.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "❤️ ${art.reactions} • ✅ ${art.verifiedCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditArticleDialog(
    article: Article,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(article.title) }
    var content by remember { mutableStateOf(article.content) }
    var medicines by remember { mutableStateOf(article.medicines.joinToString(",")) }
    var cats by remember { mutableStateOf(article.categories.joinToString(",")) }
    var source by remember { mutableStateOf(article.source) }
    var photoUrl by remember {
        mutableStateOf(if (article.photoPath.startsWith("http")) article.photoPath else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ পোস্ট রিভিউ ও এডিট") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("🖼️ কভার ফটো CDN লিংক (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("বিস্তারিত") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                OutlinedTextField(
                    value = medicines,
                    onValueChange = { medicines = it },
                    label = { Text("ভেষজ/উপাদান (কমা দিয়ে)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("📚 তথ্যসূত্র / সোর্স (ঐচ্ছিক)") },
                    singleLine = true,
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
            Button(onClick = { onSave(title, content, medicines, cats, photoUrl, source) }) {
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
    isLoading: Boolean,
    onChangePasswordGlobal: (String, String) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String) -> Unit,
    onLockAdmin: () -> Unit
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf("") }
    var newRecoveryCode by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                Toast.makeText(context, "✅ সম্পূর্ণ ডাটাবেস ব্যাকআপ সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ ফাইল সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                onImportBackup(json)
            } catch (e: Exception) {
                Toast.makeText(context, "❌ ফাইল পড়তে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Full Database Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "💾 সম্পূর্ণ ডাটাবেস ব্যাকআপ ও এক্সপোর্ট",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "সব পোস্ট, বুকমার্ক, নোট এবং কাস্টম ডাটা JSON ফাইল হিসেবে ফোনে ব্যাকআপ রাখুন অথবা রিস্টোর করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onExportBackup { json ->
                                pendingExportJson = json
                                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                exportLauncher.launch("health_knowledge_full_backup_$dateStr.json")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("এক্সপোর্ট (JSON)")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ইমপোর্ট (JSON)")
                    }
                }
            }
        }

        // 2. Encrypted & Firestore-Synced Admin Password & Recovery Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "🔐 এনক্রিপ্টেড পাসওয়ার্ড ও রিকভারি সিস্টেম (Firestore Sync)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "• পাসওয়ার্ড ও রিকভারি কোড SHA-256 হ্যাশ দিয়ে এনক্রিপ্ট করে Firestore (`appConfig/adminAuth`)-এ সংরক্ষিত থাকে।\n" +
                    "• ডিফল্ট পাসওয়ার্ড: admin123 | ডিফল্ট রিকভারি কোড: doctor2026\n" +
                    "• পাসওয়ার্ড ভুলে গেলে লগইন ডায়ালগ থেকে রিকভারি কোড দিয়ে সরাসরি রিসেট করা যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("নতুন অ্যাডমিন পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newRecoveryCode,
                    onValueChange = { newRecoveryCode = it },
                    label = { Text("নতুন মাস্টার রিকভারি কোড (ঐচ্ছিক, কমপক্ষে ৪ অক্ষর)") },
                    placeholder = { Text("খালি রাখলে আগের রিকভারি কোড বহাল থাকবে") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = newPassword.trim().length >= 4 && !isLoading,
                    onClick = {
                        onChangePasswordGlobal(newPassword, newRecoveryCode)
                        newPassword = ""
                        newRecoveryCode = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("এনক্রিপ্ট ও Firestore-এ আপডেট করুন")
                }
            }
        }

        OutlinedButton(
            onClick = onLockAdmin,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("অ্যাডমিন মোড লক করে বের হন")
        }
    }
}
