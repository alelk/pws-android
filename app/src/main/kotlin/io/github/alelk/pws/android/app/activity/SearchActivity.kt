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
import android.text.InputType
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.commit
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.fragment.SearchResultsFragment
import io.github.alelk.pws.android.app.model.SearchSongViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId

@AndroidEntryPoint
class SearchActivity : AppCompatThemedActivity() {

  private val searchViewModel: SearchSongViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_search)
    if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    handleIntent()
    if (Intent.ACTION_VIEW == intent.action) {
      finish()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent()
  }

  private fun handleIntent() {
    var resultsFragment = supportFragmentManager.findFragmentById(R.id.fragment_search_results) as? SearchResultsFragment
    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY)
      if (resultsFragment != null) {
        searchViewModel.setSearchQuery(query)
      } else {
        resultsFragment = SearchResultsFragment.newInstance(query)
        supportFragmentManager.commit {
          add(R.id.fragment_search_results, resultsFragment)
        }
      }
    } else if (Intent.ACTION_VIEW == intent.action) {
      val data = intent.data ?: return
      val songNumberId = SongNumberId.parse(data.lastPathSegment ?: return)
      val intentSongView = Intent(applicationContext, SongActivity::class.java).apply {
        putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId.toString())
      }
      startActivity(intentSongView)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_search, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
    val searchView = menu.findItem(R.id.menu_search).actionView as SearchView?
    if (searchManager != null) searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    val query = intent.getStringExtra(SearchManager.QUERY)
    if (TextUtils.isEmpty(query)) {
      searchView!!.isIconified = false
    } else {
      searchView!!.isIconified = true
      searchView.setQuery(query, false)
    }
    searchView.inputType = intent.getIntExtra(KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT)
    if (searchView.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER) {
      searchView.queryHint = getString(R.string.hint_enter_song_number)
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  companion object {
    const val KEY_INPUT_TYPE = "com.alelk.pws.pwapp.activity.SearchActivity.inputType"
  }
}