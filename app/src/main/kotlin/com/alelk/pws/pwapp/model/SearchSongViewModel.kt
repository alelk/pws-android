package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.SongSearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class SearchSongViewModel(application: Application) : AndroidViewModel(application) {
  private val db = DatabaseProvider.getDatabase(application)
  private val songDao = db.songDao()

  private val _searchQuery = MutableStateFlow<String?>(null)
  val searchQuery: StateFlow<String?> get() = _searchQuery.asStateFlow()

  private val _searchResults = MutableStateFlow<List<SongSearchResult>>(emptyList())
  val searchResults: StateFlow<List<SongSearchResult>> get() = _searchResults.asStateFlow()

  fun setSearchQuery(query: String?) {
    if (_searchQuery.value != query) {
      _searchQuery.value = query
      Timber.d("Search query changed: $query")
      performSearch(query)
    }
  }

  private fun performSearch(query: String?) {
    viewModelScope.launch {
      _searchQuery.filterNotNull()
        .mapLatest {
          val songNumber = query?.toIntOrNull()
          when {
            songNumber != null -> songDao.findBySongNumber(songNumber, 50)

            !query.isNullOrBlank() -> {
              val searchText = query.trim().replace(Regex("\\s+"), "* NEAR/6 ") + "*"
              songDao.findBySongText(searchText, 50)
            }

            else -> emptyList()
          }
        }
        .catch { e ->
          Timber.e(e, "Error during search: ${e.message}")
        }
        .collectLatest { results ->
          _searchResults.value = results
          Timber.d("Search results updated: ${results.size} items found.")
        }
    }
  }
}