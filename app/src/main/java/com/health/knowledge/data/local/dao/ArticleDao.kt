package com.health.knowledge.data.local.dao

import androidx.room.*
import com.health.knowledge.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    // ── SEARCH (Pinned first, excludes deleted & drafts) ─────────────────────
    @Query("""
        SELECT * FROM articles 
        WHERE isDeleted = 0 AND isDraft = 0
          AND (title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR medicines LIKE '%' || :query || '%'
           OR categories LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, reactions DESC, createdAt DESC
    """)
    fun searchArticles(query: String): Flow<List<ArticleEntity>>

    // ── ADMIN SEARCH (includes drafts) ───────────────────────────────────────
    @Query("""
        SELECT * FROM articles 
        WHERE isDeleted = 0
          AND (title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR medicines LIKE '%' || :query || '%'
           OR categories LIKE '%' || :query || '%')
        ORDER BY isDraft DESC, isPinned DESC, reactions DESC, createdAt DESC
    """)
    fun searchArticlesAdmin(query: String): Flow<List<ArticleEntity>>

    // ── GET ALL (Pinned first, excludes deleted & drafts) ────────────────────
    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 0 ORDER BY isPinned DESC, reactions DESC, createdAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isDeleted = 0 ORDER BY isDraft DESC, isPinned DESC, reactions DESC, createdAt DESC")
    fun getAllArticlesAdmin(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 0")
    suspend fun getAllActiveSnapshot(): List<ArticleEntity>

    // ── GET BY CATEGORY ──────────────────────────────────────────────────────
    @Query("""
        SELECT * FROM articles 
        WHERE isDeleted = 0 AND isDraft = 0
          AND categories LIKE '%' || :category || '%'
        ORDER BY isPinned DESC, reactions DESC, createdAt DESC
    """)
    fun getByCategory(category: String): Flow<List<ArticleEntity>>

    // ── GET SINGLE ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Int): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id AND isDeleted = 0")
    fun observeById(id: Int): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE messageId = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE firebaseDocId = :docId LIMIT 1")
    suspend fun getByFirebaseDocId(docId: String): ArticleEntity?

    // ── BOOKMARKS ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 0 AND isBookmarked = 1 ORDER BY isPinned DESC, createdAt DESC")
    fun getBookmarked(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, bookmarked: Boolean)

    // ── READING HISTORY & PERSONAL NOTES ─────────────────────────────────────
    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 0 AND lastReadAt > 0 ORDER BY lastReadAt DESC LIMIT 100")
    fun getReadingHistory(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET lastReadAt = :timestamp WHERE id = :id")
    suspend fun updateLastReadAt(id: Int, timestamp: Long)

    @Query("UPDATE articles SET lastReadAt = 0 WHERE lastReadAt > 0")
    suspend fun clearReadingHistory()

    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 0 AND TRIM(userNote) != '' ORDER BY lastReadAt DESC, createdAt DESC")
    fun getArticlesWithNotes(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET userNote = :note WHERE id = :id")
    suspend fun updateUserNote(id: Int, note: String)

    // ── DRAFTS ───────────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles WHERE isDeleted = 0 AND isDraft = 1 ORDER BY createdAt DESC")
    fun getDrafts(): Flow<List<ArticleEntity>>

    @Query("""
        UPDATE articles 
        SET isDraft = 0,
            syncedFromFirebase = :syncedFromFirebase,
            firebaseDocId = :firebaseDocId,
            date = :date,
            createdAt = :createdAt
        WHERE id = :id
    """)
    suspend fun markDraftPublished(
        id: Int,
        syncedFromFirebase: Boolean,
        firebaseDocId: String,
        date: String,
        createdAt: Long
    )

    // ── PIN / UNPIN ──────────────────────────────────────────────────────────
    @Query("UPDATE articles SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePin(id: Int, pinned: Boolean)

    // ── REACTIONS ────────────────────────────────────────────────────────────
    @Query("UPDATE articles SET reactions = :reactions, isReacted = :isReacted WHERE id = :id")
    suspend fun updateReaction(id: Int, reactions: Int, isReacted: Boolean)

    @Query("UPDATE articles SET reactions = :reactions WHERE id = :id")
    suspend fun updateReactionsCountOnly(id: Int, reactions: Int)

    // ── EDIT / REVIEW (includes CDN photoUrl) ────────────────────────────────
    @Query("""
        UPDATE articles 
        SET title = :title,
            content = :content,
            medicines = :medicines,
            categories = :categories,
            photoPath = :photoPath,
            hasPhoto = :hasPhoto,
            searchKeywords = :searchKeywords,
            isReviewed = :isReviewed
        WHERE id = :id
    """)
    suspend fun updateArticleContent(
        id: Int,
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoPath: String,
        hasPhoto: Boolean,
        searchKeywords: String,
        isReviewed: Boolean
    )

    // ── INSERT (used for seeding & Firebase sync) ────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity): Long

    // ── CHECK IF FIREBASE DOC ALREADY SYNCED ─────────────────────────────────
    @Query("SELECT COUNT(*) FROM articles WHERE firebaseDocId = :docId")
    suspend fun countByFirebaseDocId(docId: String): Int

    // ── STATS ────────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE isDeleted = 0 AND isDraft = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE syncedFromFirebase = 1 AND isDeleted = 0 AND isDraft = 0")
    suspend fun getFirebaseSyncedCount(): Int

    // ── SOFT DELETE (persists deletion of bundled & remote posts) ────────────
    @Query("UPDATE articles SET isDeleted = 1, isPinned = 0 WHERE id = :id")
    suspend fun markDeleted(id: Int)

    @Query("UPDATE articles SET isDeleted = 1, isPinned = 0 WHERE firebaseDocId = :docId")
    suspend fun deleteByFirebaseDocId(docId: String)
}
