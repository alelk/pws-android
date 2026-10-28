package io.github.alelk.pws.android.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.data.repository.room.bookstatistic.BookStatisticRepositoryImpl
import io.github.alelk.pws.data.repository.room.favorite.FavoriteRepositoryImpl
import io.github.alelk.pws.data.repository.room.history.HistoryRepositoryImpl
import io.github.alelk.pws.data.repository.room.song.SongRepositoryImpl
import io.github.alelk.pws.data.repository.room.songnumber.SongNumberRepositoryImpl
import io.github.alelk.pws.data.repository.room.songtag.SongTagRepositoryImpl
import io.github.alelk.pws.data.repository.room.tag.TagRepositoryImpl
import io.github.alelk.pws.data.repository.room.transaction.RoomTransactionRunner
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.portable.backup.ExportBackupUseCase
import io.github.alelk.pws.portable.backup.RestoreBackupUseCase

/**
 * [BackupManager] over the production repositories of [db] and the settings [dataStore], wired as the app's Koin
 * modules wire it (repoRoomModule + databaseModule).
 */
fun BackupManager(db: PwsDatabase, dataStore: DataStore<Preferences>): BackupManager {
  val tx = RoomTransactionRunner(db)
  val settings = DataStoreBackupSettings(dataStore)
  val songs = SongRepositoryImpl(db.songDao(), db.songNumberDao())
  val tags = TagRepositoryImpl(db.tagDao(), db.songTagDao())
  val songTags = SongTagRepositoryImpl(db.songTagDao(), db.tagDao(), db.songDao(), db.songNumberDao())
  val statistics = BookStatisticRepositoryImpl(db.bookStatisticDao())
  val history = HistoryRepositoryImpl(db.historyDao(), db.songNumberDao())
  val favorites = FavoriteRepositoryImpl(db.favoriteDao())
  return BackupManager(
    export = ExportBackupUseCase(favorites, songs, tags, songTags, statistics, history, settings, tx),
    restore = RestoreBackupUseCase(
      SongNumberRepositoryImpl(db.songNumberDao()),
      songs,
      favorites,
      tags,
      tags,
      songTags,
      songTags,
      statistics,
      history,
      settings,
      tx,
    ),
  )
}
