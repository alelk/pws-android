package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.SongDao
import com.alelk.pws.database.dao.SongNumberDao
import com.alelk.pws.database.dao.SongNumberWithSongWithBookWithFavorites
import com.alelk.pws.database.entity.SongEntity
import com.alelk.pws.database.entity.SongNumberEntity
import kotlinx.coroutines.flow.Flow

class SongsViewModel(application: Application) : AndroidViewModel(application) {
  private val songNumberDao: SongNumberDao = DatabaseProvider.getDatabase(application).songNumberDao()
  private val songDao: SongDao = DatabaseProvider.getDatabase(application).songDao()
  fun getSongOfBook(songNumberId: Long): Flow<SongNumberWithSongWithBookWithFavorites> = songNumberDao.getSongOfBookById(songNumberId)
  fun getSongNumber(songNumberId: Long): Flow<SongNumberEntity> = songNumberDao.getById(songNumberId)
  suspend fun updateSong(song: SongEntity) = songDao.update(song)
}