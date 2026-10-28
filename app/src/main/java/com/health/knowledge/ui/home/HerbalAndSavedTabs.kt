package com.health.knowledge.ui.home

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.health.knowledge.domain.model.Article
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.UserPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyHealthTipCard(
    article: Article,
    reminderEnabled: Boolean,
    reminderTimeText: String = "",
    onReadClick: () -> Unit,
    onNextTipClick: () -> Unit,
    onToggleReminder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(18.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f).solidOverSurface()
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💡 আজকের স্বাস্থ্য টিপস",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminderEnabled && reminderTimeText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onToggleReminder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = reminderTimeText,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onToggleReminder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (reminderEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = "রিমাইন্ডার",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextTipClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "পরবর্তী টিপস",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = article.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (article.medicines.isNotEmpty()) {
                    Text(
                        text = "🌿 ${article.medicines.take(3).joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = "বিস্তারিত পড়ুন →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SortAndResultsHeader(
    headerText: String,
    sortOrder: String,
    onSortChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortChipItem(
                    label = "✨ নতুন",
                    selected = sortOrder == UserPreferencesManager.SORT_NEWEST,
                    onClick = { onSortChange(UserPreferencesManager.SORT_NEWEST) }
                )
                SortChipItem(
                    label = "🔥 জনপ্রিয়",
                    selected = sortOrder == UserPreferencesManager.SORT_POPULAR,
                    onClick = { onSortChange(UserPreferencesManager.SORT_POPULAR) }
                )
                SortChipItem(
                    label = "📜 পুরাতন",
                    selected = sortOrder == UserPreferencesManager.SORT_OLDEST,
                    onClick = { onSortChange(UserPreferencesManager.SORT_OLDEST) }
                )
            }
        }
    }
}

@Composable
private fun SortChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = if (selected) null else onePxBorder(MaterialTheme.colorScheme.outlineVariant),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ThemeAndReminderDialog(
    currentThemeMode: String,
    reminderEnabled: Boolean,
    reminderHour: Int = 8,
    reminderMinute: Int = 0,
    formattedReminderTime: String = "সকাল ০৮:০০ (AM)",
    onSelectTheme: (String) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onSendTestNotification: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onToggleReminder(isGranted)
        if (isGranted) {
            Toast.makeText(context, "🔔 দৈনিক স্বাস্থ্য টিপস রিমাইন্ডার চালু হয়েছে!", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("🎨 রিডিং থিম ও রিমাইন্ডার", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("আপনার চোখের আরামের জন্য পছন্দের রিডিং মোড বেছে নিন:", style = MaterialTheme.typography.bodySmall)

                val themes = listOf(
                    Triple(UserPreferencesManager.THEME_LIGHT, "☀️ লাইট মোড (Pure Light)", "দিনের বেলা পড়ার জন্য পরিষ্কার সাদা-সবুজ থিম"),
                    Triple(UserPreferencesManager.THEME_SEPIA, "📜 সেপিয়া রিডিং মোড (Eye Care)", "দীর্ঘক্ষণ পড়ার জন্য চোখের আরামদায়ক ওয়ার্ম পেপার থিম"),
                    Triple(UserPreferencesManager.THEME_DARK, "🌙 ডার্ক মোড (Night Dark)", "রাতের বেলা কম আলোতে পড়ার জন্য ডার্ক থিম"),
                    Triple(UserPreferencesManager.THEME_SYSTEM, "📱 সিস্টেম ডিফল্ট", "ফোনের সেটিংস অনুযায়ী স্বয়ংক্রিয় পরিবর্তন")
                )

                themes.forEach { (mode, title, subtitle) ->
                    val isSelected = currentThemeMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.solidOverSurface()
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f).solidOverSurface(),
                        border = onePxBorder(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTheme(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = isSelected, onClick = { onSelectTheme(mode) })
                        }
                    }
                }

                HorizontalDivider(thickness = com.health.knowledge.ui.theme.onePxDp())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔔 দৈনিক স্বাস্থ্য টিপস রিমাইন্ডার", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("প্রতিদিনের কাস্টমাইজড টাইমে পুশ নোটিফিকেশন পান", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onToggleReminder(wantEnable)
                            }
                        }
                    )
                }

                // Customized Notification Time Selector
                if (reminderEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f).solidOverSurface(),
                        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        onTimeSelected(selectedHour, selectedMinute)
                                        Toast.makeText(
                                            context,
                                            "🔔 রিমাইন্ডারের সময় সেট করা হয়েছে!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    reminderHour,
                                    reminderMinute,
                                    false
                                ).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "নোটিফিকেশনের সময়",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "ডিফল্ট: সকাল ০৮:০০ টা (ট্যাপ করে পরিবর্তন করুন)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = formattedReminderTime,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = onePxBorder(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("টেস্ট নোটিফিকেশন দেখুন", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("সম্পন্ন")
            }
        }
    )
}

@Composable
fun HerbalIndexTabContent(
    herbalIndex: List<HerbalIngredientStat>,
    herbSearchQuery: String,
    selectedHerb: String?,
    selectedMultiCategories: Set<String>,
    filteredArticles: List<Article>,
    isAdminMode: Boolean,
    onHerbQueryChange: (String) -> Unit,
    onSelectHerb: (String?) -> Unit,
    onToggleMultiCategory: (String) -> Unit,
    onClearFilters: () -> Unit,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onReactionClick: (Article) -> Unit,
    onVerifyClick: (Article) -> Unit = {},
    onPinToggle: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllHerbs by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Herbal & Multi-Filter Header Card
        item(key = "herbal_header") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                border = onePxBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f).solidOverSurface()
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🌿 ভেষজ ও উপাদান ইনডেক্স (${herbalIndex.size}টি)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ভেষজ উপাদান বা একাধিক বিভাগ একসাথে ফিল্টার করুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedHerb != null || selectedMultiCategories.isNotEmpty() || herbSearchQuery.isNotBlank()) {
                            TextButton(onClick = onClearFilters) {
                                Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("রিসেট", fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = herbSearchQuery,
                        onValueChange = onHerbQueryChange,
                        placeholder = { Text("ভেষজ উপাদান বা রোগ খুঁজুন (যেমন: হলুদ, আদা, মধু)...") },
                        leadingIcon = { Icon(Icons.Default.Eco, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (herbSearchQuery.isNotBlank()) {
                                IconButton(onClick = { onHerbQueryChange("") }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Multi-select category chips
                    Text("বিভাগ ফিল্টার (একাধিক নির্বাচন করা যাবে):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { (cat, emoji) ->
                            val isSelected = selectedMultiCategories.contains(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleMultiCategory(cat) },
                                label = { Text(cat.replace("_", " ")) },
                                leadingIcon = { Text(emoji) }
                            )
                        }
                    }

                    // Herbal ingredients cloud
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "জনপ্রিয় ভেষজ উপাদানসমূহ:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (herbalIndex.size > 12) {
                            TextButton(onClick = { showAllHerbs = !showAllHerbs }) {
                                Text(if (showAllHerbs) "সংক্ষিপ্ত করুন ▲" else "সব দেখুন (${herbalIndex.size}) ▼", fontSize = 12.sp)
                            }
                        }
                    }

                    val displayedHerbs = if (showAllHerbs) herbalIndex.take(60) else herbalIndex.take(12)
                    val chunkedHerbs = displayedHerbs.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunkedHerbs.forEach { rowHerbs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowHerbs.forEach { stat ->
                                    val selected = selectedHerb == stat.name
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (selected) null else onePxBorder(MaterialTheme.colorScheme.outlineVariant),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectHerb(stat.name) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🌿 ${stat.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${stat.count}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowHerbs.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Filtered results count
        item(key = "herbal_results_header") {
            val activeFilters = buildList {
                selectedHerb?.let { add("🌿 $it") }
                if (selectedMultiCategories.isNotEmpty()) {
                    add(selectedMultiCategories.joinToString(", ") { it.replace("_", " ") })
                }
            }.joinToString(" + ")

            Text(
                text = if (activeFilters.isNotEmpty()) {
                    "$activeFilters বিষয়ক সমাধান (${filteredArticles.size}টি)"
                } else {
                    "সকল ভেষজ স্বাস্থ্যতথ্য (${filteredArticles.size}টি)"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 3. Matching articles
        items(
            items = filteredArticles,
            key = { "herb_art_${it.id}" }
        ) { article ->
            ArticleCard(
                article = article,
                isAdminMode = isAdminMode,
                onClick = { onArticleClick(article.id) },
                onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) },
                onReactionClick = { onReactionClick(article) },
                onVerifyClick = { onVerifyClick(article) },
                onPinToggle = { onPinToggle(article) },
                onDeleteClick = { onDeleteClick(article) }
            )
        }
    }
}

@Composable
fun SavedAndHistoryTabContent(
    savedSubTab: String,
    bookmarkedArticles: List<Article>,
    historyArticles: List<Article>,
    notedArticles: List<Article>,
    myArticles: List<Article> = emptyList(),
    myPendingCount: Int = 0,
    myApprovedCount: Int = 0,
    myTotalReactions: Int = 0,
    listState: LazyListState,
    isAdminMode: Boolean,
    onSubTabChange: (String) -> Unit,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onReactionClick: (Article) -> Unit,
    onVerifyClick: (Article) -> Unit = {},
    onPinToggle: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit,
    onClearHistory: () -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String, (Result<Int>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                Toast.makeText(context, "✅ ব্যাকআপ ফাইল সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ ব্যাকআপ সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
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
                onImportBackup(json) { result ->
                    result.onSuccess { count ->
                        Toast.makeText(context, "✅ $count টি বুকমার্ক/নোট সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_LONG).show()
                    }.onFailure {
                        Toast.makeText(context, "❌ সঠিক ব্যাকআপ JSON ফাইল নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ ফাইল পড়তে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tabs + Backup/Restore Action Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            border = onePxBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f).solidOverSurface()
            )
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = savedSubTab == "bookmarks",
                            onClick = { onSubTabChange("bookmarks") },
                            label = { Text("🔖 সেভ (${bookmarkedArticles.size})", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = savedSubTab == "my_posts",
                            onClick = { onSubTabChange("my_posts") },
                            label = { Text("✍️ আমার পোস্ট (${myArticles.size})", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = savedSubTab == "history",
                            onClick = { onSubTabChange("history") },
                            label = { Text("🕒 হিস্ট্রি (${historyArticles.size})", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = savedSubTab == "notes",
                            onClick = { onSubTabChange("notes") },
                            label = { Text("📓 নোটস (${notedArticles.size})", fontSize = 12.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onExportBackup { json ->
                                    pendingExportJson = json
                                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("health_knowledge_backup_$dateStr.json")
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ব্যাকআপ এক্সপোর্ট", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("রিস্টোর", fontSize = 11.sp)
                        }
                    }

                    if (savedSubTab == "history" && historyArticles.isNotEmpty()) {
                        TextButton(
                            onClick = onClearHistory,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("মুছুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        val activeList = when (savedSubTab) {
            "history" -> historyArticles
            "notes" -> notedArticles
            "my_posts" -> myArticles
            else -> bookmarkedArticles
        }

        val emptyMessage = when (savedSubTab) {
            "history" -> "এখনো কোনো আর্টিকেল পড়া হয়নি"
            "notes" -> "কোনো আর্টিকেলে এখনো ব্যক্তিগত নোট যোগ করা হয়নি"
            "my_posts" -> "এই ডিভাইস থেকে এখনও কোনো পোস্ট যোগ করা হয়নি। হোম পেজের '+ পোস্ট করুন' বাটন থেকে পোস্ট করতে পারবেন!"
            else -> "কোনো সেভ করা আর্টিকেল নেই"
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Analytics Dashboard for My Posts
            if (savedSubTab == "my_posts" && myArticles.isNotEmpty()) {
                item(key = "my_post_analytics_dashboard") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f).solidOverSurface()
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📊", fontSize = 16.sp)
                                Text(
                                    "আপনার পোস্টের লাইভ অ্যানালিটিক্স",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MyMetricBox(
                                    title = "মোট পোস্ট",
                                    value = "${myArticles.size}",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MyMetricBox(
                                    title = "⏳ পেন্ডিং",
                                    value = "$myPendingCount",
                                    color = Color(0xFFFF8F00),
                                    modifier = Modifier.weight(1f)
                                )
                                MyMetricBox(
                                    title = "✅ অনুমোদিত",
                                    value = "$myApprovedCount",
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                MyMetricBox(
                                    title = "❤️ রিয়্যাকশন",
                                    value = "$myTotalReactions",
                                    color = Color(0xFFE91E63),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (activeList.isEmpty()) {
                item(key = "empty_saved_subtab") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (savedSubTab) {
                                    "history" -> "🕒"
                                    "notes" -> "📓"
                                    "my_posts" -> "✍️"
                                    else -> "🔖"
                                },
                                fontSize = 42.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(
                    items = activeList,
                    key = { "${savedSubTab}_${it.id}" }
                ) { article ->
                    Column {
                        if (savedSubTab == "notes" && article.userNote.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = "📓 আপনার নোট: ${article.userNote}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        ArticleCard(
                            article = article,
                            isAdminMode = isAdminMode,
                            onClick = { onArticleClick(article.id) },
                            onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) },
                            onReactionClick = { onReactionClick(article) },
                            onVerifyClick = { onVerifyClick(article) },
                            onPinToggle = { onPinToggle(article) },
                            onDeleteClick = { onDeleteClick(article) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyMetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
