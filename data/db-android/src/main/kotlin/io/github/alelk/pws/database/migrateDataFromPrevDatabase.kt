package io.github.alelk.pws.database

import android.content.Context
import androidx.room.withTransaction
import io.github.alelk.pws.database.history.HistoryEntity
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.TagEntity
import io.github.alelk.pws.database.support.PwsDb1xDataProvider
import io.github.alelk.pws.database.support.PwsDb2xDataProvider
import io.github.alelk.pws.domain.core.SongNumber
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File

internal val DATABASE_PREV_NAMES =
  arrayOf(
    // encrypted (SQLCipher) — versioned filename used before the switch to stable `pws.db`
    "pws.3.2.3.db",
    // plain SQLite — was unencrypted before SQLCipher was introduced
    "pws.1.8.0.db", "pws.1.2.0.db", "pws.1.1.0.db", "pws.0.9.1.db", "pws.2.0.0.db", "pws.3.0.0.db",
    // plain SQLite, Room v11 — the RuStore fork `pws-android-rustore` 2.3.1 (same applicationId as
    // the `rustore` flavor, so an in-place update finds it next to the new `pws.db`)
    "pws.2.3.0.db",
  )

/** Sub-directory of the databases dir where fully migrated legacy files are moved instead of deleted. */
internal const val LEGACY_QUARANTINE_DIR = "legacy-migrated"

/**
 * After this many startup attempts that could not match every legacy record, the source file is
 * quarantined anyway — otherwise a record that can never match (e.g. a book no longer in the
 * catalog) would make the migration re-run on every launch forever.
 */
internal const val LEGACY_MAX_ATTEMPTS = 5

private const val PREFS_NAME = "pws_legacy_migration"
private fun attemptsKey(dbName: String) = "attempts:$dbName"

/** How many records of one kind the legacy database had, and how many landed in the current one. */
data class MigrationCount(val found: Int, val migrated: Int) {
  val isComplete: Boolean get() = migrated >= found

  companion object {
    val EMPTY = MigrationCount(0, 0)
  }
}

/** Per-kind outcome of one [migrateDataTo] run. Counts only — never content. */
data class MigrationReport(
  val favorites: MigrationCount = MigrationCount.EMPTY,
  val history: MigrationCount = MigrationCount.EMPTY,
  val editedSongs: MigrationCount = MigrationCount.EMPTY,
  val tags: MigrationCount = MigrationCount.EMPTY,
) {
  val found: Int get() = favorites.found + history.found + editedSongs.found + tags.found
  val migrated: Int get() = favorites.migrated + history.migrated + editedSongs.migrated + tags.migrated

  /** Every legacy record was matched in the current database. */
  val isComplete: Boolean
    get() = favorites.isComplete && history.isComplete && editedSongs.isComplete && tags.isComplete
}

/** What happened to one legacy database file during a migration run. */
data class LegacyMigrationOutcome(
  /** File name of the legacy database, e.g. `pws.2.3.0.db`. Not personal data. */
  val sourceName: String,
  val result: Result,
  /** Null when the run failed before a report could be produced. */
  val report: MigrationReport?,
  /** 1-based startup attempt number for this file (0 for retries that do not count as an attempt). */
  val attempt: Int,
  val durationMs: Long,
) {
  enum class Result(val id: String) {
    /** Everything matched; the file was moved to quarantine. */
    COMPLETE("complete"),

    /** Some records did not match; the file stays in place and the migration will be retried. */
    PARTIAL_RETRY("partial_retry"),

    /** Some records never matched within [LEGACY_MAX_ATTEMPTS]; quarantined anyway. */
    PARTIAL_GAVE_UP("partial_gave_up"),

    /** The run threw; the file stays in place and the migration will be retried. */
    FAILED_RETRY("failed_retry"),

    /** The run kept failing within [LEGACY_MAX_ATTEMPTS]; quarantined anyway. */
    FAILED_GAVE_UP("failed_gave_up"),
  }
}

/** True when at least one legacy database file is still waiting to be migrated. */
internal fun hasPendingLegacyDatabase(context: Context): Boolean =
  DATABASE_PREV_NAMES.any { context.getDatabasePath(it)?.isFile == true }

/**
 * Migrates user data from every legacy database file found next to the current one.
 *
 * A source file is never deleted. When all of its records have been matched it is moved to
 * `databases/legacy-migrated/` (quarantine), so the data stays recoverable and the file is no longer
 * picked up by [DATABASE_PREV_NAMES]. When some records could not be matched (their book is not
 * installed yet) the file stays in place and the migration re-runs on the next launch and after the
 * next book install; [countAttempt] runs (app start) are capped by [LEGACY_MAX_ATTEMPTS].
 */
internal suspend fun migrateDataFromPrevDatabase(
  context: Context,
  currentDatabase: PwsDatabase,
  passphrase: ByteArray,
  countAttempt: Boolean = true,
): List<LegacyMigrationOutcome> {
  val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  return DATABASE_PREV_NAMES.mapNotNull { dbName ->
    val dbFile = context.getDatabasePath(dbName)
    if (dbFile == null || !dbFile.isFile) {
      Timber.d("no previous database $dbFile found")
      return@mapNotNull null
    }
    val startedAt = System.currentTimeMillis()
    val attempt = if (countAttempt) prefs.getInt(attemptsKey(dbName), 0) + 1 else 0
    if (countAttempt) prefs.edit().putInt(attemptsKey(dbName), attempt).commit()

    val report: Result<MigrationReport> = runCatching {
      val prevDb = checkNotNull(openReadOnlyDatabase(dbFile, passphrase)) { "cannot open previous database $dbFile" }
      prevDb.use { it.migrateDataTo(currentDatabase).getOrThrow() }
    }.onFailure { e ->
      Timber.e(e, "error migrating data from previous database $dbName: ${e.message}")
    }

    val exhausted = attempt >= LEGACY_MAX_ATTEMPTS
    val result = when {
      report.getOrNull()?.isComplete == true -> LegacyMigrationOutcome.Result.COMPLETE
      report.isSuccess -> if (exhausted) LegacyMigrationOutcome.Result.PARTIAL_GAVE_UP else LegacyMigrationOutcome.Result.PARTIAL_RETRY
      else -> if (exhausted) LegacyMigrationOutcome.Result.FAILED_GAVE_UP else LegacyMigrationOutcome.Result.FAILED_RETRY
    }
    when (result) {
      LegacyMigrationOutcome.Result.PARTIAL_RETRY, LegacyMigrationOutcome.Result.FAILED_RETRY ->
        Timber.w("previous database $dbName kept for retry ($result, attempt $attempt): ${report.getOrNull()}")

      else -> {
        Timber.i("previous database $dbName migrated ($result): ${report.getOrNull()}, move to quarantine...")
        // A failed move leaves the file in place: the (idempotent) migration simply runs again.
        runCatching { quarantineLegacyDatabase(dbFile) }
          .onSuccess { prefs.edit().remove(attemptsKey(dbName)).commit() }
          .onFailure { e -> Timber.e(e, "failed to quarantine previous database $dbName: ${e.message}") }
      }
    }
    LegacyMigrationOutcome(
      sourceName = dbName,
      result = result,
      report = report.getOrNull(),
      attempt = attempt,
      durationMs = System.currentTimeMillis() - startedAt,
    )
  }
}

/**
 * Moves [dbFile] and its SQLite side files into `<databases>/legacy-migrated/`. Replaces a previous
 * quarantined copy of the same name. Falls back to copy + delete when a rename is not possible.
 */
internal fun quarantineLegacyDatabase(dbFile: File) {
  val dir = File(dbFile.parentFile, LEGACY_QUARANTINE_DIR)
  if (!dir.exists()) check(dir.mkdirs() || dir.exists()) { "failed to create quarantine dir $dir" }
  listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
    val source = File(dbFile.path + suffix)
    if (!source.exists()) return@forEach
    val target = File(dir, source.name)
    if (target.exists()) target.delete()
    if (!source.renameTo(target)) {
      source.copyTo(target, overwrite = true)
      source.delete()
    }
    Timber.i("quarantined $source → $target")
  }
}

internal suspend fun MigrationDbSource.migrateDataTo(currentDatabase: PwsDatabase): Result<MigrationReport> =
  kotlin.runCatching {
    Timber.i("found previous database $path ($version), migrate user data to...")
    val dataProviders = listOf(PwsDb1xDataProvider(this), PwsDb2xDataProvider(this))
    val dataProvider = dataProviders.find { this.version in it.dbVersions }
    checkNotNull(dataProvider) { "no data provider found for database version $version" }

    // Phase 1: install books from old DB if the target DB has no books yet.
    // This ensures (bookId, songNumber) lookups in Phase 2 can succeed even when
    // onboarding has not yet run (e.g. after a silent APK update on a device that
    // had one of the legacy named databases). The app shell runs this migration strictly before
    // seeding built-in books, so on an upgrade the target is still empty here.
    if (currentDatabase.bookDao().count() == 0) {
      val books = dataProvider.getBooks().getOrDefault(emptyList())
      Timber.i("installing ${books.size} books from old DB $path into empty target DB...")
      currentDatabase.withTransaction {
        // Insert all unique songs first — avoids CASCADE-delete of song_numbers that would
        // occur if we inserted songs per-book using REPLACE and the same song appears in
        // multiple books (replacing a song deletes its song_number rows via ON DELETE CASCADE).
        val allSongs = books.flatMap { it.songs }.distinctBy { it.id }
        currentDatabase.songDao().insert(allSongs)

        for (bookData in books) {
          if (currentDatabase.bookDao().getById(bookData.book.id) != null) continue
          currentDatabase.bookDao().update(bookData.book)
          currentDatabase.bookStatisticDao().upsert(bookData.bookStatistic)
          for (sn in bookData.songNumbers) {
            runCatching { currentDatabase.songNumberDao().insert(sn) }
              .onFailure { e -> Timber.w("skip duplicate song number ${sn.bookId}#${sn.number}: ${e.message}") }
          }
          currentDatabase.installedBookDao().upsert(bookData.installedBook)
        }
      }
      Timber.i("books installed from old DB $path")
    }

    // Phase 2: migrate user data (favorites, history, edited songs, tags). Every step is
    // idempotent, so a retry after a partial run (see migrateDataFromPrevDatabase) only adds what
    // could not be matched before.
    val favorites = dataProvider.getFavorites()
    val history = dataProvider.getHistory()
    val editedSongs = dataProvider.getEditedSongs()
    val tags = dataProvider.getTags()

    suspend fun getSongNumberEntities(songNumbers: List<SongNumber>) =
      songNumbers
        .groupBy { it.bookId }
        .mapValues { (_, values) -> values.map { it.number }.distinct() }
        .flatMap { (bookId, sn) -> currentDatabase.songNumberDao().getByBookIdAndSongNumbers(bookId, sn) }

    // upsert favorites
    Timber.i("upsert favorites from $path...")
    val favoritesCount = favorites.getOrNull()?.distinct()?.let { songNumbers ->
      val songNumberIds = getSongNumberEntities(songNumbers).map { it.id }
      val countUpserted = songNumberIds.map { id ->
        runCatching {
          currentDatabase.favoriteDao().addToFavorites(id)
        }.onFailure { e -> Timber.e("error upserting song $id to favorite: ${e.message}") }
      }.count { it.isSuccess }
      Timber.i("$countUpserted of ${songNumbers.size} favorites upserted")
      MigrationCount(found = songNumbers.size, migrated = countUpserted)
    }

    // upsert history; rows already present (same song, same access time) are kept as they are
    val historyCount = history.getOrNull()?.distinct()?.let { historyItems ->
      val existing = currentDatabase.historyDao().getAllFlow().first()
        .mapTo(HashSet()) { Triple(it.bookId, it.songId, it.accessTimestamp) }
      val results = historyItems.map { item ->
        val songNumberId =
          currentDatabase.songNumberDao().getByBookIdAndSongNumber(item.songNumber.bookId, item.songNumber.number)?.id
            ?: return@map false
        if (Triple(songNumberId.bookId, songNumberId.songId, item.timestamp) in existing) return@map true
        runCatching {
          currentDatabase.historyDao().insert(HistoryEntity(songNumberId, accessTimestamp = item.timestamp))
        }.onFailure { e -> Timber.e("error upserting history item $item to history: ${e.message}") }
          .isSuccess
      }
      Timber.i("${results.count { it }} of ${historyItems.size} history records present")
      MigrationCount(found = historyItems.size, migrated = results.count { it })
    }

    // upsert edited songs
    val editedSongsCount = editedSongs.getOrNull()?.distinctBy { it.number }?.let { songs ->
      val songIdBySongNumber =
        getSongNumberEntities(songs.map { it.number }).associate { SongNumber(it.bookId, it.number) to it.songId }
      val songChangeBySongId =
        songs.mapNotNull { change ->
          val songId = songIdBySongNumber[change.number]
          songId?.let { it to change }
        }.toMap()
      val updatedSongIds = songChangeBySongId.filter { (songId, change) ->
        runCatching {
          val song = checkNotNull(currentDatabase.songDao().getById(songId)) { "song $songId not found" }
          currentDatabase.songDao()
            .update(song.copy(lyric = change.lyric, bibleRef = change.bibleRef, tonalities = change.tonalities, edited = true))
        }.onFailure { e -> Timber.e("error updating song #$songId (${change.number}): ${e.message}") }
          .isSuccess
      }.keys
      Timber.i("${updatedSongIds.size} songs has been updated")
      // Counted per legacy (book, number) entry — one edited song is listed once per book it is in.
      MigrationCount(found = songs.size, migrated = songs.count { songIdBySongNumber[it.number] in updatedSongIds })
    }

    // upsert tags; counted as tag↔song links
    val tagsCount = tags.getOrNull()?.let { allTags ->
      val songNumbersByTagId = allTags.associate { t -> t.id to t.songNumbers.flatMap { (bookId, numbers) -> numbers.map { SongNumber(bookId, it) } } }
      val allSongNumbers = songNumbersByTagId.values.flatten().distinct()
      val songIdBySongNumber = getSongNumberEntities(allSongNumbers).associate { SongNumber(it.bookId, it.number) to it.songId }
      val predefinedTags = allTags.filter { it.predefined }
      val customTags = allTags - predefinedTags.toSet()
      val found = songNumbersByTagId.values.sumOf { it.size }
      var migrated = 0
      val customSongIdsByTagId =
        customTags.mapNotNull { tag ->
          runCatching {
            val existingTag = currentDatabase.tagDao().getAllByName(tag.name).firstOrNull()
            val targetTagId =
              if (existingTag != null) {
                Timber.d("existing custom tag '${tag.name}' (${existingTag.id}) found, update tag color")
                currentDatabase.tagDao().update(existingTag.copy(color = tag.color))
                existingTag.id
              } else {
                val tagId = currentDatabase.tagDao().getNextCustomTagId()
                currentDatabase.tagDao().insert(TagEntity(id = tagId, name = tag.name, color = tag.color, priority = 1000, predefined = false))
                Timber.d("new custom tag '${tag.name}' created: $tagId")
                tagId
              }
            targetTagId to (songNumbersByTagId[tag.id] ?: emptyList())
          }.onFailure { e -> Timber.e("error upserting  custom tag ${tag.name}: ${e.message}") }
            .getOrNull()
        }
      // Predefined tags normally arrive with book bundles; one the new database does not have yet is
      // created from the legacy row, so the user's links to it are not lost to the foreign key.
      val predefinedSongIdsByTagId =
        predefinedTags.mapNotNull { tag ->
          runCatching {
            val targetTagId =
              currentDatabase.tagDao().getById(tag.id)?.id
                ?: currentDatabase.tagDao().getAllByName(tag.name).firstOrNull()?.id
                ?: tag.id.also {
                  currentDatabase.tagDao().insert(TagEntity(id = tag.id, name = tag.name, color = tag.color, priority = 0, predefined = true))
                  Timber.d("predefined tag '${tag.name}' (${tag.id}) created from the previous database")
                }
            targetTagId to (songNumbersByTagId[tag.id] ?: emptyList())
          }.onFailure { e -> Timber.e("error upserting predefined tag ${tag.name}: ${e.message}") }
            .getOrNull()
        }
      (customSongIdsByTagId + predefinedSongIdsByTagId).forEach { (tagId, songNumbers) ->
        val matched = songNumbers.filter { it in songIdBySongNumber }
        val tagEntities = matched.mapNotNull { songIdBySongNumber[it] }.distinct().map { SongTagEntity(it, tagId) }
        runCatching {
          currentDatabase.songTagDao().insertIfMissing(tagEntities)
        }.onSuccess { migrated += matched.size }
          .onFailure { e -> Timber.e("error inserting ${tagEntities.size} tag entities for tag $tagId: ${e.message}") }
      }
      Timber.i("$migrated of $found tag links upserted")
      MigrationCount(found = found, migrated = migrated)
    }

    // A kind that could not be read fails the whole run (after the other kinds were migrated), so
    // the source file is kept and the migration retried.
    favorites.getOrThrow()
    history.getOrThrow()
    editedSongs.getOrThrow()
    tags.getOrThrow()
    MigrationReport(
      favorites = checkNotNull(favoritesCount),
      history = checkNotNull(historyCount),
      editedSongs = checkNotNull(editedSongsCount),
      tags = checkNotNull(tagsCount),
    )
  }
