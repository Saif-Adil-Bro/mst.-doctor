package com.health.knowledge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messageId: String = "",
    val title: String,
    val content: String,
    val date: String = "",
    val reactions: Int = 0,
    val medicines: String = "",      // comma-separated: "হলুদ,আদা,মধু"
    val categories: String = "",     // comma-separated: "চোখ,পেট"
    val hasPhoto: Boolean = false,
    val photoPath: String = "",
    val searchKeywords: String = "", // for full-text search
    val syncedFromFirebase: Boolean = false,
    val firebaseDocId: String = "",
    val isBookmarked: Boolean = false,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val isReacted: Boolean = false,
    val isReviewed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = 0L,
    val userNote: String = "",
    val isDraft: Boolean = false,
    val isMine: Boolean = false,
    val authorDeviceId: String = "",
    val source: String = "",
    val verifiedCount: Int = 0,
    val isVerified: Boolean = false
) {
    val postKey: String
        get() = if (firebaseDocId.isNotBlank()) "fb_$firebaseDocId"
        else "bundled_${messageId.ifBlank { id.toString() }}"
}
