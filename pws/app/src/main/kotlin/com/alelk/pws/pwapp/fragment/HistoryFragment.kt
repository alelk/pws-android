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

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.HistoryDao
import com.alelk.pws.database.dao.HistoryItem
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.SongActivity
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter
import kotlinx.coroutines.launch

/**
 * History Fragment
 *
 * Created by Alex Elkin on 18.02.2016.
 */
class HistoryFragment : Fragment() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var historyAdapter: HistoryRecyclerViewAdapter
  private val historyViewModel: HistoryViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_history, container, false)

    recyclerView = view.findViewById(R.id.rv_history)
    recyclerView.layoutManager = LinearLayoutManager(activity)

    historyAdapter = HistoryRecyclerViewAdapter { psalmNumberId: Long ->
      val intentPsalmView = Intent(activity, SongActivity::class.java)
      intentPsalmView.putExtra(SongActivity.KEY_SONG_NUMBER_ID, psalmNumberId)
      startActivity(intentPsalmView)
    }

    recyclerView.adapter = historyAdapter

    // Observe the history items from ViewModel
    historyViewModel.historyItems.observe(viewLifecycleOwner) { historyItems ->
      historyAdapter.submitList(historyItems)
    }

    return view
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val menuClearHistory = menu.add(R.string.menu_clear_history)
    menuClearHistory.setIcon(R.drawable.ic_delete_black_24dp)
    menuClearHistory.setOnMenuItemClickListener {
      historyViewModel.clearHistory()
      true
    }
    super.onCreateOptionsMenu(menu, inflater)
  }

  companion object {
    fun newInstance(): HistoryFragment {
      return HistoryFragment()
    }
  }
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

  private val historyDao: HistoryDao = DatabaseProvider.getDatabase(application).historyDao()

  val historyItems: LiveData<List<HistoryItem>> = historyDao.getAll().asLiveData()

  fun clearHistory() = viewModelScope.launch {
    historyDao.deleteAll()
  }
}