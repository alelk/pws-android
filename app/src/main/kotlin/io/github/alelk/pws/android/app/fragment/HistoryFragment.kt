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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.activity.SongActivity
import io.github.alelk.pws.android.app.adapter.HistoryRecyclerViewAdapter
import io.github.alelk.pws.android.app.model.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * History Fragment
 *
 * Created by Alex Elkin on 18.02.2016.
 */
@AndroidEntryPoint
class HistoryFragment @Inject constructor() : Fragment() {

  private val historyViewModel: HistoryViewModel by viewModels()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_history, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val historyAdapter = HistoryRecyclerViewAdapter { songNumberId: SongNumberId ->
      val intentSongView = Intent(activity, SongActivity::class.java)
      intentSongView.putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId.toString())
      startActivity(intentSongView)
    }

    view.findViewById<RecyclerView>(R.id.rv_history).apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = historyAdapter

    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        historyViewModel.historyItems.collectLatest { historyItems ->
          historyAdapter.submitList(historyItems)
        }
      }
    }

    setHasOptionsMenu(true)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val menuClearHistory = menu.add(R.string.menu_clear_history)
    menuClearHistory.setIcon(R.drawable.ic_delete_black_24dp)
    menuClearHistory.setOnMenuItemClickListener {
      viewLifecycleOwner.lifecycleScope.launch {
        historyViewModel.clearHistory()
      }
      true
    }
    return super.onCreateOptionsMenu(menu, inflater)
  }
}