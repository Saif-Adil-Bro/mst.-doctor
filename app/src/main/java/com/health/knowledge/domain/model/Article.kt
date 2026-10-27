package com.health.knowledge.domain.model

import com.health.knowledge.data.local.entity.ArticleEntity

data class Article(
    val id: Int,
    val messageId: String,
    val title: String,
    val content: String,
    val date: String,
    val reactions: Int,
    val medicines: List<String>,
    val categories: List<String>,
    val hasPhoto: Boolean,
    val photoPath: String,
    val syncedFromFirebase: Boolean,
    val isBookmarked: Boolean
)

fun ArticleEntity.toDomain() = Article(
    id = id,
    messageId = messageId,
    title = title,
    content = content,
    date = date,
    reactions = reactions,
    medicines = medicines.split(",").filter { it.isNotBlank() },
    categories = categories.split(",").filter { it.isNotBlank() },
    hasPhoto = hasPhoto,
    photoPath = photoPath,
    syncedFromFirebase = syncedFromFirebase,
    isBookmarked = isBookmarked
)
