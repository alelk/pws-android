package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.SongInfo
import com.alelk.pws.database.entity.TagEntity
import com.alelk.pws.database.model.TagId
import kotlinx.coroutines.flow.Flow

class TagViewModel(application: Application) : AndroidViewModel(application) {
  private val tagDao = DatabaseProvider.getDatabase(application).tagDao()

  val allTags: LiveData<List<TagEntity>> = tagDao.getAll().asLiveData()

  suspend fun addTag(tag: TagEntity) = tagDao.insert(tag)
  suspend fun updateTag(tag: TagEntity) = tagDao.update(tag)
  suspend fun findByName(name: String) = tagDao.getAllByName(name)
  suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)

  suspend fun getLastCustomTag() =
    tagDao.getAllNotPredefined()
      .filter { it.isCustomTag() }
      .sortedByDescending { it.id.customTagNumber() }
      .lastOrNull()

  suspend fun getNextCustomTagId(): TagId {
    val lastCustomTagIdNumber = getLastCustomTag()?.id?.customTagNumber()
    return TagId.createCustomTag(lastCustomTagIdNumber?.plus(1) ?: 1)
  }

  fun getTagSongs(tagId: TagId): Flow<List<SongInfo>> = tagDao.getTagSongs(tagId)
}

private const val customTagPrefix = "custom-"

fun TagId.customTagNumber(): Int? = this.toString().takeIf { it.startsWith(customTagPrefix) }?.removePrefix(customTagPrefix)?.dropWhile { it == '0' }?.toInt()
fun TagId.Companion.createCustomTag(number: Int) = parse("$customTagPrefix${number.toString().padStart(5, '0')}")
fun TagEntity.isCustomTag(): Boolean = this.id.customTagNumber() != null