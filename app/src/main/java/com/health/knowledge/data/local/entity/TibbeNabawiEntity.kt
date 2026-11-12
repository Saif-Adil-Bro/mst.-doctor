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

data class TibbeQuote(
    val type: String = "হাদিস",
    val arabic: String = "",
    val translation: String = "",
    val reference: String = ""
)

fun TibbeNabawiEntity.parsedQuotes(): List<TibbeQuote> {
    val raw = quotes?.trim()
    if (raw.isNullOrBlank()) return emptyList()

    return try {
        if (raw.startsWith("[")) {
            val element = com.google.gson.JsonParser.parseString(raw)
            if (element.isJsonArray) {
                val array = element.asJsonArray
                val list = mutableListOf<TibbeQuote>()
                for (item in array) {
                    if (item.isJsonObject) {
                        val obj = item.asJsonObject
                        val type = obj.get("type")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: "হাদিস"
                        val arabic = (obj.get("arabic") ?: obj.get("text") ?: obj.get("quote") ?: obj.get("matn"))?.asString?.trim().orEmpty()
                        val translation = (obj.get("translation") ?: obj.get("bangla") ?: obj.get("meaning"))?.asString?.trim().orEmpty()
                        val reference = (obj.get("reference") ?: obj.get("source") ?: obj.get("book"))?.asString?.trim().orEmpty()
                        if (arabic.isNotBlank() || translation.isNotBlank()) {
                            list.add(TibbeQuote(type = type, arabic = arabic, translation = translation, reference = reference))
                        }
                    } else if (item.isJsonPrimitive) {
                        val s = item.asString.trim()
                        if (s.isNotBlank()) {
                            list.add(TibbeQuote(type = "হাদিস", arabic = s))
                        }
                    }
                }
                if (list.isNotEmpty()) return list
            }
        } else if (raw.startsWith("{")) {
            val element = com.google.gson.JsonParser.parseString(raw)
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val type = obj.get("type")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: "হাদিস"
                val arabic = (obj.get("arabic") ?: obj.get("text") ?: obj.get("quote") ?: obj.get("matn"))?.asString?.trim().orEmpty()
                val translation = (obj.get("translation") ?: obj.get("bangla") ?: obj.get("meaning"))?.asString?.trim().orEmpty()
                val reference = (obj.get("reference") ?: obj.get("source") ?: obj.get("book"))?.asString?.trim().orEmpty()
                if (arabic.isNotBlank() || translation.isNotBlank()) {
                    return listOf(TibbeQuote(type = type, arabic = arabic, translation = translation, reference = reference))
                }
            }
        }
        listOf(TibbeQuote(type = "হাদিস", arabic = raw))
    } catch (_: Exception) {
        listOf(TibbeQuote(type = "হাদিস", arabic = raw))
    }
}

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
        val quotesList = parsedQuotes()
        if (quotesList.isNotEmpty()) {
            quotesList.forEach { q ->
                append("📜 ${q.type.ifBlank { "হাদিস" }}:\n")
                if (q.arabic.isNotBlank()) append("${q.arabic}\n")
                if (q.translation.isNotBlank()) append("অনুবাদ: ${q.translation}\n")
                if (q.reference.isNotBlank()) append("সূত্র: ${q.reference}\n")
                append("\n")
            }
        }
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
