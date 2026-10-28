package com.health.knowledge.ui.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.home.TagChip
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.ShareCardGenerator
import com.health.knowledge.util.UserPreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    articleId: Int,
    onBack: () -> Unit,
    onRelatedArticleClick: (Int) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val relatedArticles by viewModel.relatedArticles.collectAsState()
    val verifierEntries by viewModel.verifierEntries.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showCopied by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullscreenPhoto by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showVerifyPopup by remember { mutableStateOf(false) }
    var isSharingImage by remember { mutableStateOf(false) }

    if (showVerifyPopup && article != null) {
        val art = article!!
        VerificationConsentDialog(
            article = art,
            onDismiss = { showVerifyPopup = false },
            onSubmit = { name, contact, comment ->
                viewModel.submitVerification(art, name, contact, comment) {
                    showVerifyPopup = false
                    Toast.makeText(
                        context,
                        "✅ আপনার সত্যায়ন ও ভেরিফিকেশন সফলভাবে প্রকাশিত হয়েছে!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRemoveVerification = if (art.isVerified) {
                {
                    viewModel.removeVerification(art) {
                        showVerifyPopup = false
                        Toast.makeText(context, "ভেরিফিকেশন তুলে নেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
            } else null
        )
    }

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

    if (showShareDialog && article != null) {
        val art = article!!
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("📤 পোস্ট শেয়ার করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("আপনি কীভাবে এই স্বাস্থ্যতথ্যটি শেয়ার করতে চান?", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = {
                            showShareDialog = false
                            isSharingImage = true
                            coroutineScope.launch {
                                ShareCardGenerator.shareArticleAsImageCard(context, art)
                                isSharingImage = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("🖼️ সুন্দর ইমেজ কার্ড হিসেবে শেয়ার")
                    }

                    OutlinedButton(
                        onClick = {
                            showShareDialog = false
                            val sourceFooter = if (art.source.isNotBlank()) "\n\n📚 সূত্র: ${art.source}" else ""
                            val shareText = "${art.title}\n\n${art.content}$sourceFooter"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder()
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("📝 সম্পূর্ণ টেক্সট হিসেবে শেয়ার")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showFullscreenPhoto && article != null) {
        FullscreenPhotoViewerDialog(
            photoUrl = article!!.photoPath,
            title = article!!.title,
            onDismiss = { showFullscreenPhoto = false }
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
                        IconButton(onClick = { showShareDialog = true }) {
                            if (isSharingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Share, "শেয়ার")
                            }
                        }
                        // Copy
                        IconButton(onClick = {
                            val sourceFooter = if (art.source.isNotBlank()) "\n\n📚 সূত্র: ${art.source}" else ""
                            clipboardManager.setText(AnnotatedString("${art.title}\n\n${art.content}$sourceFooter"))
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
            val hasCdnImage = remember(art.photoPath) {
                art.photoPath.startsWith("http://") || art.photoPath.startsWith("https://")
            }

            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Font Size + Reading Theme Control Bar (A- / A+ / Light / Sepia / Dark)
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FontSizeControlBar(
                            fontSizeSp = fontSizeSp,
                            themeMode = themeMode,
                            onDecrease = viewModel::decreaseFontSize,
                            onIncrease = viewModel::increaseFontSize,
                            onReset = viewModel::resetFontSize,
                            onSelectTheme = viewModel::setThemeMode
                        )
                    }
                }

                // ── Cover Photo from CDN URL (Tap for Fullscreen Pinch-to-Zoom)
                if (hasCdnImage) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = onePxBorder(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullscreenPhoto = true }
                            ) {
                                Box {
                                    AsyncImage(
                                        model = remember(art.photoPath) {
                                            ImageRequest.Builder(context)
                                                .data(art.photoPath)
                                                .crossfade(true)
                                                .build()
                                        },
                                        contentDescription = art.title,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.ZoomIn,
                                                contentDescription = "জুম করুন",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("জুম করতে ট্যাপ করুন", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Selectable Title ─────────────────────────────────────────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SelectionContainer {
                            Text(
                                text = art.title,
                                fontSize = (fontSizeSp + 4f).sp,
                                lineHeight = ((fontSizeSp + 4f) * 1.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // ── Meta info + Interactive Global Reaction & Verification ───
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Surface(
                                        color = if (art.isVerified) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        border = onePxBorder(
                                            if (art.isVerified) Color(0xFF2E7D32).copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { showVerifyPopup = true }
                                    ) {
                                        Text(
                                            "✅ পরীক্ষিত (${art.verifiedCount})",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (art.isVerified) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Reaction Button (1 per IP, synced globally via Firestore)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (art.isReacted) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = onePxBorder(
                                        if (art.isReacted) Color(0xFFD32F2F).copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
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
                                            text = if (art.isReacted) "পছন্দ (${art.reactions})" else "পছন্দ করুন (${art.reactions})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (art.isReacted) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Source / Reference Badge (if provided)
                            if (art.source.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = onePxBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = "তথ্যসূত্র",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        SelectionContainer {
                                            Text(
                                                text = "📚 তথ্যসূত্র: ${art.source}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Categories section ───────────────────────────────────────
                if (art.categories.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                }

                // ── Selectable Medicines / ingredients section (ইনলাইন ভেষজ উপাদান) ───────────────
                if (art.medicines.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionCard(title = "💊 উপাদান / ভেষজ") {
                                SelectionContainer {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        art.medicines.forEach { med ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                                border = onePxBorder()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("🌿", fontSize = (fontSizeSp - 1f).coerceAtLeast(12f).sp)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = med,
                                                        fontSize = fontSizeSp.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
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

                // ── Selectable Main Content - 100% Full Width Card ─────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        border = onePxBorder(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📝 বিস্তারিত সমাধান",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "টেক্সট সিলেক্ট করা যাবে",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            HorizontalDivider(
                                thickness = com.health.knowledge.ui.theme.onePxDp(),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            SelectionContainer {
                                Text(
                                    text = art.content,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 1.65f).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── Global User Verification Card ("পরীক্ষিত / কাজ হয়েছে") ────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (art.isVerified) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
                            ),
                            border = onePxBorder(
                                if (art.isVerified) Color(0xFF2E7D32).copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "পরীক্ষিত ও ফলাফল যাচাই",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (art.isVerified) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "${art.verifiedCount} জন ভেরিফাই করেছেন",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }

                                Text(
                                    text = "এই পরামর্শ বা ভেষজ সমাধানটি আপনার পরীক্ষিত বা এর দ্বারা উপকার হয়ে থাকলে নিচের বাটনে ক্লিক করে সত্যায়ন ও মতামত প্রকাশ করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (art.isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { showVerifyPopup = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (art.isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        if (art.isVerified) Icons.Default.CheckCircle else Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (art.isVerified) {
                                            "✅ আপনার পরীক্ষিত হিসেবে ভেরিফাইড (${art.verifiedCount})"
                                        } else {
                                            "☑️ পরীক্ষিত ভেরিফাই করুন (${art.verifiedCount})"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Display published user verification comments/names if any exist
                                val withCommentsOrNames = remember(verifierEntries) {
                                    verifierEntries.filter { it.name.isNotBlank() || it.comment.isNotBlank() }.take(5)
                                }
                                if (withCommentsOrNames.isNotEmpty()) {
                                    HorizontalDivider(
                                        thickness = com.health.knowledge.ui.theme.onePxDp(),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Text(
                                        text = "💬 ভেরিফাইকারী পাঠকদের অভিজ্ঞতা:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    withCommentsOrNames.forEach { entry ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = onePxBorder(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = "✅ ${entry.name.ifBlank { "একজন পাঠক" }} (সত্যায়িত)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                if (entry.comment.isNotBlank()) {
                                                    Text(
                                                        text = entry.comment,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
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

                // ── Personal Note Section (ব্যক্তিগত নোট) ─────────────────────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PersonalNoteCard(
                            savedNote = art.userNote,
                            onSaveNote = { newNote ->
                                viewModel.saveUserNote(newNote)
                                Toast.makeText(context, "📓 আপনার নোট সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // ── Bottom Reaction & Image Card Share Bar ───────────────────
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f).solidOverSurface()
                            ),
                            border = onePxBorder(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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

                                OutlinedButton(
                                    onClick = {
                                        isSharingImage = true
                                        coroutineScope.launch {
                                            ShareCardGenerator.shareArticleAsImageCard(context, art)
                                            isSharingImage = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = onePxBorder()
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("🖼️ ইমেজ কার্ড হিসেবে সোশ্যাল মিডিয়ায় শেয়ার করুন")
                                }
                            }
                        }
                    }
                }

                // ── Related / Suggested Articles Section (ডানে-বামে স্ক্রোলযোগ্য) ─────────────────
                if (relatedArticles.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔗 সম্পর্কিত আরও স্বাস্থ্যতথ্য",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "👈 ডানে-বামে স্ক্রোল করুন 👉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(relatedArticles, key = { it.id }) { rel ->
                                    val relHasImage = remember(rel.photoPath) {
                                        rel.photoPath.startsWith("http://") || rel.photoPath.startsWith("https://")
                                    }
                                    Card(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .clickable { onRelatedArticleClick(rel.id) },
                                        shape = RoundedCornerShape(14.dp),
                                        border = onePxBorder(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (relHasImage) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(rel.photoPath)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = rel.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            }
                                            Text(
                                                text = rel.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (rel.medicines.isNotEmpty()) {
                                                Text(
                                                    text = "🌿 ${rel.medicines.take(2).joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "বিস্তারিত দেখুন",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
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
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Popup Dialog when clicking the "পরীক্ষিত" (Verified) button:
 * - Includes truthfulness attestation & consent checkbox (REQUIRED)
 * - Optional Name, Email/Phone Number, and Comment fields
 * - Publishes globally to Firestore
 */
@Composable
fun VerificationConsentDialog(
    article: Article,
    onDismiss: () -> Unit,
    onSubmit: (name: String, contact: String, comment: String) -> Unit,
    onRemoveVerification: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var isConsentChecked by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = onePxBorder(),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "পরীক্ষিত ও সত্যায়ন ফরম",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isPublishing) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                // Post title preview
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = onePxBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📌 \"${article.title}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Optional Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("আপনার নাম (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: মো. আব্দুল্লাহ") },
                    singleLine = true,
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("ইমেইল বা মোবাইল নম্বর (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: 017XXXXXXXX বা email@example.com") },
                    singleLine = true,
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("মন্তব্য বা আপনার অভিজ্ঞতা (ঐচ্ছিক)") },
                    placeholder = { Text("কীভাবে উপকার পেয়েছেন তা সংক্ষেপে লিখতে পারেন...") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isPublishing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Required Truthfulness Attestation Checkbox
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConsentChecked)
                        Color(0xFFE8F5E9)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = onePxBorder(
                        if (isConsentChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isPublishing) { isConsentChecked = !isConsentChecked }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isConsentChecked,
                            onCheckedChange = { isConsentChecked = it },
                            enabled = !isPublishing,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2E7D32)
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "আমি সজ্ঞানে সত্যায়ন করছি যে এই সমাধানটি আমার নিজের পরীক্ষিত বা এর দ্বারা আমার কাজ হয়েছে এবং আমি সত্য বলছি। *",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isConsentChecked) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "* শুধুমাত্র এই সম্মতি টিক চিহ্নটি আবশ্যক, বাকি সব তথ্য ঐচ্ছিক।",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemoveVerification != null) {
                        TextButton(
                            onClick = onRemoveVerification,
                            enabled = !isPublishing
                        ) {
                            Text("তুলে নিন", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isPublishing
                        ) {
                            Text("বাতিল")
                        }
                    }

                    Button(
                        onClick = {
                            isPublishing = true
                            onSubmit(name, contact, comment)
                        },
                        enabled = isConsentChecked && !isPublishing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("প্রকাশ হচ্ছে...")
                        } else {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("প্রকাশ করুন (Publish)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalNoteCard(
    savedNote: String,
    onSaveNote: (String) -> Unit
) {
    var noteInput by remember(savedNote) { mutableStateOf(savedNote) }
    var isEditing by remember { mutableStateOf(false) }

    SectionCard(title = "📓 আপনার ব্যক্তিগত নোট") {
        if (!isEditing && savedNote.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = onePxBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = savedNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { isEditing = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = onePxBorder(),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("এডিট", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                noteInput = ""
                                onSaveNote("")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("মুছুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("এই আর্টিকেল সম্পর্কে আপনার নিজস্ব নোট বা অভিজ্ঞতা লিখে রাখুন...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 5
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing) {
                        TextButton(onClick = {
                            noteInput = savedNote
                            isEditing = false
                        }) {
                            Text("বাতিল")
                        }
                    }
                    Button(
                        onClick = {
                            onSaveNote(noteInput)
                            isEditing = false
                        },
                        enabled = noteInput.trim() != savedNote.trim()
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("নোট সেভ করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenPhotoViewerDialog(
    photoUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale
                        if (newScale > 1f) {
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-600f, 600f),
                                y = (offset.y + pan.y).coerceIn(-800f, 800f)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = remember(photoUrl) {
                    ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build()
                },
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Top bar with close & zoom reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "পিঞ্চ বা ডাবল-ট্যাপ করে জুম করুন (${(scale * 100).toInt()}%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun FontSizeControlBar(
    fontSizeSp: Float,
    themeMode: String = UserPreferencesManager.THEME_SYSTEM,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit,
    onSelectTheme: (String) -> Unit = {}
) {
    val percent = ((fontSizeSp / UserPreferencesManager.DEFAULT_FONT_SIZE) * 100).toInt()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            // Reading Theme Selector Row (Light / Sepia / Dark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "রিডিং থিম:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val themeChips = listOf(
                    UserPreferencesManager.THEME_LIGHT to "☀️ লাইট",
                    UserPreferencesManager.THEME_SEPIA to "📜 সেপিয়া",
                    UserPreferencesManager.THEME_DARK to "🌙 ডার্ক",
                    UserPreferencesManager.THEME_SYSTEM to "অটো"
                )
                themeChips.forEach { (mode, label) ->
                    val selected = themeMode == mode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (selected) null else onePxBorder(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTheme(mode) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
        border = onePxBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
