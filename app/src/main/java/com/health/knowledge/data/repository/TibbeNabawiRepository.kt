package com.health.knowledge.data.repository

import com.health.knowledge.data.local.dao.TibbeNabawiDao
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TibbeNabawiRepository @Inject constructor(
    private val tibbeNabawiDao: TibbeNabawiDao
) {

    fun getAll(): Flow<List<TibbeNabawiEntity>> = tibbeNabawiDao.getAll()

    fun getByCategory(category: String): Flow<List<TibbeNabawiEntity>> {
        return if (category.isBlank() || category == "সকল") {
            tibbeNabawiDao.getAll()
        } else {
            tibbeNabawiDao.getByCategory(category)
        }
    }

    fun search(query: String, category: String? = null): Flow<List<TibbeNabawiEntity>> {
        val cleanQuery = query.trim().lowercase()
        return if (category.isNullOrBlank() || category == "সকল") {
            if (cleanQuery.isBlank()) tibbeNabawiDao.getAll()
            else tibbeNabawiDao.search(cleanQuery)
        } else {
            if (cleanQuery.isBlank()) tibbeNabawiDao.getByCategory(category)
            else tibbeNabawiDao.searchWithCategory(cleanQuery, category)
        }
    }

    fun getById(id: Int): Flow<TibbeNabawiEntity?> = tibbeNabawiDao.getById(id)

    suspend fun getByIdSync(id: Int): TibbeNabawiEntity? = tibbeNabawiDao.getByIdSync(id)

    fun getBookmarked(): Flow<List<TibbeNabawiEntity>> = tibbeNabawiDao.getBookmarked()

    fun getCategories(): Flow<List<String>> = tibbeNabawiDao.getCategories()

    suspend fun toggleBookmark(id: Int, currentState: Boolean) {
        tibbeNabawiDao.setBookmark(id, !currentState)
    }

    suspend fun searchGlobal(query: String, limit: Int = 5): List<TibbeNabawiEntity> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return emptyList()
        return tibbeNabawiDao.searchGlobal(cleanQuery, limit)
    }
}
