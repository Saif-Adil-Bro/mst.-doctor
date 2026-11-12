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
    var raw = quotes?.trim()
    if (raw.isNullOrBlank()) return emptyList()

    // 1. If wrapped in outer quotes or doubly escaped
    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length > 2) {
        raw = raw.substring(1, raw.length - 1).replace("\\\"", "\"").replace("\\\\", "\\").trim()
    }

    // 2. Try standard or lenient JSON parsing
    try {
        val jsonCleaned = raw.trim()
        if (jsonCleaned.startsWith("[") || jsonCleaned.startsWith("{")) {
            val reader = com.google.gson.stream.JsonReader(java.io.StringReader(jsonCleaned)).apply {
                isLenient = true
            }
            val element = com.google.gson.JsonParser.parseReader(reader)
            val list = mutableListOf<TibbeQuote>()

            fun processObject(obj: com.google.gson.JsonObject) {
                val type = obj.get("type")?.asString?.trim().takeUnless { it.isNullOrBlank() } ?: "হাদিস"
                val arabic = (obj.get("arabic") ?: obj.get("text") ?: obj.get("quote") ?: obj.get("matn"))?.asString?.trim().orEmpty()
                val translation = (obj.get("translation") ?: obj.get("bangla") ?: obj.get("meaning"))?.asString?.trim().orEmpty()
                val reference = (obj.get("reference") ?: obj.get("source") ?: obj.get("book"))?.asString?.trim().orEmpty()
                if (arabic.isNotBlank() || translation.isNotBlank()) {
                    list.add(TibbeQuote(type = type, arabic = arabic, translation = translation, reference = reference))
                }
            }

            if (element.isJsonArray) {
                for (item in element.asJsonArray) {
                    if (item.isJsonObject) {
                        processObject(item.asJsonObject)
                    } else if (item.isJsonPrimitive) {
                        val s = item.asString.trim()
                        if (s.isNotBlank() && !s.contains("\"type\"")) {
                            list.add(TibbeQuote(type = "হাদিস", arabic = s))
                        }
                    }
                }
                if (list.isNotEmpty()) return list
            } else if (element.isJsonObject) {
                processObject(element.asJsonObject)
                if (list.isNotEmpty()) return list
            }
        }
    } catch (_: Exception) {
        // Fall through to regex extraction
    }

    // 3. Resilient Regex Extraction for damaged, escaped or concatenated JSON strings
    if (raw.contains("arabic") || raw.contains("translation") || raw.contains("\"type\"")) {
        val extractedList = mutableListOf<TibbeQuote>()
        val blockRegex = Regex("""\{[^{}]*\}""")
        val blocks = blockRegex.findAll(raw).map { it.value }.toList()
        val targets = if (blocks.isNotEmpty()) blocks else listOf(raw)

        fun extractField(block: String, key: String): String {
            val pat = Regex("""\"$key\"\s*:\s*\"((?:\\.|[^\"\\])*)\"""")
            val m = pat.find(block)
            return m?.groupValues?.getOrNull(1)?.replace("\\\"", "\"")?.replace("\\n", "\n")?.trim().orEmpty()
        }

        for (b in targets) {
            val type = extractField(b, "type").ifBlank { "হাদিস" }
            val ar = extractField(b, "arabic").ifBlank { extractField(b, "matn") }
            val tr = extractField(b, "translation").ifBlank { extractField(b, "bangla") }
            val rf = extractField(b, "reference").ifBlank { extractField(b, "source") }
            if (ar.isNotBlank() || tr.isNotBlank()) {
                extractedList.add(TibbeQuote(type = type, arabic = ar, translation = tr, reference = rf))
            }
        }
        if (extractedList.isNotEmpty()) return extractedList
    }

    // 4. Safe fallback: Never treat raw JSON syntax as Arabic matn!
    val isRawJson = raw.startsWith("{") || raw.startsWith("[") || raw.contains("\"type\":") || raw.contains("\"arabic\":")
    if (isRawJson) {
        val cleanAr = raw.replace(Regex("""[{}\[\]"]|type:|arabic:|translation:|reference:"""), "").trim()
        return if (cleanAr.isNotBlank()) listOf(TibbeQuote(type = "হাদিস", arabic = cleanAr)) else emptyList()
    }

    return listOf(TibbeQuote(type = "হাদিস", arabic = raw))
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
        source = "আত-তিব্বুন নববী (ইবনুল কাইয়্যিম রহিমাহুল্লাহ)${if (sourcePages.isNotBlank()) " • পৃ: ${formatPageRangeBn(sourcePages)}" else ""}"
    )
}

/**
 * Converts ASCII digits 0-9 in a string to Bengali digits ০-৯.
 */
fun toBengaliDigits(str: String): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return str.map { ch ->
        if (ch in '0'..'9') bnDigits[ch - '0'] else ch
    }.joinToString("")
}

/**
 * Formats comma-separated page numbers (e.g. "17, 18, 19, 20" or "১৭, ১৮, ১৯, ২০")
 * into clean Bengali consecutive page ranges (e.g. "১৭ থেকে ২০").
 */
fun formatPageRangeBn(sourcePages: String): String {
    if (sourcePages.isBlank()) return ""
    val normalized = sourcePages.map { ch ->
        when (ch) {
            in '০'..'৯' -> '0' + (ch - '০')
            else -> ch
        }
    }.joinToString("")

    val numbers = Regex("""\d+""").findAll(normalized)
        .mapNotNull { it.value.toIntOrNull() }
        .sorted()
        .distinct()
        .toList()

    if (numbers.isEmpty()) {
        return toBengaliDigits(sourcePages)
    }

    val ranges = mutableListOf<Pair<Int, Int>>()
    var rangeStart = numbers[0]
    var prev = numbers[0]

    for (i in 1 until numbers.size) {
        val curr = numbers[i]
        if (curr == prev + 1) {
            prev = curr
        } else {
            ranges.add(Pair(rangeStart, prev))
            rangeStart = curr
            prev = curr
        }
    }
    ranges.add(Pair(rangeStart, prev))

    return ranges.joinToString(", ") { (start, end) ->
        if (start == end) {
            toBengaliDigits(start.toString())
        } else {
            "${toBengaliDigits(start.toString())} থেকে ${toBengaliDigits(end.toString())}"
        }
    }
}
