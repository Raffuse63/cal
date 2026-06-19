package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun insert(item: HistoryItem) = historyDao.insertHistoryItem(item)

    suspend fun clear() = historyDao.clearAllHistory()
}
