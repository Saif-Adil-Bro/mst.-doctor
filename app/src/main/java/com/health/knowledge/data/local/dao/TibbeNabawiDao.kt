package com.health.knowledge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TibbeNabawiDao {

    @Query("SELECT * FROM tibbe_nabawi_posts ORDER BY id ASC")
    fun getAll(): Flow<List<TibbeNabawiEntity>>

    @Query("SELECT * FROM tibbe_nabawi_posts WHERE category = :category ORDER BY id ASC")
    fun getByCategory(category: String): Flow<List<TibbeNabawiEntity>>

    @Query("""
        SELECT * FROM tibbe_nabawi_posts 
        WHERE search_keywords LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%'
           OR food_name LIKE '%' || :query || '%'
           OR arabic_name LIKE '%' || :query || '%'
           OR diseases LIKE '%' || :query || '%'
        ORDER BY id ASC
    """)
    fun search(query: String): Flow<List<TibbeNabawiEntity>>

    @Query("""
        SELECT * FROM tibbe_nabawi_posts 
        WHERE category = :category AND (
            search_keywords LIKE '%' || :query || '%' 
            OR title LIKE '%' || :query || '%'
            OR food_name LIKE '%' || :query || '%'
            OR arabic_name LIKE '%' || :query || '%'
            OR diseases LIKE '%' || :query || '%'
        )
        ORDER BY id ASC
    """)
    fun searchWithCategory(query: String, category: String): Flow<List<TibbeNabawiEntity>>

    @Query("SELECT * FROM tibbe_nabawi_posts WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<TibbeNabawiEntity?>

    @Query("SELECT * FROM tibbe_nabawi_posts WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Int): TibbeNabawiEntity?

    @Query("SELECT * FROM tibbe_nabawi_posts WHERE is_bookmarked = 1 ORDER BY id ASC")
    fun getBookmarked(): Flow<List<TibbeNabawiEntity>>

    @Query("SELECT COUNT(*) FROM tibbe_nabawi_posts")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tibbe_nabawi_posts")
    suspend fun getCount(): Int

    @Query("SELECT DISTINCT category FROM tibbe_nabawi_posts ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("UPDATE tibbe_nabawi_posts SET is_bookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: Int, isBookmarked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<TibbeNabawiEntity>)

    @Update
    suspend fun update(post: TibbeNabawiEntity)

    @Query("""
        SELECT * FROM tibbe_nabawi_posts 
        WHERE search_keywords LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%'
           OR food_name LIKE '%' || :query || '%'
           OR diseases LIKE '%' || :query || '%'
        ORDER BY id ASC LIMIT :limit
    """)
    suspend fun searchGlobal(query: String, limit: Int = 10): List<TibbeNabawiEntity>
}
