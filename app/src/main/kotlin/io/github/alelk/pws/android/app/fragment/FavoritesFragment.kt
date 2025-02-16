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
package io.github.alelk.pws.android.app.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.activity.SongActivity
import io.github.alelk.pws.android.app.adapter.FavoritesRecyclerViewAdapter
import io.github.alelk.pws.android.app.model.FavoriteInfo
import io.github.alelk.pws.android.app.model.FavoritesViewModel
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Favorites Fragment
 *
 * Created by Alex Elkin on 18.02.2016.
 */
@AndroidEntryPoint
class FavoritesFragment @Inject constructor() : Fragment() {
  private var sortOrder = SORT_BY_ADDED_DATE // default
  private var isAscending = false // default order for added date is descending
  private val sharedPrefs by lazy {
    requireActivity().getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
  }
  private var mFavoritesAdapter: FavoritesRecyclerViewAdapter? = null
  private var menuSortByAddedDate: MenuItem? = null
  private var menuSortByName: MenuItem? = null
  private var menuSortByNumber: MenuItem? = null

  private val favoritesViewModel: FavoritesViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sortOrder = sharedPrefs.getInt(KEY_SORTED_BY, SORT_BY_ADDED_DATE)
    isAscending = sharedPrefs.getBoolean(KEY_IS_ASCENDING, if (sortOrder == SORT_BY_ADDED_DATE) false else true)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_favorite, container, false)
    val recyclerView = v.findViewById<RecyclerView>(R.id.rv_favorites)
    recyclerView.layoutManager = LinearLayoutManager(requireContext())

    mFavoritesAdapter = FavoritesRecyclerViewAdapter { songNumberId: SongNumberId ->
      val intentSongView = Intent(requireActivity(), SongActivity::class.java)
      intentSongView.putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId.toString())
      startActivity(intentSongView)
    }
    recyclerView.adapter = mFavoritesAdapter

    observeFavorites()

    return v
  }

  private var observeFavoritesJob: Job? = null

  private fun observeFavorites() {
    synchronized(this) {
      observeFavoritesJob?.cancel()
      observeFavoritesJob = viewLifecycleOwner.lifecycleScope.launch {
        favoritesViewModel.allFavorites.collectLatest { favorites ->
          when (sortOrder) {
            SORT_BY_NUMBER -> {
              if (isAscending) updateUI(favorites.sortedBy { it.songNumber })
              else updateUI(favorites.sortedByDescending { it.songNumber })
            }

            SORT_BY_NAME -> {
              if (isAscending) updateUI(favorites.sortedBy { it.songName })
              else updateUI(favorites.sortedByDescending { it.songName })
            }

            SORT_BY_ADDED_DATE -> {
              if (isAscending) updateUI(favorites.sortedBy { it.position })
              else updateUI(favorites.sortedByDescending { it.position })
            }
          }
        }
      }
    }
  }

  private fun updateUI(favorites: List<FavoriteInfo>) {
    mFavoritesAdapter?.submitList(favorites)
  }

  private fun saveSortPreferences() {
    sharedPrefs.edit().apply {
      putInt(KEY_SORTED_BY, sortOrder)
      putBoolean(KEY_IS_ASCENDING, isAscending)
      apply()
    }
  }

  private fun updateSortAndRefresh(newSortOrder: Int, defaultAscending: Boolean) {
    if (sortOrder == newSortOrder) {
      isAscending = !isAscending
    } else {
      sortOrder = newSortOrder
      isAscending = defaultAscending
    }
    observeFavorites()
    saveSortPreferences()
    requireActivity().invalidateOptionsMenu()
  }

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

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    saveSortPreferences()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    menuSortByAddedDate = menu.add(0, 1, 0, R.string.sort_by_added_date)
    menuSortByName = menu.add(0, 2, 0, R.string.sort_by_name)
    menuSortByNumber = menu.add(0, 3, 0, R.string.sort_by_number)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      menuSortByNumber!!.itemId -> {
        updateSortAndRefresh(SORT_BY_NUMBER, true)
        return true
      }

      menuSortByName!!.itemId -> {
        updateSortAndRefresh(SORT_BY_NAME, true)
        return true
      }

      menuSortByAddedDate!!.itemId -> {
        updateSortAndRefresh(SORT_BY_ADDED_DATE, false)
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    // Use the AndroidX MenuBuilder to force icons in the overflow menu
    if (menu is MenuBuilder) {
      menu.setOptionalIconsVisible(true)
    }

    updateMenuIcon(menuSortByNumber, SORT_BY_NUMBER)
    updateMenuIcon(menuSortByName, SORT_BY_NAME)
    updateMenuIcon(menuSortByAddedDate, SORT_BY_ADDED_DATE)

    super.onPrepareOptionsMenu(menu)
  }

  companion object {
    const val SORT_BY_ADDED_DATE = 2
    const val SORT_BY_NUMBER = 3
    const val SORT_BY_NAME = 4
    private const val KEY_SORTED_BY = "favorites-sorted-by"
    private const val KEY_IS_ASCENDING = "favorites-is-ascending"
  }
}
