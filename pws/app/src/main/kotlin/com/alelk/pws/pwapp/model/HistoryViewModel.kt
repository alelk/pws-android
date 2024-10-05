package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.HistoryDao
import com.alelk.pws.database.dao.HistoryItem
import kotlinx.coroutines.flow.Flow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

  private val historyDao: HistoryDao = DatabaseProvider.getDatabase(application).historyDao()

  val historyItems: Flow<List<HistoryItem>> = historyDao.getAll()

  suspend fun clearHistory() {
    historyDao.deleteAll()
  }
}