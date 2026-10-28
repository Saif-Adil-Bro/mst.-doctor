package com.health.knowledge.data.remote

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
     * Fetches global overrides (pins, edits, deletes, global reaction counts, cover photos).
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
     * Fetches live global reaction count & IP reaction status for a single post.
     */
    suspend fun fetchLiveReactionStatus(postKey: String, ipHash: String): Pair<Int?, Boolean?> {
        return try {
            val metaDoc = metaCollection.document(postKey).get().await()
            val reactorDoc = metaCollection.document(postKey)
                .collection("reactors")
                .document(ipHash)
                .get()
                .await()

            val globalCount = metaDoc.getLong("reactions")?.toInt()
            val hasIpReacted = if (metaDoc.exists()) reactorDoc.exists() else null
            Pair(globalCount, hasIpReacted)
        } catch (e: Exception) {
            Pair(null, null)
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
     * Admin: Edit / Review an article globally (including CDN cover photo URL).
     */
    suspend fun updateArticleGlobal(
        postKey: String,
        firebaseDocId: String,
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        isReviewed: Boolean
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val hasPhoto = photoUrl.isNotBlank()
            val data = mapOf(
                "postKey" to postKey,
                "title" to title,
                "content" to content,
                "medicines" to medicines,
                "categories" to categories,
                "photoUrl" to photoUrl,
                "hasPhoto" to hasPhoto,
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
                        "photoUrl" to photoUrl,
                        "hasPhoto" to hasPhoto,
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
     * Atomic Global Per-IP Reaction using Firestore Transaction:
     * - Checks if `postMeta/{postKey}/reactors/{ipHash}` exists in Firestore
     * - Atomically increments or decrements the global `reactions` counter
     * - Returns Pair(globalReactionCount, isReactedByThisIp)
     */
    suspend fun toggleReactionGlobalTransaction(
        postKey: String,
        firebaseDocId: String,
        ipHash: String,
        wantReact: Boolean,
        fallbackBaseCount: Int
    ): Result<Pair<Int, Boolean>> {
        return try {
            val metaRef = metaCollection.document(postKey)
            val reactorRef = metaRef.collection("reactors").document(ipHash)

            val resultPair = db.runTransaction { transaction ->
                val metaSnap = transaction.get(metaRef)
                val reactorSnap = transaction.get(reactorRef)

                val currentGlobalCount = metaSnap.getLong("reactions")?.toInt() ?: fallbackBaseCount
                val alreadyReactedInCloud = reactorSnap.exists()

                val updatedCount = when {
                    wantReact && !alreadyReactedInCloud -> {
                        transaction.set(
                            reactorRef,
                            mapOf(
                                "ipHash" to ipHash,
                                "reactedAt" to System.currentTimeMillis()
                            )
                        )
                        currentGlobalCount + 1
                    }
                    !wantReact && alreadyReactedInCloud -> {
                        transaction.delete(reactorRef)
                        (currentGlobalCount - 1).coerceAtLeast(0)
                    }
                    else -> currentGlobalCount
                }

                transaction.set(
                    metaRef,
                    mapOf(
                        "postKey" to postKey,
                        "reactions" to updatedCount,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )

                Pair(updatedCount, wantReact)
            }.await()

            if (firebaseDocId.isNotBlank()) {
                collection.document(firebaseDocId).set(
                    mapOf("reactions" to resultPair.first),
                    SetOptions.merge()
                ).await()
            }

            Result.success(resultPair)
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
        val addedByAdmin: Boolean = true,
        val authorDeviceId: String = ""
    ) {
        fun toEntity(myDeviceId: String = "", myDocIds: Set<String> = emptySet()): ArticleEntity {
            val isSubmittedByMe = (authorDeviceId.isNotBlank() && authorDeviceId == myDeviceId) ||
                    (firebaseDocId.isNotBlank() && firebaseDocId in myDocIds)
            return ArticleEntity(
                title = title,
                content = content,
                date = date,
                reactions = reactions,
                medicines = medicines,
                categories = categories,
                hasPhoto = hasPhoto || photoUrl.isNotBlank(),
                photoPath = photoUrl,
                syncedFromFirebase = true,
                firebaseDocId = firebaseDocId,
                isPinned = isPinned,
                isReviewed = isReviewed,
                searchKeywords = "$title $content $medicines $categories".lowercase(),
                createdAt = createdAt,
                isMine = isSubmittedByMe,
                authorDeviceId = authorDeviceId
            )
        }
    }

    data class PostMetaOverride(
        val postKey: String = "",
        val title: String = "",
        val content: String = "",
        val medicines: String = "",
        val categories: String = "",
        val photoUrl: String = "",
        val hasPhoto: Boolean = false,
        val hasContentOverride: Boolean = false,
        val isPinned: Boolean? = null,
        val isDeleted: Boolean = false,
        val isReviewed: Boolean = true,
        val reactions: Int? = null,
        val updatedAt: Long = 0L
    )
}
