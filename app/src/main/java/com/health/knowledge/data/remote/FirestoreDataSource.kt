package com.health.knowledge.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.health.knowledge.data.local.entity.ArticleEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val collection = db.collection("healthArticles")
    private val metaCollection = db.collection("postMeta")

    /**
     * Fetches all articles from Firestore.
     */
    suspend fun fetchAllArticles(): List<FirestoreArticle> {
        return try {
            val snapshot = collection
                .orderBy("createdAt")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreArticle::class.java)?.copy(firebaseDocId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches only NEW articles added after a given timestamp.
     */
    suspend fun fetchNewArticlesSince(lastSyncTimestamp: Long): List<FirestoreArticle> {
        return try {
            val snapshot = collection
                .whereGreaterThan("createdAt", lastSyncTimestamp)
                .orderBy("createdAt")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreArticle::class.java)?.copy(firebaseDocId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches global overrides (pins, edits, deletes, reaction counts) for both bundled and Firebase articles.
     */
    suspend fun fetchAllPostMeta(): List<PostMetaOverride> {
        return try {
            val snapshot = metaCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(PostMetaOverride::class.java)?.copy(postKey = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Admin: Upload a new article to Firestore.
     */
    suspend fun addArticle(article: FirestoreArticle): Result<String> {
        return try {
            val docRef = collection.add(article).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Pin or unpin an article globally.
     */
    suspend fun setPinnedGlobal(postKey: String, firebaseDocId: String, isPinned: Boolean): Result<Unit> {
        return try {
            val data = mapOf(
                "postKey" to postKey,
                "isPinned" to isPinned,
                "updatedAt" to System.currentTimeMillis()
            )
            metaCollection.document(postKey).set(data, SetOptions.merge()).await()
            if (firebaseDocId.isNotBlank()) {
                collection.document(firebaseDocId).set(mapOf("isPinned" to isPinned), SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Delete an article globally across all devices.
     */
    suspend fun deleteArticleGlobal(postKey: String, firebaseDocId: String): Result<Unit> {
        return try {
            val data = mapOf(
                "postKey" to postKey,
                "isDeleted" to true,
                "isPinned" to false,
                "updatedAt" to System.currentTimeMillis()
            )
            metaCollection.document(postKey).set(data, SetOptions.merge()).await()
            if (firebaseDocId.isNotBlank()) {
                collection.document(firebaseDocId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: Edit / Review an article globally.
     */
    suspend fun updateArticleGlobal(
        postKey: String,
        firebaseDocId: String,
        title: String,
        content: String,
        medicines: String,
        categories: String,
        isReviewed: Boolean
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val data = mapOf(
                "postKey" to postKey,
                "title" to title,
                "content" to content,
                "medicines" to medicines,
                "categories" to categories,
                "isReviewed" to isReviewed,
                "hasContentOverride" to true,
                "updatedAt" to now
            )
            metaCollection.document(postKey).set(data, SetOptions.merge()).await()
            if (firebaseDocId.isNotBlank()) {
                collection.document(firebaseDocId).set(
                    mapOf(
                        "title" to title,
                        "content" to content,
                        "medicines" to medicines,
                        "categories" to categories,
                        "isReviewed" to isReviewed
                    ),
                    SetOptions.merge()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Per-IP Reaction: Ensures each IP hash can react at most once per post in Firestore.
     */
    suspend fun syncReactionByIp(
        postKey: String,
        firebaseDocId: String,
        ipHash: String,
        wantReact: Boolean,
        newLocalReactionCount: Int
    ): Result<Unit> {
        return try {
            val metaRef = metaCollection.document(postKey)
            val reactorRef = metaRef.collection("reactors").document(ipHash)

            if (wantReact) {
                reactorRef.set(
                    mapOf(
                        "ipHash" to ipHash,
                        "reactedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                reactorRef.delete().await()
            }

            metaRef.set(
                mapOf(
                    "postKey" to postKey,
                    "reactions" to newLocalReactionCount,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            if (firebaseDocId.isNotBlank()) {
                collection.document(firebaseDocId).set(
                    mapOf("reactions" to newLocalReactionCount),
                    SetOptions.merge()
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class FirestoreArticle(
        val firebaseDocId: String = "",
        val title: String = "",
        val content: String = "",
        val date: String = "",
        val reactions: Int = 0,
        val medicines: String = "",
        val categories: String = "",
        val hasPhoto: Boolean = false,
        val photoUrl: String = "",
        val isPinned: Boolean = false,
        val isReviewed: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
        val addedByAdmin: Boolean = true
    ) {
        fun toEntity(): ArticleEntity = ArticleEntity(
            title = title,
            content = content,
            date = date,
            reactions = reactions,
            medicines = medicines,
            categories = categories,
            hasPhoto = hasPhoto,
            syncedFromFirebase = true,
            firebaseDocId = firebaseDocId,
            isPinned = isPinned,
            isReviewed = isReviewed,
            searchKeywords = "$title $content $medicines $categories".lowercase(),
            createdAt = createdAt
        )
    }

    data class PostMetaOverride(
        val postKey: String = "",
        val title: String = "",
        val content: String = "",
        val medicines: String = "",
        val categories: String = "",
        val hasContentOverride: Boolean = false,
        val isPinned: Boolean? = null,
        val isDeleted: Boolean = false,
        val isReviewed: Boolean = true,
        val reactions: Int? = null,
        val updatedAt: Long = 0L
    )
}
