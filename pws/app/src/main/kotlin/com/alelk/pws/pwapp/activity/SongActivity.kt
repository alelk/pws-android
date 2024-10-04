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
package com.alelk.pws.pwapp.activity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.SongTextFragmentPagerAdapter
import com.alelk.pws.pwapp.dialog.PsalmPreferencesDialogFragment
import com.alelk.pws.pwapp.dialog.SearchPsalmNumberDialogFragment
import com.alelk.pws.pwapp.fragment.SongHeaderFragment
import com.alelk.pws.pwapp.model.SongViewModel
import com.alelk.pws.pwapp.model.textDocument
import com.alelk.pws.pwapp.model.textDocumentHtml
import com.alelk.pws.pwapp.theme.ThemeType
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Psalm Activity
 *
 * Created by Alex Elkin on 25.03.2016.
 */
class SongActivity : AppCompatThemedActivity() {
  private val songNumberIdState = MutableStateFlow(-1L)

  private val songViewModel: SongViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_psalm)
    val songNumberId = intent.getLongExtra(KEY_SONG_NUMBER_ID, -1L)
    Timber.d("song activity created: song number id = $songNumberId")
    songViewModel.setSongNumberId(songNumberId)

    val toolbar = findViewById<Toolbar>(R.id.toolbar_psalm)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    findViewById<FloatingActionButton>(R.id.fab_psalm).apply { setOnClickListener { lifecycleScope.launch { songViewModel.toggleFavorite() } } }
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          songViewModel.isFavorite.collect { isFavorite ->
            findViewById<FloatingActionButton>(R.id.fab_psalm).apply {
              if (isFavorite) setImageResource(R.drawable.ic_favorite_white_24dp)
              else setImageResource(R.drawable.ic_favorite_border_white_24dp)
            }
          }
        }
        launch {
          songViewModel.number.collect { songNumber -> findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_psalm).title = "№ $songNumber" }
        }
      }
    }

    (supportFragmentManager.findFragmentById(R.id.fragment_psalm_header) as? SongHeaderFragment?)
      ?: run {
        supportFragmentManager.commit {
          add(R.id.fragment_psalm_header, SongHeaderFragment())
        }
      }

    val songTextPager = findViewById<ViewPager2>(R.id.pager_psalm_text)
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        songViewModel.allBookNumbers.collect { allBookNumbers ->
          val songNumberIds = allBookNumbers?.map { checkNotNull(it.id) { "song number id cannot be null" } }
          Timber.d("book ${songViewModel.song.value?.book?.externalId} has ${songNumberIds?.size} song numbers")
          if (songNumberIds != null) {
            (songTextPager.adapter as? SongTextFragmentPagerAdapter)
              ?.setSongNumberIds(songNumberIds)
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
    menuInflater.inflate(R.menu.menu_psalm, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
    val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
    if (searchManager != null) searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == R.id.menu_jump) {
      val searchNumberDialog: DialogFragment =
        SearchPsalmNumberDialogFragment.newInstance(songNumberIdState.value)
      searchNumberDialog.show(
        supportFragmentManager,
        SearchPsalmNumberDialogFragment::class.java.simpleName
      )
      return true
    } else if (id == R.id.action_settings || id == R.id.menu_text_size) {
//      if (mPsalmPreferences!!.textSize < 0) {
//        val fragment =
//          mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//        mPsalmPreferences!!.textSize = fragment.psalmTextSize
//      }
      val psalmPreferencesDialog: DialogFragment =
        PsalmPreferencesDialogFragment.newInstance(mPsalmPreferences!!)
      psalmPreferencesDialog.show(
        supportFragmentManager,
        PsalmPreferencesDialogFragment::class.java.simpleName
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun shareSong() {
    songViewModel.song.value?.let { song ->
      val shareIntent = ShareCompat.IntentBuilder(this).setType("text/plain").setText(song.textDocument).setHtmlText(song.textDocumentHtml).intent
      startActivity(Intent.createChooser(shareIntent, getString(R.string.lbl_share)))
    }
  }

//  override fun onUpdateSongInfo(songHolder: SongHolder?) {
//    if (songHolder == null ||
//      mBookPsalmNumberIds!![mPagerPsalmText!!.currentItem] != songHolder.psalmNumberId
//    ) return
//    val collapsingToolbarLayout =
//      findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_psalm)
//    collapsingToolbarLayout.title = "№ " + songHolder.psalmNumber
//    mPsalmHeaderFragment!!.updateUi(
//      songHolder.psalmName,
//      songHolder.bookName,
//      songHolder.bibleRef
//    )
//  }
//
//  override fun onRequestFullscreenMode() {
//    val intent = Intent(this, SongFullscreenActivity::class.java)
//    intent.putExtra(
//      SongFullscreenActivity.KEY_PSALM_NUMBER_ID,
//      mBookPsalmNumberIds!![mPagerPsalmText!!.currentItem]
//    )
//    startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_ACTIVITY)
//  }
//
//  override fun onEditRequest(psalmNumberId: Long) {
//    val intent = Intent(this, SongEditActivity::class.java)
//    intent.putExtra(SongEditActivity.KEY_SONG_NUMBER_ID, psalmNumberId)
//    startActivityForResult(intent, REQUEST_CODE_EDIT_ACTIVITY)
//  }

//  @Deprecated("Deprecated in Java")
//  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//    super.onActivityResult(requestCode, resultCode, data)
//    when (requestCode) {
//      REQUEST_CODE_FULLSCREEN_ACTIVITY -> {
//        if (resultCode != RESULT_OK || data == null) return
//        songNumberId = data.getLongExtra(SongFullscreenActivity.KEY_PSALM_NUMBER_ID, -1)
//        mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(songNumberId)
//      }
//      REQUEST_CODE_EDIT_ACTIVITY -> {
//        if (data == null) return
//        songNumberId = data.getLongExtra(KEY_SONG_NUMBER_ID, -1L)
//        mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(songNumberId)
//        if (resultCode == RESULT_OK) {
//          val fragment =
//            mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//          fragment.reloadUi()
//        }
//      }
//    }
//  }

//  override fun onPositiveButtonClick(psalmNumberId: Long) {
//    mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(psalmNumberId)
//  }
//
//  override fun onNegativeButtonClick() {}
//  override fun onPreferencesChanged(preferences: PsalmPreferences?) {
//    if (preferences != null) {
//      val fragment = mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//      fragment.applyPsalmPreferences(preferences)
//    }
//  }

//  override fun onApplyPreferences(preferences: PsalmPreferences?) {
//    mPsalmPreferences = preferences
//    PreferenceManager.getDefaultSharedPreferences(baseContext)
//      .edit()
//      .putFloat(SongTextFragment.KEY_PSALM_TEXT_SIZE, preferences!!.textSize)
//      .putBoolean(SongTextFragment.KEY_PSALM_TEXT_EXPANDED, preferences.isExpandPsalmText)
//      .apply()
//    val fragment =
//      mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//    fragment.applyPsalmPreferences(preferences)
//    mPsalmTextPagerAdapter?.applyPsalmPreferences(preferences)
//  }
//
//  override fun onCancelPreferences(previousPreferences: PsalmPreferences?) {
//    mPsalmPreferences = previousPreferences
//    val fragment =
//      mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//    fragment.applyPsalmPreferences(previousPreferences!!)
//  }

//  private inner class FabFavoritesOnClick : View.OnClickListener {
//    override fun onClick(v: View) {
//      val fragment =
//        mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as SongTextFragment
//      if (fragment.isFavoritePsalm) {
//        fragment.removeSongFromFavorites()
//        Snackbar.make(v, R.string.msg_removed_from_favorites, Snackbar.LENGTH_SHORT)
//          .setAction("Action", null).show()
//      } else {
//        fragment.addSongToFavorites()
//        Snackbar.make(v, R.string.msg_added_to_favorites, Snackbar.LENGTH_SHORT)
//          .setAction("Action", null).show()
//      }
//    }
//  }


  override val themeType: ThemeType
    get() = ThemeType.NO_ACTION_BAR

  companion object {
    const val KEY_SONG_NUMBER_ID = "psalmNumberId"
    private const val REQUEST_CODE_FULLSCREEN_ACTIVITY = 1
    private const val REQUEST_CODE_EDIT_ACTIVITY = 2
  }
}