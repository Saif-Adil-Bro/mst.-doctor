package com.health.knowledge.data.local.dao

import androidx.room.*
import com.health.knowledge.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    // ── SEARCH ──────────────────────────────────────────────────────────────
    @Query("""
        SELECT * FROM articles 
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR medicines LIKE '%' || :query || '%'
           OR categories LIKE '%' || :query || '%'
        ORDER BY reactions DESC, createdAt DESC
    """)
    fun searchArticles(query: String): Flow<List<ArticleEntity>>

    // ── GET ALL ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles ORDER BY reactions DESC, createdAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    // ── GET BY CATEGORY ──────────────────────────────────────────────────────
    @Query("""
        SELECT * FROM articles 
        WHERE categories LIKE '%' || :category || '%'
        ORDER BY reactions DESC
    """)
    fun getByCategory(category: String): Flow<List<ArticleEntity>>

    // ── GET SINGLE ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: Int): ArticleEntity?

    // ── BOOKMARKS ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarked(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, bookmarked: Boolean)

    // ── INSERT (used for Firebase sync) ──────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity)

    // ── CHECK IF FIREBASE DOC ALREADY SYNCED ─────────────────────────────────
    @Query("SELECT COUNT(*) FROM articles WHERE firebaseDocId = :docId")
    suspend fun countByFirebaseDocId(docId: String): Int

    // ── STATS ────────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE syncedFromFirebase = 1")
    suspend fun getFirebaseSyncedCount(): Int

    // ── DELETE ───────────────────────────────────────────────────────────────
    @Query("DELETE FROM articles WHERE firebaseDocId = :docId")
    suspend fun deleteByFirebaseDocId(docId: String)
}
