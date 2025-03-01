package io.github.alelk.pws.android.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.adapter.SongsInfoAdapter
import io.github.alelk.pws.android.app.model.TagsViewModel
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookEntity
import io.github.alelk.pws.domain.model.TagId

@AndroidEntryPoint
class TagSongsActivity : AppCompatThemedActivity() {
  private lateinit var recyclerView: RecyclerView
  private lateinit var songsInfoAdapter: SongsInfoAdapter
  private val tagsViewModel: TagsViewModel by viewModels()

  // Add sorting state
  private var sortOrder = SORT_BY_ADDED_DATE
  private var isAscending = false
  private val sharedPrefs by lazy {
    getSharedPreferences("category_sort_prefs", Context.MODE_PRIVATE)
  }

  // Add tagId as class property
  private lateinit var tagId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_category_songs)

    // Initialize tagId
    tagId = checkNotNull(intent.getStringExtra(TAG_ID)) { "$TAG_ID required" }
    val tagName = intent.getStringExtra(TAG_NAME) ?: ""

    title = "${getString(R.string.title_activity_category_songs)} $tagName"

    songsInfoAdapter = SongsInfoAdapter { songInfo -> onSongSelected(songInfo) }

    recyclerView = findViewById<RecyclerView>(R.id.category_songs_recycler_view).apply {
      adapter = songsInfoAdapter
      layoutManager = LinearLayoutManager(this@TagSongsActivity)
    }

    // Load saved preferences
    sortOrder = sharedPrefs.getInt(KEY_SORTED_BY, SORT_BY_ADDED_DATE)
    isAscending = sharedPrefs.getBoolean(KEY_IS_ASCENDING, false)

    tagsViewModel.getTagSongs(TagId.parse(tagId)).asLiveData().observe(this) { songInfoList ->
      val sortedList = when (sortOrder) {
        SORT_BY_NUMBER -> {
          if (isAscending) songInfoList.sortedBy { it.songNumber.number }
          else songInfoList.sortedByDescending { it.songNumber.number }
        }
        SORT_BY_NAME -> {
          if (isAscending) songInfoList.sortedBy { it.song.name }
          else songInfoList.sortedByDescending { it.song.name }
        }
        SORT_BY_ADDED_DATE -> {
          // todo: fix sorting by date
          if (isAscending) songInfoList.sortedBy { it.songNumber.id.identifier }
          else songInfoList.sortedByDescending { it.songNumber.id.identifier }
        }
        else -> songInfoList
      }
      songsInfoAdapter.swapData(sortedList)
    }
  }

  private fun onSongSelected(data: SongNumberWithSongWithBookEntity) {
    val intentSongView = Intent(this, SongActivity::class.java).apply {
      putExtra(SongActivity.KEY_SONG_NUMBER_ID, data.songNumber.id.toString())
    }
    startActivity(intentSongView)
  }

  // Add menu handling
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_category_sort, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
    updateSortMenuIcons(menu)
    return super.onPrepareOptionsMenu(menu)
  }

  private fun updateSortMenuIcons(menu: Menu) {
    listOf(
      R.id.action_sort_by_number to SORT_BY_NUMBER,
      R.id.action_sort_by_name to SORT_BY_NAME,
      R.id.action_sort_by_added_date to SORT_BY_ADDED_DATE
    ).forEach { (menuId, sortType) ->
      updateMenuIcon(menu.findItem(menuId), sortType)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_sort_by_number -> {
        updateSortAndRefresh(SORT_BY_NUMBER, true)
        true
      }
      R.id.action_sort_by_name -> {
        updateSortAndRefresh(SORT_BY_NAME, true)
        true
      }
      R.id.action_sort_by_added_date -> {
        updateSortAndRefresh(SORT_BY_ADDED_DATE, false)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  // Add sorting logic
  private fun updateSortAndRefresh(newSortOrder: Int, defaultAscending: Boolean) {
    if (sortOrder == newSortOrder) {
      isAscending = !isAscending
    } else {
      sortOrder = newSortOrder
      isAscending = defaultAscending
    }
    saveSortPreferences()
    refreshSongs()
    invalidateOptionsMenu()
  }

  private fun saveSortPreferences() {
    sharedPrefs.edit().apply {
      putInt(KEY_SORTED_BY, sortOrder)
      putBoolean(KEY_IS_ASCENDING, isAscending)
      apply()
    }
  }

  private fun refreshSongs() {
    tagsViewModel.getTagSongs(TagId.parse(tagId)).asLiveData().observe(this) { songs ->
      songsInfoAdapter.swapData(sortSongsList(songs))
    }
  }

  private fun sortSongsList(songs: List<SongNumberWithSongWithBookEntity>) = when (sortOrder) {
    SORT_BY_NUMBER -> songs.sortedByDirection { it.songNumber.number }
    SORT_BY_NAME -> songs.sortedByDirection { it.song.name }
    SORT_BY_ADDED_DATE -> songs.sortedByDirection { it.songNumber.id.identifier }
    else -> songs
  }

  private inline fun <T : Comparable<T>> List<SongNumberWithSongWithBookEntity>.sortedByDirection(
    crossinline selector: (SongNumberWithSongWithBookEntity) -> T
  ) = if (isAscending) sortedBy(selector) else sortedByDescending(selector)

  private fun updateMenuIcon(menuItem: MenuItem?, sortType: Int) {
    menuItem?.let { item ->
      if (sortOrder == sortType) {
        val iconRes = if (isAscending) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward
        item.setIcon(iconRes)
      } else {
        item.icon = null
      }
    }
  }

  companion object {
    const val TAG_NAME = "tag_name"
    const val TAG_ID = "tag_id"
    // Add sorting constants
    const val SORT_BY_ADDED_DATE = 2
    const val SORT_BY_NUMBER = 3
    const val SORT_BY_NAME = 4
    private const val KEY_SORTED_BY = "category-sorted-by"
    private const val KEY_IS_ASCENDING = "category-is-ascending"
  }
}