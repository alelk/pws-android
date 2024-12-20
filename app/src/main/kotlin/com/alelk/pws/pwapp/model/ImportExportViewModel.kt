package com.alelk.pws.pwapp.model

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.Favorite
import com.alelk.pws.database.dao.SongDetails
import com.alelk.pws.pwapp.theme.AppTheme
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.database.common.model.Color
import io.github.alelk.pws.database.common.model.TagId
import io.github.alelk.pws.database.common.model.Tonality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream

class ImportExportViewModel(application: Application) : AndroidViewModel(application) {
  private val appPreferencesViewModel: AppPreferencesViewModel = AppPreferencesViewModel(getApplication())
  private val tagViewModel: TagsViewModel = TagsViewModel(application)

  private val db = DatabaseProvider.getDatabase(application)
  private val bookStatisticDao = db.bookStatisticDao()
  private val favoriteDao = db.favoriteDao()
  private val categoryDao = db.tagDao()
  private val songDao = db.songDao()
  private val songNumberTagDao = db.songNumberTagDao()
  private val songNumberDao = db.songNumberDao()

  suspend fun exportDatabase(outputFile: File): Boolean = withContext(Dispatchers.IO) {
    try {
      // Get all data
      val songTextSize = appPreferencesViewModel.songTextSize.firstOrNull()
      val songTextExpanded = appPreferencesViewModel.songTextExpanded.firstOrNull()
      val appTheme = appPreferencesViewModel.appTheme.firstOrNull()
      val bookPreferences = bookStatisticDao.getUserPreferredBooks().map {
        BookPreference(it.book.name, it.book.displayShortName, it.bookStatistic.userPreference ?: 0)
      }
      val favorites = favoriteDao.getAll().firstOrNull()
      val editedSongs = songDao.getAllEdited()
      val customCategories = categoryDao.getAllNotPredefined()
      val categoryDetails = customCategories.map {
        val songs = songNumberTagDao.getSongDetailsByTagId(it.id).map { sd ->
          sd.copy(sd.bookName, sd.bookShortName, sd.songNumber, sd.songName, null, null)
        }
        CategoryDetails(it.name, it.color, songs)
      }

      // Create export data structure
      val exportData = ExportData(
        version = 1,
        preferences = Preferences(songTextSize, songTextExpanded, appTheme, bookPreferences),
        favorites = favorites,
        categories = categoryDetails,
        editedSongs = editedSongs
      )

      // Serialize exportData to outputFile JSON
      val json = jacksonObjectMapper().writeValueAsString(exportData)
      Timber.d("Exported data: $json")
      outputFile.writeText(json)

      Timber.i("Database exported successfully to ${outputFile.absolutePath}")
      return@withContext true
    } catch (e: Exception) {
      Timber.e(e, "Failed to export database")
      return@withContext false
    }
  }

  suspend fun importDatabase(inputStream: InputStream, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
      // Deserialize inputFile to ExportData
      val exportData = jacksonObjectMapper().readValue(inputStream, ExportData::class.java)

      // Import new data
      exportData.preferences.appTheme?.let { appPreferencesViewModel.setAppTheme(it) }
      exportData.preferences.songTextExpanded?.let { appPreferencesViewModel.setSongTextExpanded(it) }
      exportData.preferences.songTextSize?.let { appPreferencesViewModel.setSongTextSize(it) }
      exportData.preferences.booksPreferences?.forEach { bookPreference ->
        bookStatisticDao.getByBookShortName(bookPreference.bookShortName)?.let {
          val updatedBookStatistic = it.bookStatistic.copy(userPreference = bookPreference.preference)
          bookStatisticDao.update(updatedBookStatistic)
        }
      }

      exportData.favorites?.forEach { favorite ->
        songNumberDao.getByBookShortNameAndSongNumber(favorite.bookShortName, favorite.songNumber)?.let {
          val songNumberId = it.id
          if (songNumberId != null) {
            favoriteDao.addToFavorites(songNumberId)
          } else {
            Timber.e("Song number id not found for favorite: id=${favorite.id}, songNumber=${favorite.songNumber}, songName=${favorite.songName}, bookDisplayName=${favorite.bookDisplayName}, bookShortName=${favorite.bookShortName}")
          }
        }
      }

      exportData.editedSongs?.forEach { songDetails ->
        // Find the song number entry for this book and number combination
        songNumberDao.getByBookShortNameAndSongNumber(songDetails.bookShortName, songDetails.songNumber)?.let { songNumber ->
          // Get the associated song
          songDao.getById(songNumber.songId)?.let { existingSong ->
            // Update the song with edited content
            val updatedSong = existingSong.copy(
              name = songDetails.songName,
              lyric = songDetails.songText ?: existingSong.lyric,
              bibleRef = songDetails.bibleReferences ?: existingSong.bibleRef,
              tonalities = songDetails.songTonality?.let { listOfNotNull(Tonality.fromIdentifier(it)) } ?: existingSong.tonalities,
              edited = true
            )
            songDao.update(updatedSong)
            Timber.d("Updated edited song: id=${updatedSong.id}, name=${updatedSong.name}")
          } ?: run {
            Timber.e("Song not found for number: bookShortName=${songDetails.bookShortName}, number=${songDetails.songNumber}")
          }
        } ?: run {
          Timber.e("Song number not found: bookShortName=${songDetails.bookShortName}, number=${songDetails.songNumber}")
        }
      }

      exportData.categories?.forEach { categoryDetails ->
        // First check if category already exists
        val existingCategory = categoryDao.getAllByName(categoryDetails.name).firstOrNull()
        val categoryEntity = if (existingCategory == null ) {
          // Create new category if it doesn't exist
          val newCategory = TagEntity(
            id = tagViewModel.getNextCustomTagId(),
            name = categoryDetails.name,
            color = categoryDetails.color,
            predefined = false,
            priority = 0 // Default priority for user-created categories
          )
          categoryDao.insert(newCategory)
          newCategory
        } else {
          // Update existing category
          val updatedCategory = existingCategory.copy(color = categoryDetails.color)
          categoryDao.update(updatedCategory)
          updatedCategory
        }

        // Add song-category relationships
        categoryDetails.songs.forEach { songDetails ->
          songNumberDao.getByBookShortNameAndSongNumber(songDetails.bookShortName, songDetails.songNumber)?.let { songNumber ->
            // Create song-tag relationship if it doesn't exist
            if (songNumberTagDao.getById(songNumber.id!!, categoryEntity.id) == null) {
              val songNumberTagEntity = SongNumberTagEntity(
                songNumberId = songNumber.id!!,
                tagId = categoryEntity.id
              )
              songNumberTagDao.insert(songNumberTagEntity)
            }
          } ?: run {
            Timber.e("Song number not found for category assignment: bookShortName=${songDetails.bookShortName}, number=${songDetails.songNumber}")
          }
        }
      }

      Timber.i("Database imported successfully from ${uri.path}")
      return@withContext true
    } catch (e: Exception) {
      Timber.e(e, "Failed to import database")
      return@withContext false
    }
  }
}

data class ExportData(
  val version: Int = 1,
  val preferences: Preferences,
  val favorites: List<Favorite>? = null,
  val categories: List<CategoryDetails>? = null,
  val editedSongs: List<SongDetails>? = null
)

data class Preferences(
  val songTextSize: Float?,
  val songTextExpanded: Boolean?,
  val appTheme: AppTheme?,
  val booksPreferences: List<BookPreference>?
)

data class BookPreference(
  val bookName: String,
  val bookShortName: String,
  val preference: Int
)

data class CategoryDetails(
  val name: String,
  val color: Color,
  val songs: List<SongDetails>
)