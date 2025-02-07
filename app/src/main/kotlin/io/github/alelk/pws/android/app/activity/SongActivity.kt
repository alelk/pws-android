/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.alelk.pws.android.app.activity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.alelk.pws.pwapp.R
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.adapter.SongTextFragmentPagerAdapter
import io.github.alelk.pws.android.app.dialog.EditSongTagsDialog
import io.github.alelk.pws.android.app.dialog.JumpToSongByNumberDialogFragment
import io.github.alelk.pws.android.app.dialog.SongPreferencesDialogFragment
import io.github.alelk.pws.android.app.fragment.SongHeaderFragment
import io.github.alelk.pws.android.app.model.SongViewModel
import io.github.alelk.pws.android.app.model.TagsViewModel
import io.github.alelk.pws.android.app.model.textDocument
import io.github.alelk.pws.android.app.model.textDocumentHtml
import io.github.alelk.pws.android.app.theme.ThemeType
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Psalm Activity
 *
 * Created by Alex Elkin on 25.03.2016.
 */
@AndroidEntryPoint
class SongActivity : AppCompatThemedActivity() {
  private val songViewModel: SongViewModel by viewModels()
  private val tagsViewModel: TagsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_song)
    val songNumberId = intent.getLongExtra(KEY_SONG_NUMBER_ID, -1L)
    Timber.d("song activity created: song number id = $songNumberId")
    songViewModel.setSongNumberId(songNumberId)

    val toolbar = findViewById<Toolbar>(R.id.toolbar_song)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    findViewById<FloatingActionButton>(R.id.fab_song).apply { setOnClickListener { lifecycleScope.launch { songViewModel.toggleFavorite() } } }
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          songViewModel.isFavorite.collect { isFavorite ->
            findViewById<FloatingActionButton>(R.id.fab_song).apply {
              if (isFavorite) setImageResource(R.drawable.ic_favorite_white_24dp)
              else setImageResource(R.drawable.ic_favorite_border_white_24dp)
            }
          }
        }
        launch {
          songViewModel.number.collect { songNumber -> findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_song).title = "№ $songNumber" }
        }
      }
    }

    (supportFragmentManager.findFragmentById(R.id.fragment_song_header) as? SongHeaderFragment?)
      ?: run {
        supportFragmentManager.commit {
          add(R.id.fragment_song_header, SongHeaderFragment())
        }
      }

    val songTextPager = findViewById<ViewPager2>(R.id.pager_song_text)
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        songViewModel.allBookNumbers.collect { allBookNumbers ->
          val songNumberIds = allBookNumbers?.map { checkNotNull(it.id) { "song number id cannot be null" } }
          Timber.d("book ${songViewModel.song.value?.book?.externalId} has ${songNumberIds?.size} song numbers")
          if (songNumberIds != null) {
            (songTextPager.adapter as? SongTextFragmentPagerAdapter)
              ?.let { it.allSongNumberIds = songNumberIds }
              ?: run {
                songTextPager.adapter = SongTextFragmentPagerAdapter(this@SongActivity, songNumberIds)
                songTextPager.currentItem = songNumberIds.indexOf(songNumberId)
                songTextPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                  override fun onPageSelected(position: Int) {
                    songViewModel.setSongNumberId(songNumberIds[position])
                  }
                })
              }
          }
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_song, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as? SearchManager
    val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
    if (searchManager != null) {
      searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
    when (item.itemId) {
      R.id.menu_jump -> {
        val bookId = songViewModel.song.value?.book?.externalId
        if (bookId != null)
          JumpToSongByNumberDialogFragment.newInstance(bookId).show(supportFragmentManager, JumpToSongByNumberDialogFragment::class.java.simpleName)
        true
      }

      R.id.action_settings, R.id.menu_text_size -> {
        SongPreferencesDialogFragment().show(supportFragmentManager, SongPreferencesDialogFragment::class.java.simpleName)
        true
      }

      R.id.menu_edit -> {
        songViewModel.songNumberId.value?.let { songNumberId ->
          startActivity(Intent(this, SongEditActivity::class.java).putExtra(SongEditActivity.KEY_SONG_NUMBER_ID, songNumberId))
        }
        true
      }

      R.id.menu_edit_categories -> {
        lifecycleScope.launch {
          val songTags = songViewModel.tags.filterNotNull().firstOrNull()
          val allTags = tagsViewModel.allTags.filterNotNull().firstOrNull()
          if (songTags != null && allTags != null) {
            EditSongTagsDialog(this@SongActivity).show(songTags, allTags) { assignedTags ->
              lifecycleScope.launch {
                songViewModel.setTags(assignedTags)
              }
            }
          }
        }

        true
      }

      R.id.menu_share -> {
        songViewModel.song.value?.let { song ->
          val shareIntent = ShareCompat.IntentBuilder(this).setType("text/plain").setText(song.textDocument).setHtmlText(song.textDocumentHtml).intent
          startActivity(Intent.createChooser(shareIntent, getString(R.string.lbl_share)))
        }
        true
      }

      else -> {
        super.onOptionsItemSelected(item)
      }
    }

  override val themeType: ThemeType get() = ThemeType.NO_ACTION_BAR

  companion object {
    const val KEY_SONG_NUMBER_ID = "psalmNumberId"
  }
}