package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.model.SongNumber

interface PwsDbDataProvider {
  /** PWS database versions supported by this provider. */
  val dbVersions: IntRange

  /** Get songs favorites. */
  suspend fun getFavorites(): Result<List<SongNumber>>

  /** Get songs history. */
  suspend fun getHistory(): Result<List<HistoryItem>>

  /** Get edited songs. */
  suspend fun getEditedSongs(): Result<List<SongChange>>
}