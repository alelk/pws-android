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

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
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
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter

/**
 * Read Now Fragment
 *
 * Created by Alex Elkin on 17.02.2016.
 */
class ReadNowFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private var rvRecentPsalms: RecyclerView? = null
  private var mRecentPsalmsAdapter: HistoryRecyclerViewAdapter? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mRecentPsalmsAdapter = HistoryRecyclerViewAdapter { psalmNumberId: Long? ->
      val intentPsalmView = Intent(requireActivity().baseContext, PsalmActivity::class.java)
      intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
      startActivity(intentPsalmView)
    }
    loaderManager.initLoader(PWS_RECENT_PSALM_LOADER, null, this)
  }

  override fun onResume() {
    super.onResume()
    mRecentPsalmsAdapter!!.notifyDataSetChanged()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_readnow, null)
    rvRecentPsalms = v.findViewById(R.id.rv_recent)
    val layoutManager = LinearLayoutManager(
      requireActivity().applicationContext
    )
    rvRecentPsalms?.layoutManager = layoutManager
    rvRecentPsalms?.adapter = mRecentPsalmsAdapter
    rvRecentPsalms?.isNestedScrollingEnabled = true
    return v
  }

  override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> =
    when (loaderId) {
      PWS_RECENT_PSALM_LOADER -> CursorLoader(
        requireActivity().baseContext,
        PwsDataProviderContract.History.getContentUri(DEFAULT_RECENT_LIMIT),
        null,
        null,
        null,
        null
      )
      else -> throw java.lang.IllegalStateException("unable to create cursor loader")
    }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    mRecentPsalmsAdapter!!.swapCursor(data)
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mRecentPsalmsAdapter!!.swapCursor(null)
  }

  companion object {
    const val PWS_RECENT_PSALM_LOADER = 30
    const val DEFAULT_RECENT_LIMIT = 10
  }
}