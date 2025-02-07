package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.dao.HistoryDao
import io.github.alelk.pws.database.dao.HistoryItem
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