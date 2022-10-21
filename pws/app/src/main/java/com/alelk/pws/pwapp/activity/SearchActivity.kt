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
import android.text.InputType
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.fragment.SearchResultsFragment

class SearchActivity : AppCompatThemedActivity() {
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
    var resultsFragment =
      supportFragmentManager.findFragmentById(R.id.fragment_search_results) as SearchResultsFragment?
    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY)
      if (resultsFragment != null) {
        resultsFragment.updateQuery(query)
      } else {
        resultsFragment = SearchResultsFragment.newInstance(query)
        supportFragmentManager.beginTransaction()
          .add(R.id.fragment_search_results, resultsFragment).commit()
      }
    } else if (Intent.ACTION_VIEW == intent.action) {
      val data = intent.data ?: return
      val n = data.lastPathSegment ?: return
      val psalmNumberId = n.toLong()
      if (psalmNumberId != -1L) {
        val intentPsalmView = Intent(applicationContext, PsalmActivity::class.java)
        intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentPsalmView)
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_search, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
    val searchView = menu.findItem(R.id.menu_search).actionView as SearchView?
    if (searchManager != null) searchView!!.setSearchableInfo(
      searchManager.getSearchableInfo(
        componentName
      )
    )
    val query = intent.getStringExtra(SearchManager.QUERY)
    if (TextUtils.isEmpty(query)) {
      searchView!!.isIconified = false
    } else {
      searchView!!.isIconified = true
      searchView.setQuery(query, false)
    }
    searchView.inputType = intent.getIntExtra(KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT)
    if (searchView.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER) {
      searchView.queryHint = getString(R.string.hint_enter_psalm_number)
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