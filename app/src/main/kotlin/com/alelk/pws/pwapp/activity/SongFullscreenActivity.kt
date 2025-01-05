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

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.SongTextFragmentPagerAdapter
import com.alelk.pws.pwapp.fragment.SongTextFragment
import com.alelk.pws.pwapp.model.BooksViewModel
import com.alelk.pws.pwapp.model.FavoritesViewModel
import com.alelk.pws.pwapp.model.SongsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// todo: reimplement or remove
@Deprecated("reimplement or remove")
@AndroidEntryPoint
class SongFullscreenActivity : AppCompatThemedActivity() {
  private var songNumberId: Long = -1

  private val songsViewModel: SongsViewModel by viewModels()
  private val bookViewModel: BooksViewModel by viewModels()
  private val favoritesViewModel: FavoritesViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_psalm_fullscreen)
    songNumberId = intent.getLongExtra(KEY_PSALM_NUMBER_ID, -1)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val songTextPager = findViewById<ViewPager2>(R.id.pager_psalm_text)
    lifecycleScope.launch {
      songsViewModel.getSongNumber(songNumberId).collect { n ->
        bookViewModel.getBookSongNumbers(n.bookId).collect { numbers ->
          val songNumberIds = numbers.map { checkNotNull(it.id) { "impossible state: no song number id" } }
          val pagerAdapter = SongTextFragmentPagerAdapter(this@SongFullscreenActivity, songNumberIds)
          songTextPager.adapter = pagerAdapter
          songTextPager.currentItem = songNumberIds.indexOf(songNumberId)
          songTextPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
              songNumberChanged(songNumberIds[position])
            }
          })
        }
      }
    }

    findViewById<Button>(R.id.btn_favorites).apply {
      setOnClickListener {
        lifecycleScope.launch {
          favoritesViewModel.toggleFavorite(songNumberId)
        }
      }
      setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) v.performClick()
        true
      }
    }
  }

  fun songNumberChanged(nextSongNumberId: Long) {
    this.songNumberId = nextSongNumberId
  }

  companion object {
    const val KEY_PSALM_NUMBER_ID = SongTextFragment.KEY_SONG_NUMBER_ID
  }
}