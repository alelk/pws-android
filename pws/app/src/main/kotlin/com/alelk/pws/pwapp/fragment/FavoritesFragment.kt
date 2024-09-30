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
package com.alelk.pws.pwapp.fragment

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.adapter.FavoritesRecyclerViewAdapter

/**
 * Favorites Fragment
 *
 * Created by Alex Elkin on 18.02.2016.
 */
class FavoritesFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private var sortOrder = SORT_BY_ADDED_DATE // by default
  private val sharedPrefs by lazy {
    requireActivity().getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
  }
  private var mFavoritesAdapter: FavoritesRecyclerViewAdapter? = null
  private var menuSortByAddedDate: MenuItem? = null
  private var menuSortByName: MenuItem? = null
  private var menuSortByNumber: MenuItem? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sortOrder = sharedPrefs.getInt(KEY_SORTED_BY, SORT_BY_ADDED_DATE)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_favorite, null)
    val recyclerView = v.findViewById<RecyclerView>(R.id.rv_favorites)
    val layoutManager = LinearLayoutManager(
      requireActivity().applicationContext
    )
    recyclerView.layoutManager = layoutManager
    mFavoritesAdapter =
      FavoritesRecyclerViewAdapter { psalmNumberId: Long ->
        val intentPsalmView = Intent(requireActivity().baseContext, PsalmActivity::class.java)
        intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentPsalmView)
      }
    recyclerView.adapter = mFavoritesAdapter
    loaderManager.initLoader(PWS_FAVORITES_LOADER, null, this)
    return v
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    with(sharedPrefs.edit()) {
      putInt(KEY_SORTED_BY, sortOrder)
      apply()
    }
  }

  override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> =
    when (loaderId) {
      PWS_FAVORITES_LOADER -> CursorLoader(
        requireActivity().baseContext,
        PwsDataProviderContract.Favorites.CONTENT_URI,
        null,
        null,
        null,
        when (sortOrder) {
          SORT_BY_NUMBER -> "${PwsDataProviderContract.Favorites.COLUMN_PSALMNUMBER} ASC"
          SORT_BY_NAME -> "${PwsDataProviderContract.Favorites.COLUMN_PSALMNAME} ASC"
          SORT_BY_ADDED_DATE -> PwsDataProviderContract.Favorites.SORT_ORDER
          else -> null
        }
      )

      else -> throw java.lang.IllegalStateException("unable to create loader")
    }

  @Deprecated("Deprecated in Java")
  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    menuSortByAddedDate = menu.add(0, 1, 0, R.string.sort_by_added_date)
    menuSortByName = menu.add(0, 2, 0, R.string.sort_by_name)
    menuSortByNumber = menu.add(0, 3, 0, R.string.sort_by_number)
    super.onCreateOptionsMenu(menu, inflater)
  }

  @Deprecated("Deprecated in Java")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val result = when (item.itemId) {
      menuSortByNumber!!.itemId -> {
        this.sortOrder = SORT_BY_NUMBER
        mFavoritesAdapter?.updateView()
        loaderManager.restartLoader(PWS_FAVORITES_LOADER, null, this)
        true
      }

      menuSortByName!!.itemId -> {
        this.sortOrder = SORT_BY_NAME
        mFavoritesAdapter?.updateView()
        loaderManager.restartLoader(PWS_FAVORITES_LOADER, null, this)
        true
      }

      menuSortByName!!.itemId -> {
        this.sortOrder = SORT_BY_ADDED_DATE
        mFavoritesAdapter?.updateView()
        loaderManager.restartLoader(PWS_FAVORITES_LOADER, null, this)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
    with(sharedPrefs.edit()) {
      putInt(KEY_SORTED_BY, sortOrder)
      apply()
    }
    return result
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    mFavoritesAdapter!!.swapCursor(data)
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mFavoritesAdapter!!.swapCursor(null)
  }

  companion object {
    const val PWS_FAVORITES_LOADER = 1
    const val SORT_BY_ADDED_DATE = 2
    const val SORT_BY_NUMBER = 3
    const val SORT_BY_NAME = 4
    private const val KEY_SORTED_BY = "favorites-sorted-by"
  }
}