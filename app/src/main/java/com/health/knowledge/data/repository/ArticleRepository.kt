package com.health.knowledge.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.entity.ArticleEntity
import com.health.knowledge.data.remote.FirestoreDataSource
import com.health.knowledge.domain.model.Article
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleDao: ArticleDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val prefsManager: UserPreferencesManager
) {
    private val syncMutex = Mutex()

    // ── Read from LOCAL Room DB (always fast, always offline) ────────────────
    fun searchArticles(query: String): Flow<List<ArticleEntity>> {
        val myDeviceId = prefsManager.getDeviceAuthorId()
        return if (query.isBlank()) {
            articleDao.getAllArticles(myDeviceId)
        } else {
            articleDao.searchArticles(query.trim(), myDeviceId)
        }
    }

    fun searchArticlesAdmin(query: String): Flow<List<ArticleEntity>> {
        return if (query.isBlank()) {
            articleDao.getAllArticlesAdmin()
        } else {
            articleDao.searchArticlesAdmin(query.trim())
        }
    }

    fun getAllArticles(): Flow<List<ArticleEntity>> =
        articleDao.getAllArticles(prefsManager.getDeviceAuthorId())

    fun getMyArticles(): Flow<List<ArticleEntity>> =
        articleDao.getMyArticles(prefsManager.getDeviceAuthorId())

    fun getByCategory(category: String): Flow<List<ArticleEntity>> =
        articleDao.getByCategory(category, prefsManager.getDeviceAuthorId())

    fun getBookmarked(): Flow<List<ArticleEntity>> = articleDao.getBookmarked()

    fun getReadingHistory(): Flow<List<ArticleEntity>> = articleDao.getReadingHistory()

    fun getArticlesWithNotes(): Flow<List<ArticleEntity>> = articleDao.getArticlesWithNotes()

    fun getDrafts(): Flow<List<ArticleEntity>> = articleDao.getDrafts()

    fun observeActiveCount(): Flow<Int> = articleDao.observeActiveCount()

    suspend fun getById(id: Int): ArticleEntity? = articleDao.getById(id)

    fun observeById(id: Int): Flow<ArticleEntity?> = articleDao.observeById(id)

    suspend fun updateBookmark(id: Int, bookmarked: Boolean) =
        articleDao.updateBookmark(id, bookmarked)

    suspend fun markArticleRead(id: Int) =
        articleDao.updateLastReadAt(id, System.currentTimeMillis())

    suspend fun clearReadingHistory() =
        articleDao.clearReadingHistory()

    suspend fun saveUserNote(id: Int, note: String) =
        articleDao.updateUserNote(id, note.trim())

    suspend fun getTotalCount(): Int = articleDao.getTotalCount()

    suspend fun getFirebaseSyncedCount(): Int = articleDao.getFirebaseSyncedCount()

    // ── Encrypted & Firestore-Synced Admin Auth + Reset System ───────────────
    suspend fun verifyAndUnlockAdminGlobal(rawPassword: String): Boolean = withContext(Dispatchers.IO) {
        val remoteConfig = withTimeoutOrNull(3500L) {
            firestoreDataSource.fetchAdminAuthConfig()
        }
        if (remoteConfig != null && remoteConfig.recoveryKeyHash.isNotBlank()) {
            prefsManager.saveEncryptedAdminCredentials(
                passwordHash = remoteConfig.passwordHash.ifBlank { prefsManager.getAdminPasswordHash() },
                recoveryKeyHash = remoteConfig.recoveryKeyHash
            )
        }
        val unlocked = prefsManager.verifyAndUnlockAdmin(
            inputPassword = rawPassword,
            remotePasswordHash = remoteConfig?.passwordHash
        )
        if (unlocked && (remoteConfig == null || remoteConfig.passwordHash.isBlank())) {
            withTimeoutOrNull(3000L) {
                firestoreDataSource.saveAdminAuthConfig(
                    passwordHash = prefsManager.getAdminPasswordHash(),
                    recoveryKeyHash = prefsManager.getAdminRecoveryHash()
                )
            }
        }
        unlocked
    }

    suspend fun resetAdminPasswordWithRecoveryGlobal(
        recoveryCode: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (newPassword.trim().length < 4) {
            return@withContext Result.failure(IllegalArgumentException("নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে"))
        }
        val remoteConfig = withTimeoutOrNull(4000L) {
            firestoreDataSource.fetchAdminAuthConfig()
        }
        val isRecoveryValid = prefsManager.verifyRecoveryCode(
            inputRecoveryCode = recoveryCode,
            remoteRecoveryHash = remoteConfig?.recoveryKeyHash
        )
        if (!isRecoveryValid) {
            return@withContext Result.failure(IllegalArgumentException("রিকভারি কোড সঠিক নয়!"))
        }

        val encryptedNewPassword = prefsManager.encryptSecret(newPassword)
        val recoveryHashToKeep = remoteConfig?.recoveryKeyHash?.takeIf { it.isNotBlank() }
            ?: prefsManager.getAdminRecoveryHash()

        prefsManager.saveEncryptedAdminCredentials(
            passwordHash = encryptedNewPassword,
            recoveryKeyHash = recoveryHashToKeep
        )
        prefsManager.unlockAdminSession()

        withTimeoutOrNull(4500L) {
            firestoreDataSource.saveAdminAuthConfig(
                passwordHash = encryptedNewPassword,
                recoveryKeyHash = recoveryHashToKeep
            )
        }
        Result.success(Unit)
    }

    suspend fun changeAdminPasswordGlobal(
        newPassword: String,
        newRecoveryCode: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (newPassword.trim().length < 4) {
            return@withContext Result.failure(IllegalArgumentException("নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে"))
        }
        val newPassHash = prefsManager.encryptSecret(newPassword)
        val newRecHash = if (newRecoveryCode.trim().length >= 4) {
            prefsManager.encryptSecret(newRecoveryCode)
        } else {
            prefsManager.getAdminRecoveryHash()
        }

        prefsManager.saveEncryptedAdminCredentials(
            passwordHash = newPassHash,
            recoveryKeyHash = newRecHash
        )

        withTimeoutOrNull(4500L) {
            firestoreDataSource.saveAdminAuthConfig(
                passwordHash = newPassHash,
                recoveryKeyHash = newRecHash
            )
        }
        Result.success(Unit)
    }

    // ── Pull-to-Refresh Live Cloud Sync (Deduplicated & Mutex-Protected) ─────
    suspend fun syncFromCloudNow(): Int = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            var addedOrUpdated = 0
            try {
                articleDao.normalizeBundledTimestamps()
                articleDao.deduplicateCloudArticles()

                val remoteArticles = withTimeoutOrNull(6000L) {
                    firestoreDataSource.fetchAllArticles()
                } ?: emptyList()

                val myDeviceId = prefsManager.getDeviceAuthorId()
                val myDocIds = prefsManager.getMySubmittedDocIds()

                val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                val hadInitialCloudSync = syncPrefs.getBoolean("has_completed_initial_cloud_sync", false)

                if (remoteArticles.isNotEmpty()) {
                    val newEntities = mutableListOf<ArticleEntity>()
                    val seenDocIds = mutableSetOf<String>()

                    for (fbArt in remoteArticles) {
                        if (fbArt.firebaseDocId.isNotBlank() && !seenDocIds.add(fbArt.firebaseDocId)) {
                            continue
                        }
                        val isMine = (fbArt.authorDeviceId.isNotBlank() && fbArt.authorDeviceId == myDeviceId) ||
                                (fbArt.firebaseDocId.isNotBlank() && fbArt.firebaseDocId in myDocIds)

                        val existing = articleDao.findExistingCloudOrUserArticle(
                            docId = fbArt.firebaseDocId,
                            title = fbArt.title
                        )
                        if (existing != null) {
                            if (existing.firebaseDocId.isBlank() && fbArt.firebaseDocId.isNotBlank()) {
                                articleDao.updateFirebaseDocId(existing.id, fbArt.firebaseDocId)
                            }
                            if (isMine && !existing.isMine) {
                                articleDao.markAsMine(existing.id, myDeviceId)
                            }
                            if (existing.isReviewed != fbArt.isReviewed && fbArt.isReviewed) {
                                articleDao.approveArticle(existing.id)
                                HealthNotificationHelper.showNewPostNotification(
                                    context = context,
                                    articleId = existing.id,
                                    title = existing.title,
                                    content = existing.content,
                                    isApprovedMine = isMine || existing.isMine
                                )
                            }
                            if (fbArt.verifiedCount > existing.verifiedCount) {
                                articleDao.updateVerifiedCountOnly(existing.id, fbArt.verifiedCount)
                            }
                        } else {
                            newEntities.add(fbArt.toEntity(myDeviceId = myDeviceId, myDocIds = myDocIds))
                        }
                    }
                    if (newEntities.isNotEmpty()) {
                        articleDao.insertAll(newEntities)
                        addedOrUpdated += newEntities.size
                        if (hadInitialCloudSync) {
                            val firstApproved = newEntities.firstOrNull { it.isReviewed && !it.isDraft }
                            if (firstApproved != null) {
                                val inserted = articleDao.getByFirebaseDocId(firstApproved.firebaseDocId)
                                val targetId = inserted?.id ?: 0
                                HealthNotificationHelper.showNewPostNotification(
                                    context = context,
                                    articleId = targetId,
                                    title = firstApproved.title,
                                    content = firstApproved.content
                                )
                            }
                        }
                    }
                    syncPrefs.edit().putBoolean("has_completed_initial_cloud_sync", true).apply()
                }

                articleDao.deduplicateCloudArticles()

                val metaList = withTimeoutOrNull(6000L) {
                    firestoreDataSource.fetchAllPostMeta()
                } ?: emptyList()

                for (meta in metaList) {
                    val targetEntity = when {
                        meta.postKey.startsWith("fb_") -> {
                            val docId = meta.postKey.removePrefix("fb_")
                            articleDao.getByFirebaseDocId(docId)
                        }
                        meta.postKey.startsWith("bundled_") -> {
                            val keyPart = meta.postKey.removePrefix("bundled_")
                            articleDao.getByMessageId(keyPart)
                                ?: keyPart.toIntOrNull()?.let { articleDao.getById(it) }
                        }
                        else -> null
                    } ?: continue

                    if (meta.isDeleted) {
                        articleDao.markDeleted(targetEntity.id)
                        continue
                    }

                    meta.isPinned?.let { pinned ->
                        articleDao.updatePin(targetEntity.id, pinned)
                    }

                    if (meta.hasContentOverride && meta.title.isNotBlank() && meta.content.isNotBlank()) {
                        val resolvedSource = meta.source.ifBlank { targetEntity.source }
                        val keywords = "${meta.title} ${meta.content} ${meta.medicines} ${meta.categories} $resolvedSource".lowercase()
                        val resolvedPhoto = meta.photoUrl.ifBlank { targetEntity.photoPath }
                        articleDao.updateArticleContent(
                            id = targetEntity.id,
                            title = meta.title,
                            content = meta.content,
                            medicines = meta.medicines,
                            categories = meta.categories,
                            photoPath = resolvedPhoto,
                            hasPhoto = resolvedPhoto.isNotBlank(),
                            searchKeywords = keywords,
                            isReviewed = meta.isReviewed,
                            source = resolvedSource
                        )
                    }

                    meta.reactions?.let { remoteReactions ->
                        if (remoteReactions >= 0) {
                            articleDao.updateReactionsCountOnly(targetEntity.id, remoteReactions)
                        }
                    }

                    meta.verifiedCount?.let { remoteVerified ->
                        if (remoteVerified >= 0) {
                            articleDao.updateVerifiedCountOnly(targetEntity.id, remoteVerified)
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep existing local data intact on offline/timeout
            }
            addedOrUpdated
        }
    }

    // ── Per-IP Global Reaction (Optimistic Local + Atomic Firestore Sync) ────
    suspend fun toggleReaction(article: Article) {
        val wantReact = !article.isReacted
        val optimisticCount = if (wantReact) {
            article.reactions + 1
        } else {
            (article.reactions - 1).coerceAtLeast(0)
        }

        articleDao.updateReaction(
            id = article.id,
            reactions = optimisticCount,
            isReacted = wantReact
        )

        withTimeoutOrNull(5000L) {
            val ipHash = prefsManager.getClientIpHash()
            val txResult = firestoreDataSource.toggleReactionGlobalTransaction(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                ipHash = ipHash,
                wantReact = wantReact,
                fallbackBaseCount = article.reactions
            )
            txResult.getOrNull()?.let { (globalCount, reactedState) ->
                articleDao.updateReaction(
                    id = article.id,
                    reactions = globalCount,
                    isReacted = reactedState
                )
            }
        }
    }

    // ── Global User Verification ("পরীক্ষিত / কাজ হয়েছে") with Consent & Optional Details ──
    suspend fun submitVerificationWithDetails(
        article: Article,
        verifierName: String = "",
        verifierContact: String = "",
        verifierComment: String = ""
    ) {
        val optimisticCount = if (!article.isVerified) {
            article.verifiedCount + 1
        } else {
            article.verifiedCount.coerceAtLeast(1)
        }

        // 1. Optimistic local update for instant UI feedback
        articleDao.updateVerification(
            id = article.id,
            verifiedCount = optimisticCount,
            isVerified = true
        )

        // 2. Atomic Firestore Transaction to save attestation + optional info globally
        withTimeoutOrNull(5500L) {
            val ipHash = prefsManager.getClientIpHash()
            val txResult = firestoreDataSource.toggleVerificationGlobalTransaction(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                ipHash = ipHash,
                wantVerify = true,
                fallbackBaseCount = article.verifiedCount,
                verifierName = verifierName,
                verifierContact = verifierContact,
                verifierComment = verifierComment
            )
            txResult.getOrNull()?.let { (globalVerifiedCount, verifiedState) ->
                articleDao.updateVerification(
                    id = article.id,
                    verifiedCount = globalVerifiedCount,
                    isVerified = verifiedState
                )
            }
        }
    }

    suspend fun removeVerification(article: Article) {
        val optimisticCount = (article.verifiedCount - 1).coerceAtLeast(0)
        articleDao.updateVerification(
            id = article.id,
            verifiedCount = optimisticCount,
            isVerified = false
        )

        withTimeoutOrNull(5000L) {
            val ipHash = prefsManager.getClientIpHash()
            val txResult = firestoreDataSource.toggleVerificationGlobalTransaction(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                ipHash = ipHash,
                wantVerify = false,
                fallbackBaseCount = article.verifiedCount
            )
            txResult.getOrNull()?.let { (globalVerifiedCount, verifiedState) ->
                articleDao.updateVerification(
                    id = article.id,
                    verifiedCount = globalVerifiedCount,
                    isVerified = verifiedState
                )
            }
        }
    }

    suspend fun fetchVerifierEntries(postKey: String): List<FirestoreDataSource.VerifierEntry> {
        return withTimeoutOrNull(4500L) {
            firestoreDataSource.fetchVerifierEntries(postKey)
        } ?: emptyList()
    }

    /**
     * Refreshes the live global reaction & verification counts from Firestore when viewing a post.
     */
    suspend fun refreshLiveReactions(article: Article) {
        withTimeoutOrNull(4500L) {
            val ipHash = prefsManager.getClientIpHash()
            val (globalCount, hasIpReacted) = firestoreDataSource.fetchLiveReactionStatus(
                postKey = article.postKey,
                ipHash = ipHash
            )
            if (globalCount != null) {
                articleDao.updateReaction(
                    id = article.id,
                    reactions = globalCount,
                    isReacted = hasIpReacted ?: article.isReacted
                )
            }

            val (globalVerifiedCount, hasIpVerified) = firestoreDataSource.fetchLiveVerificationStatus(
                postKey = article.postKey,
                ipHash = ipHash
            )
            if (globalVerifiedCount != null) {
                articleDao.updateVerification(
                    id = article.id,
                    verifiedCount = globalVerifiedCount,
                    isVerified = hasIpVerified ?: article.isVerified
                )
            }
        }
    }

    // ── Admin: Add new article (saves locally + syncs to Firebase without duplicates) ──
    suspend fun addArticleToFirebase(
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        date: String,
        isPinned: Boolean = false,
        source: String = ""
    ): Result<String> = syncMutex.withLock {
        val now = System.currentTimeMillis()
        val cleanPhotoUrl = photoUrl.trim()
        val cleanSource = source.trim()
        val preGeneratedDocId = firestoreDataSource.generateNewDocId()

        val firestoreArticle = FirestoreDataSource.FirestoreArticle(
            firebaseDocId = preGeneratedDocId,
            title = title.trim(),
            content = content.trim(),
            date = date,
            medicines = medicines,
            categories = categories,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            photoUrl = cleanPhotoUrl,
            isPinned = isPinned,
            isReviewed = true,
            createdAt = now,
            source = cleanSource
        )

        // Insert locally first with the exact pre-generated firebaseDocId so sync never duplicates it
        val existing = articleDao.findExistingCloudOrUserArticle(preGeneratedDocId, title.trim())
        val localId = if (existing == null) {
            articleDao.insert(firestoreArticle.toEntity()).toInt()
        } else {
            existing.id
        }

        withTimeoutOrNull(5500L) {
            firestoreDataSource.addArticle(firestoreArticle)
        }

        articleDao.deduplicateCloudArticles()

        HealthNotificationHelper.showNewPostNotification(
            context = context,
            articleId = localId,
            title = title.trim(),
            content = content.trim()
        )
        Result.success(preGeneratedDocId)
    }

    // ── User: Submit Article for Admin Review (without duplicates) ───────────
    suspend fun submitUserArticle(
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        date: String,
        source: String = ""
    ): Result<String> = syncMutex.withLock {
        val now = System.currentTimeMillis()
        val cleanPhotoUrl = photoUrl.trim()
        val cleanSource = source.trim()
        val myDeviceId = prefsManager.getDeviceAuthorId()
        val preGeneratedDocId = firestoreDataSource.generateNewDocId()

        prefsManager.addMySubmittedDocId(preGeneratedDocId)
        prefsManager.addMySubmittedDocId("fb_$preGeneratedDocId")

        val firestoreArticle = FirestoreDataSource.FirestoreArticle(
            firebaseDocId = preGeneratedDocId,
            title = title.trim(),
            content = content.trim(),
            date = date,
            medicines = medicines,
            categories = categories,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            photoUrl = cleanPhotoUrl,
            isPinned = false,
            isReviewed = false, // Pending admin review
            addedByAdmin = false,
            createdAt = now,
            authorDeviceId = myDeviceId,
            source = cleanSource
        )

        val existing = articleDao.findExistingCloudOrUserArticle(preGeneratedDocId, title.trim())
        if (existing == null) {
            val entity = firestoreArticle
                .toEntity(myDeviceId = myDeviceId, myDocIds = prefsManager.getMySubmittedDocIds())
                .copy(isMine = true, authorDeviceId = myDeviceId, source = cleanSource)
            articleDao.insert(entity)
        }

        withTimeoutOrNull(6000L) {
            firestoreDataSource.addArticle(firestoreArticle)
        }

        articleDao.deduplicateCloudArticles()
        Result.success(preGeneratedDocId)
    }

    // ── Admin: Approve Pending Article (publishes globally & notifies users) ─
    suspend fun approveArticle(article: Article): Result<Unit> {
        articleDao.approveArticle(article.id)
        val cleanPhotoUrl = if (article.photoPath.startsWith("http")) article.photoPath.trim() else ""
        val medsCsv = article.medicines.joinToString(",")
        val catsCsv = article.categories.joinToString(",")

        withTimeoutOrNull(5000L) {
            firestoreDataSource.updateArticleGlobal(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                title = article.title,
                content = article.content,
                medicines = medsCsv,
                categories = catsCsv,
                photoUrl = cleanPhotoUrl,
                isReviewed = true,
                source = article.source
            )
        }

        HealthNotificationHelper.showNewPostNotification(
            context = context,
            articleId = article.id,
            title = article.title,
            content = article.content
        )

        return Result.success(Unit)
    }

    // ── Admin: Save Draft Locally (can be edited & published later) ──────────
    suspend fun saveDraftArticle(
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        date: String,
        isPinned: Boolean = false,
        source: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val cleanPhotoUrl = photoUrl.trim()
        val cleanSource = source.trim()
        val entity = ArticleEntity(
            title = title,
            content = content,
            date = date,
            medicines = medicines,
            categories = categories,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            photoPath = cleanPhotoUrl,
            isPinned = isPinned,
            isReviewed = true,
            isDraft = true,
            syncedFromFirebase = false,
            searchKeywords = "$title $content $medicines $categories $cleanSource".lowercase(),
            createdAt = now,
            source = cleanSource
        )
        return articleDao.insert(entity)
    }

    // ── Admin: Publish Draft to Firebase & Public Feed ───────────────────────
    suspend fun publishDraftArticle(article: Article): Result<String> = syncMutex.withLock {
        val now = System.currentTimeMillis()
        val today = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val cleanPhotoUrl = if (article.photoPath.startsWith("http")) article.photoPath.trim() else ""
        val medsCsv = article.medicines.joinToString(",")
        val catsCsv = article.categories.joinToString(",")
        val preGeneratedDocId = article.firebaseDocId.ifBlank { firestoreDataSource.generateNewDocId() }

        articleDao.markDraftPublished(
            id = article.id,
            syncedFromFirebase = true,
            firebaseDocId = preGeneratedDocId,
            date = today,
            createdAt = now
        )

        val firestoreArticle = FirestoreDataSource.FirestoreArticle(
            firebaseDocId = preGeneratedDocId,
            title = article.title,
            content = article.content,
            date = today,
            medicines = medsCsv,
            categories = catsCsv,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            photoUrl = cleanPhotoUrl,
            isPinned = article.isPinned,
            isReviewed = true,
            createdAt = now,
            source = article.source
        )

        withTimeoutOrNull(5000L) {
            firestoreDataSource.addArticle(firestoreArticle)
        }

        articleDao.deduplicateCloudArticles()

        HealthNotificationHelper.showNewPostNotification(
            context = context,
            articleId = article.id,
            title = article.title,
            content = article.content
        )
        Result.success(preGeneratedDocId)
    }

    // ── Admin: Pin / Unpin globally ──────────────────────────────────────────
    suspend fun togglePinArticle(article: Article) {
        val newPin = !article.isPinned
        articleDao.updatePin(article.id, newPin)

        withTimeoutOrNull(4000L) {
            firestoreDataSource.setPinnedGlobal(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                isPinned = newPin
            )
        }
    }

    // ── Admin: Edit / Review post globally (including CDN photoUrl & source) ─
    suspend fun updateArticleGlobally(
        article: Article,
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        isReviewed: Boolean = true,
        source: String = ""
    ) {
        val cleanPhotoUrl = photoUrl.trim()
        val cleanSource = source.trim()
        val keywords = "$title $content $medicines $categories $cleanSource".lowercase()
        articleDao.updateArticleContent(
            id = article.id,
            title = title,
            content = content,
            medicines = medicines,
            categories = categories,
            photoPath = cleanPhotoUrl,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            searchKeywords = keywords,
            isReviewed = isReviewed,
            source = cleanSource
        )

        if (!article.isDraft) {
            withTimeoutOrNull(4500L) {
                firestoreDataSource.updateArticleGlobal(
                    postKey = article.postKey,
                    firebaseDocId = article.firebaseDocId,
                    title = title,
                    content = content,
                    medicines = medicines,
                    categories = categories,
                    photoUrl = cleanPhotoUrl,
                    isReviewed = isReviewed,
                    source = cleanSource
                )
            }
        }
    }

    // ── Admin: Global Delete ─────────────────────────────────────────────────
    suspend fun deleteArticleGlobally(article: Article) {
        articleDao.markDeleted(article.id)

        if (!article.isDraft) {
            withTimeoutOrNull(4500L) {
                firestoreDataSource.deleteArticleGlobal(
                    postKey = article.postKey,
                    firebaseDocId = article.firebaseDocId
                )
            }
        }
    }

    // ── Backup & Restore (Bookmarks, Notes, History & Custom Posts) ──────────
    data class BackupEntry(
        val postKey: String = "",
        val title: String = "",
        val content: String = "",
        val date: String = "",
        val medicines: String = "",
        val categories: String = "",
        val photoPath: String = "",
        val isBookmarked: Boolean = false,
        val userNote: String = "",
        val lastReadAt: Long = 0L,
        val isPinned: Boolean = false,
        val syncedFromFirebase: Boolean = false,
        val source: String = ""
    )

    data class AppBackupPayload(
        val exportedAt: Long = System.currentTimeMillis(),
        val version: Int = 1,
        val entries: List<BackupEntry> = emptyList()
    )

    suspend fun exportBackupJson(includeAllArticles: Boolean = false): String = withContext(Dispatchers.IO) {
        val allActive = articleDao.getAllActiveSnapshot()
        val filtered = if (includeAllArticles) {
            allActive
        } else {
            allActive.filter { it.isBookmarked || it.userNote.isNotBlank() || it.lastReadAt > 0L || it.syncedFromFirebase }
        }
        val entries = filtered.map { entity ->
            BackupEntry(
                postKey = entity.postKey,
                title = entity.title,
                content = entity.content,
                date = entity.date,
                medicines = entity.medicines,
                categories = entity.categories,
                photoPath = entity.photoPath,
                isBookmarked = entity.isBookmarked,
                userNote = entity.userNote,
                lastReadAt = entity.lastReadAt,
                isPinned = entity.isPinned,
                syncedFromFirebase = entity.syncedFromFirebase,
                source = entity.source
            )
        }
        GsonBuilder().setPrettyPrinting().create().toJson(AppBackupPayload(entries = entries))
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cleanJson = jsonString.trim().removePrefix("\uFEFF")
            val gson = Gson()
            val entries: List<BackupEntry> = if (cleanJson.startsWith("{")) {
                val payload = gson.fromJson(cleanJson, AppBackupPayload::class.java)
                payload?.entries ?: emptyList()
            } else {
                val listType = object : TypeToken<List<BackupEntry>>() {}.type
                gson.fromJson(cleanJson, listType) ?: emptyList()
            }

            if (entries.isEmpty()) return@withContext Result.success(0)

            val currentArticles = articleDao.getAllActiveSnapshot()
            val byPostKey = currentArticles.associateBy { it.postKey }
            val byTitle = currentArticles.associateBy { it.title.trim() }

            var restoredCount = 0
            for (entry in entries) {
                val existing = byPostKey[entry.postKey] ?: byTitle[entry.title.trim()]
                if (existing != null) {
                    if (entry.isBookmarked && !existing.isBookmarked) {
                        articleDao.updateBookmark(existing.id, true)
                    }
                    if (entry.userNote.isNotBlank()) {
                        articleDao.updateUserNote(existing.id, entry.userNote)
                    }
                    if (entry.lastReadAt > existing.lastReadAt) {
                        articleDao.updateLastReadAt(existing.id, entry.lastReadAt)
                    }
                    restoredCount++
                } else if (entry.title.isNotBlank() && entry.content.isNotBlank()) {
                    val newEntity = ArticleEntity(
                        title = entry.title,
                        content = entry.content,
                        date = entry.date,
                        medicines = entry.medicines,
                        categories = entry.categories,
                        hasPhoto = entry.photoPath.isNotBlank(),
                        photoPath = entry.photoPath,
                        isBookmarked = entry.isBookmarked,
                        userNote = entry.userNote,
                        lastReadAt = entry.lastReadAt,
                        isPinned = entry.isPinned,
                        syncedFromFirebase = entry.syncedFromFirebase,
                        source = entry.source,
                        searchKeywords = "${entry.title} ${entry.content} ${entry.medicines} ${entry.categories} ${entry.source}".lowercase()
                    )
                    articleDao.insert(newEntity)
                    restoredCount++
                }
            }
            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
