package com.alelk.pws.pwapp.model

import androidx.lifecycle.ViewModel
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.dao.HistoryDao
import com.alelk.pws.database.dao.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor (database: PwsDatabase) : ViewModel() {

  private val historyDao: HistoryDao = database.historyDao()

  val historyItems: Flow<List<HistoryItem>> = historyDao.getAll()

  suspend fun clearHistory() {
    historyDao.deleteAll()
  }
}