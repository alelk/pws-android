package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.dao.HistoryDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {

  private val historyDao: HistoryDao = database.historyDao()

  val historyItems = historyDao.getAllFlow().distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  suspend fun clearHistory() {
    historyDao.deleteAll()
  }
}