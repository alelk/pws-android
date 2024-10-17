package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.SongNumberDao
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow

@Deprecated("use SongViewModel")
class SongsViewModel(application: Application) : AndroidViewModel(application) {
  private val songNumberDao: SongNumberDao = DatabaseProvider.getDatabase(application).songNumberDao()
  fun getSongNumber(songNumberId: Long): Flow<SongNumberEntity> = songNumberDao.getById(songNumberId)
}