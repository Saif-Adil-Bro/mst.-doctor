package com.health.knowledge.ui.tibbenabawi

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import com.health.knowledge.ui.theme.SolaimanLipi
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.ArabicStandaloneBlock
import com.health.knowledge.util.SmartMultilingualBody

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TibbeNabawiDetailScreen(
    postId: Int,
    onBack: () -> Unit,
    onRelatedPostClick: (Int) -> Unit = {},
    viewModel: TibbeNabawiDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsState()
    val relatedPosts by viewModel.relatedPosts.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val emeraldPrimary = Color(0xFF1B6B46)

    fun buildShareText(item: TibbeNabawiEntity): String {
        val sb = StringBuilder()
        sb.append("🌿 ${item.title}\n")
        if (!item.arabicName.isNullOrBlank()) {
            sb.append("📌 আরবি নাম: ${item.arabicName}\n")
        }
        if (item.foodName.isNotBlank()) {
            sb.append("🌱 উপাদান: ${item.foodName}\n")
        }
        if (!item.nature.isNullOrBlank()) {
            sb.append("🌡️ প্রকৃতি/মেজাজ: ${item.nature}\n")
        }
        if (!item.taste.isNullOrBlank()) {
            sb.append("👅 স্বাদ: ${item.taste}\n")
        }
        if (item.diseases.isNotBlank()) {
            sb.append("🩺 রোগসমূহ: ${item.diseases}\n")
        }
        if (!item.quotes.isNullOrBlank()) {
            sb.append("\n📜 হাদিস/উদ্ধৃতি (আরবি):\n${item.quotes}\n")
        }
        sb.append("\n📖 বিবরণ ও হাদিস:\n${item.details}\n")
        if (!item.usageInstructions.isNullOrBlank()) {
            sb.append("\n🧪 সেবন ও ব্যবহারবিধি:\n${item.usageInstructions}\n")
        }
        sb.append("\n📚 সূত্র: আত-তিব্বুন নববী (ইবনুল কাইয়্যিম রহিমাহুল্লাহ)")
        if (item.sourcePages.isNotBlank()) {
            sb.append(" • পৃষ্ঠা: ${item.sourcePages}")
        }
        return sb.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "তিব্বে নববী পথ্য",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে যান")
                    }
                },
                actions = {
                    post?.let { item ->
                        // Bookmark
                        IconButton(onClick = { viewModel.toggleBookmark() }) {
                            Icon(
                                imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "বুকমার্ক",
                                tint = if (item.isBookmarked) emeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Copy
                        IconButton(onClick = {
                            val text = buildShareText(item)
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "অনুলিপি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি")
                        }

                        // Share
                        IconButton(onClick = {
                            val text = buildShareText(item)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                            }
                            context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "শেয়ার")
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
            val currentPost = post

        if (currentPost == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = emeraldPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── 1. Hero Header Card ──────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = emeraldPrimary.copy(alpha = 0.08f).solidOverSurface()
                    ),
                    border = onePxBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category & Page Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = emeraldPrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currentPost.category,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (currentPost.sourcePages.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = onePxBorder()
                                ) {
                                    Text(
                                        text = "📖 পৃষ্ঠা: ${currentPost.sourcePages}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Title
                        Text(
                            text = currentPost.title,
                            fontFamily = SolaimanLipi,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp
                        )

                        // Arabic Name Hero Banner - Standalone Component with Middle-Center Alignment & RTL
                        if (!currentPost.arabicName.isNullOrBlank()) {
                            ArabicStandaloneBlock(
                                arabicText = currentPost.arabicName,
                                fontSize = 22.sp,
                                textColor = emeraldPrimary,
                                backgroundColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }

                // ── 2. Quick Specs Properties (Food, Nature, Taste) ─────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = onePxBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🌱 মূল উপাদান ও চারিত্রিক বৈশিষ্ট্য",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = emeraldPrimary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("উপাদান / খাদ্য", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currentPost.foodName.ifBlank { "সাধারণ" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("প্রকৃতি / মেজাজ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currentPost.nature ?: "উল্লেখ নেই",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("স্বাদ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currentPost.taste ?: "উল্লেখ নেই",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Target Diseases Flow Chips
                        if (currentPost.diseases.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "যেসব রোগের নিরাময়ে কার্যকর:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentPost.diseases.split(",").forEach { disease ->
                                    val trimmed = disease.trim()
                                    if (trimmed.isNotEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "• $trimmed",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. Hadith & Detailed Explanation ─────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = onePxBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = emeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "হাদিস ও প্রামাণ্য বিবরণ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = emeraldPrimary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // ── Dedicated Arabic Hadith / Quote Section ────────────────
                        if (!currentPost.quotes.isNullOrBlank()) {
                            ArabicStandaloneBlock(
                                arabicText = currentPost.quotes,
                                fontSize = 21.sp,
                                textColor = emeraldPrimary,
                                backgroundColor = emeraldPrimary.copy(alpha = 0.08f).solidOverSurface()
                            )
                        }

                        SelectionContainer {
                            SmartMultilingualBody(
                                content = currentPost.details,
                                fontSizeSp = 15f,
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // ── 4. Usage Instructions Box ────────────────────────
                if (!currentPost.usageInstructions.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f).solidOverSurface()
                        ),
                        border = onePxBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🧪 সেবন ও ব্যবহারবিধি",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            SelectionContainer {
                                SmartMultilingualBody(
                                    content = currentPost.usageInstructions,
                                    fontSizeSp = 14f,
                                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // ── 5. Book Reference Footer ─────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = onePxBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📚 রেফারেন্স তথ্যসূত্র:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = emeraldPrimary
                        )
                        Text(
                            text = "গ্রন্থ: আত-তিব্বুন নববী (إكرام الطب النبوي) — ইমাম ইবনুল কাইয়্যিম আল-জাওযিয়্যাহ (রহিমাহুল্লাহ)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentPost.sourcePages.isNotBlank()) {
                            Text(
                                text = "মূল কিতাবের পৃষ্ঠা: ${currentPost.sourcePages}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // ── 6. Related Posts in Same Category ────────────────
                if (relatedPosts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "এই ক্যাটাগরির আরও পথ্য ও চিকিৎসা",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(relatedPosts, key = { it.id }) { item ->
                                Card(
                                    onClick = { onRelatedPostClick(item.id) },
                                    modifier = Modifier.width(220.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = onePxBorder()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.foodName.isNotBlank()) {
                                            Text(
                                                text = "🌿 ${item.foodName}",
                                                fontSize = 11.sp,
                                                color = emeraldPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
        }
    }
}
