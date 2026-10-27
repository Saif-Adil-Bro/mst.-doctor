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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.health.knowledge.util.ShareCardGenerator
import com.health.knowledge.util.UserPreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    articleId: Int,
    onBack: () -> Unit,
    onRelatedArticleClick: (Int) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val relatedArticles by viewModel.relatedArticles.collectAsState()
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
    var isSharingImage by remember { mutableStateOf(false) }

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
                            val shareText = "${art.title}\n\n${art.content}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                        // Share (Text or Branded Social Media Image Card)
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
            val hasCdnImage = remember(art.photoPath) {
                art.photoPath.startsWith("http://") || art.photoPath.startsWith("https://")
            }

            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Font Size + Reading Theme Control Bar (A- / A+ / Light / Sepia / Dark)
                item {
                    FontSizeControlBar(
                        fontSizeSp = fontSizeSp,
                        themeMode = themeMode,
                        onDecrease = viewModel::decreaseFontSize,
                        onIncrease = viewModel::increaseFontSize,
                        onReset = viewModel::resetFontSize,
                        onSelectTheme = viewModel::setThemeMode
                    )
                }

                // ── Cover Photo from CDN URL (Tap for Fullscreen Pinch-to-Zoom)
                if (hasCdnImage) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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

                // ── Selectable Title ─────────────────────────────────────────
                item {
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

                // ── Meta info + Interactive Global Per-IP Reaction Button ────
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

                        // Reaction Button (1 per IP, synced globally via Firestore)
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

                // ── Selectable Medicines / ingredients section ───────────────
                if (art.medicines.isNotEmpty()) {
                    item {
                        SectionCard(title = "💊 উপাদান / ভেষজ") {
                            SelectionContainer {
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
                }

                // ── Selectable Main content (scales with fontSizeSp) ─────────
                item {
                    SectionCard(title = "📝 বিস্তারিত সমাধান (টেক্সট সিলেক্ট করা যাবে)") {
                        SelectionContainer {
                            Text(
                                text = art.content,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.6f).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // ── Personal Note Section (ব্যক্তিগত নোট) ─────────────────────
                item {
                    PersonalNoteCard(
                        savedNote = art.userNote,
                        onSaveNote = { newNote ->
                            viewModel.saveUserNote(newNote)
                            Toast.makeText(context, "📓 আপনার নোট সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // ── Bottom Reaction & Image Card Share Bar ───────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
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
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("🖼️ ইমেজ কার্ড হিসেবে সোশ্যাল মিডিয়ায় শেয়ার করুন")
                            }
                        }
                    }
                }

                // ── Related Articles Section ─────────────────────────────────
                if (relatedArticles.isNotEmpty()) {
                    item {
                        SectionCard(title = "🔗 সম্পর্কিত আরও স্বাস্থ্যতথ্য") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                relatedArticles.forEach { rel ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onRelatedArticleClick(rel.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rel.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (rel.medicines.isNotEmpty()) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "🌿 ${rel.medicines.take(3).joinToString(", ")}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
