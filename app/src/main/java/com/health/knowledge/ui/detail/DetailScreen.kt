package com.health.knowledge.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.home.TagChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    articleId: Int,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("স্বাস্থ্যতথ্য", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    article?.let { art ->
                        // Bookmark
                        IconButton(onClick = { viewModel.toggleBookmark(art.id, art.isBookmarked) }) {
                            Icon(
                                if (art.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "বুকমার্ক"
                            )
                        }
                        // Share
                        IconButton(onClick = {
                            val shareText = "${art.title}\n\n${art.content}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
                        }) {
                            Icon(Icons.Default.Share, "শেয়ার")
                        }
                        // Copy
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString("${art.title}\n\n${art.content}"))
                            showCopied = true
                        }) {
                            Icon(Icons.Default.ContentCopy, "কপি")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = {
            if (showCopied) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopied = false
                }
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text("টেক্সট কপি হয়েছে!")
                }
            }
        }
    ) { paddingValues ->
        article?.let { art ->
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                item {
                    Text(
                        text = art.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Meta info
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 ${art.date}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (art.reactions > 0) {
                            Text("❤️ ${art.reactions}", style = MaterialTheme.typography.labelMedium)
                        }
                        if (art.syncedFromFirebase) {
                            Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp)) {
                                Text("নতুন", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                    }
                }

                // Categories section
                if (art.categories.isNotEmpty()) {
                    item {
                        SectionCard(title = "🏷️ বিষয়বস্তু") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(art.categories) { cat ->
                                    TagChip(text = cat.replace("_", " "),
                                        color = MaterialTheme.colorScheme.primaryContainer)
                                }
                            }
                        }
                    }
                }

                // Medicines / ingredients section
                if (art.medicines.isNotEmpty()) {
                    item {
                        SectionCard(title = "💊 উপাদান / ভেষজ") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                art.medicines.forEach { med ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌿 ", fontSize = 16.sp)
                                        Text(med, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Main content
                item {
                    SectionCard(title = "📝 বিস্তারিত") {
                        Text(
                            text = art.content,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp
                        )
                    }
                }

                // Share reminder
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📌 উপকারী মনে হলে পরিচিতজনের সাথে শেয়ার করুন",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
