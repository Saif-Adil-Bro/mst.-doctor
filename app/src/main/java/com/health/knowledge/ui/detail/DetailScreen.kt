package com.health.knowledge.ui.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.home.TagChip
import com.health.knowledge.util.UserPreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    articleId: Int,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && article != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("পোস্ট ডিলিট নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে এই পোস্টটি গ্লোবালি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteArticle(article!!) {
                            Toast.makeText(context, "পোস্টটি গ্লোবালি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

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
                        if (isAdminMode) {
                            IconButton(onClick = { viewModel.togglePin(art) }) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "পিন",
                                    tint = if (art.isPinned) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "ডিলিট")
                            }
                        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Font Size Control Bar (A- / A+) ──────────────────────────
                item {
                    FontSizeControlBar(
                        fontSizeSp = fontSizeSp,
                        onDecrease = viewModel::decreaseFontSize,
                        onIncrease = viewModel::increaseFontSize,
                        onReset = viewModel::resetFontSize
                    )
                }

                // ── Title ────────────────────────────────────────────────────
                item {
                    Text(
                        text = art.title,
                        fontSize = (fontSizeSp + 4f).sp,
                        lineHeight = ((fontSizeSp + 4f) * 1.35f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ── Meta info + Interactive Per-IP Reaction Button ───────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📅 ${art.date}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (art.isPinned) {
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
                            if (art.syncedFromFirebase) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "নতুন",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }

                        // Reaction Button (1 per IP)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (art.isReacted) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.toggleReaction(art) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (art.isReacted) "❤️" else "🤍", fontSize = 15.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (art.isReacted) "পছন্দ করেছেন (${art.reactions})" else "পছন্দ করুন (${art.reactions})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (art.isReacted) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Categories section ───────────────────────────────────────
                if (art.categories.isNotEmpty()) {
                    item {
                        SectionCard(title = "🏷️ বিষয়বস্তু") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(art.categories) { cat ->
                                    TagChip(
                                        text = cat.replace("_", " "),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Medicines / ingredients section ──────────────────────────
                if (art.medicines.isNotEmpty()) {
                    item {
                        SectionCard(title = "💊 উপাদান / ভেষজ") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                art.medicines.forEach { med ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌿 ", fontSize = fontSizeSp.sp)
                                        Text(
                                            med,
                                            fontSize = fontSizeSp.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Main content (scales with fontSizeSp) ────────────────────
                item {
                    SectionCard(title = "📝 বিস্তারিত সমাধান") {
                        Text(
                            text = art.content,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.6f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Bottom Reaction & Share Card ─────────────────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📌 লেখাটি উপকারী মনে হয়েছে?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = { viewModel.toggleReaction(art) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (art.isReacted) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (art.isReacted) "❤️ ${art.reactions}" else "🤍 রিঅ্যাক্ট দিন (${art.reactions})"
                                )
                            }
                        }
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
fun FontSizeControlBar(
    fontSizeSp: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit
) {
    val percent = ((fontSizeSp / UserPreferencesManager.DEFAULT_FONT_SIZE) * 100).toInt()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "লেখার আকার ($percent%)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onDecrease,
                    enabled = fontSizeSp > UserPreferencesManager.MIN_FONT_SIZE,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (fontSizeSp != UserPreferencesManager.DEFAULT_FONT_SIZE) {
                    TextButton(
                        onClick = onReset,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("রিসেট", fontSize = 12.sp)
                    }
                }

                FilledTonalIconButton(
                    onClick = onIncrease,
                    enabled = fontSizeSp < UserPreferencesManager.MAX_FONT_SIZE,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
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
