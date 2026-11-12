package com.health.knowledge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.health.knowledge.domain.model.Article

@Entity(tableName = "tibbe_nabawi_posts")
data class TibbeNabawiEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    @ColumnInfo(name = "food_name")
    val foodName: String = "",
    @ColumnInfo(name = "arabic_name")
    val arabicName: String? = null,
    val category: String = "",
    val diseases: String = "",               // comma-separated e.g. "জ্বর,ডায়রিয়া"
    val benefits: String? = null,
    val nature: String? = null,              // মেজাজ / প্রকৃতি
    val taste: String? = null,               // স্বাদ
    @ColumnInfo(name = "usage_instructions")
    val usageInstructions: String? = null,
    val details: String = "",
    val quotes: String? = null,              // Arabic Hadith / Quote text
    @ColumnInfo(name = "source_pages")
    val sourcePages: String = "",            // "5, 6, 7"
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,
    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = ""          // Precomputed search index
)

fun TibbeNabawiEntity.toArticleDomain(): Article {
    val allCategories = mutableListOf(category, "তিব্বে নববী")
    val allMedicines = mutableListOf<String>()

    // Extract clean food name without parenthesis
    val cleanFood = foodName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
    if (cleanFood.isNotBlank()) {
        allMedicines.add(cleanFood)
    }
    if (foodName.isNotBlank() && foodName != cleanFood) {
        allMedicines.add(foodName.trim())
    }

    if (diseases.isNotBlank()) {
        diseases.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            allCategories.add(it)
        }
    }

    val bodyContent = buildString {
        if (!arabicName.isNullOrBlank()) append("📌 আরবি নাম: $arabicName\n\n")
        if (!quotes.isNullOrBlank()) append("$quotes\n\n")
        if (foodName.isNotBlank()) append("🌱 ভেষজ/উপাদান: $foodName\n\n")
        if (!nature.isNullOrBlank()) append("🌡️ প্রকৃতি/মেজাজ: $nature\n\n")
        if (!taste.isNullOrBlank()) append("👅 স্বাদ: $taste\n\n")
        if (diseases.isNotBlank()) append("🩺 রোগসমূহ: $diseases\n\n")
        val preview = benefits ?: details
        append(preview)
        if (!usageInstructions.isNullOrBlank()) {
            append("\n\n🧪 সেবন ও ব্যবহারবিধি:\n$usageInstructions")
        }
    }

    return Article(
        id = -id,
        messageId = "tibbe_$id",
        title = title,
        content = bodyContent,
        date = "",
        reactions = 0,
        medicines = allMedicines,
        categories = allCategories,
        hasPhoto = false,
        photoPath = "",
        syncedFromFirebase = false,
        firebaseDocId = "",
        isBookmarked = isBookmarked,
        isPinned = false,
        isReacted = false,
        isReviewed = true,
        postKey = "tibbe_$id",
        source = "আত-তিব্বুন নববী (ইবনুল কাইয়্যিম রহিমাহুল্লাহ)${if (sourcePages.isNotBlank()) " • পৃ: $sourcePages" else ""}"
    )
}
