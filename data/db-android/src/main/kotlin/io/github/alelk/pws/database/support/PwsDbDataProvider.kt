package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.core.SongNumber

internal interface PwsDbDataProvider {
  /** PWS database versions supported by this provider. */
  val dbVersions: IntRange

  /** Get books with all songs and song numbers — used to seed an empty target DB before user data
   *  migration so that favorites / history / tags can be linked by (bookId, songNumber). */
  suspend fun getBooks(): Result<List<BookMigrationData>>

  /** Get songs favorites. */
  suspend fun getFavorites(): Result<List<SongNumber>>

  /** Get songs history. */
  suspend fun getHistory(): Result<List<HistoryItem>>

  /** Get edited songs. */
  suspend fun getEditedSongs(): Result<List<SongChange>>

  /** Get song tags. */
  suspend fun getTags(): Result<List<Tag>>
}