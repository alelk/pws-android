package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookWithFavorite
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.domain.model.TagId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val tagDao = database.tagDao()

  val allTags: StateFlow<List<TagEntity>?> =
    tagDao.getAllFlow()
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  suspend fun addTag(tag: TagEntity) = tagDao.insert(tag)
  suspend fun updateTag(tag: TagEntity) = tagDao.update(tag)
  suspend fun findByName(name: String) = tagDao.getAllByName(name)
  suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)

  suspend fun getLastCustomTag() = tagDao.getLastCustomTag()

  suspend fun getNextCustomTagId(): TagId = tagDao.getNextCustomTagId()

  fun getTagSongs(tagId: TagId): Flow<List<SongNumberWithSongWithBookWithFavorite>> = tagDao.getTagSongsFlow(tagId)
}