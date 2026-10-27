package com.health.knowledge.data.remote

import com.google.firebase.firestore.FirebaseFirestore
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

    /**
     * Fetches all articles from Firestore.
     * Called by SyncWorker when internet is available.
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
     * Used for incremental sync — faster & cheaper.
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
     * Admin only: Upload a new article to Firestore.
     * All devices will sync this on next background job.
     */
    suspend fun addArticle(article: FirestoreArticle): Result<String> {
        return try {
            val docRef = collection.add(article).await()
            Result.success(docRef.id)
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
            searchKeywords = "$title $content $medicines $categories".lowercase(),
            createdAt = createdAt
        )
    }
}
