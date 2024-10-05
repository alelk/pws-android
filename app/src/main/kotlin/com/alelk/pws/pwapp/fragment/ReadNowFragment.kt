/*
 * Copyright (C) 2018-2024 The P&W Songs Open Source Project
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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.SongActivity
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter
import com.alelk.pws.pwapp.model.HistoryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Read Now Fragment
 *
 * Created by Alex Elkin on 17.02.2016.
 */
class ReadNowFragment : Fragment() {

  private val historyViewModel: HistoryViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = inflater.inflate(R.layout.fragment_readnow, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val historyAdapter = HistoryRecyclerViewAdapter { psalmNumberId: Long ->
      val intentPsalmView = Intent(activity, SongActivity::class.java)
      intentPsalmView.putExtra(SongActivity.KEY_SONG_NUMBER_ID, psalmNumberId)
      startActivity(intentPsalmView)
    }

    view.findViewById<RecyclerView>(R.id.rv_recent).apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = historyAdapter

    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        historyViewModel.historyItems.collectLatest { historyItems ->
          historyAdapter.submitList(historyItems.take(DEFAULT_RECENT_LIMIT))
        }
      }
    }
  }

  companion object {
    private const val DEFAULT_RECENT_LIMIT = 10
  }
}