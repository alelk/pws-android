package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.dao.SongNumberDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Deprecated("use SongViewModel")
@HiltViewModel
class SongsViewModel @Inject constructor(database: PwsDatabase) : ViewModel() {
  private val songNumberDao: SongNumberDao = database.songNumberDao()
  fun getSongNumber(songNumberId: Long): Flow<SongNumberEntity> = songNumberDao.getById(songNumberId)
}