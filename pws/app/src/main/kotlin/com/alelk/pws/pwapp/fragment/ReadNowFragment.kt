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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter

/**
 * Read Now Fragment
 *
 * Created by Alex Elkin on 17.02.2016.
 */
class ReadNowFragment : Fragment() {

  private lateinit var rvRecentPsalms: RecyclerView
  private lateinit var recentPsalmsAdapter: HistoryRecyclerViewAdapter
  private val historyViewModel: HistoryViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recentPsalmsAdapter = HistoryRecyclerViewAdapter { psalmNumberId: Long ->
      val intentPsalmView = Intent(requireActivity(), PsalmActivity::class.java)
      intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
      startActivity(intentPsalmView)
    }
  }

  override fun onResume() {
    super.onResume()
    recentPsalmsAdapter.notifyDataSetChanged()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_readnow, container, false)

    rvRecentPsalms = view.findViewById(R.id.rv_recent)
    rvRecentPsalms.layoutManager = LinearLayoutManager(requireContext())
    rvRecentPsalms.adapter = recentPsalmsAdapter
    rvRecentPsalms.isNestedScrollingEnabled = true

    historyViewModel.historyItems.observe(viewLifecycleOwner) { recentPsalms ->
      val limitedRecentPsalms = recentPsalms.take(DEFAULT_RECENT_LIMIT)
      recentPsalmsAdapter.submitList(limitedRecentPsalms)
    }

    return view
  }

  companion object {
    private const val DEFAULT_RECENT_LIMIT = 10

    @JvmStatic
    fun newInstance() = ReadNowFragment()
  }
}