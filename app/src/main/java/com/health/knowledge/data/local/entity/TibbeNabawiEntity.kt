package com.health.knowledge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    @ColumnInfo(name = "source_pages")
    val sourcePages: String = "",            // "5, 6, 7"
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,
    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = ""          // Precomputed search index
)
