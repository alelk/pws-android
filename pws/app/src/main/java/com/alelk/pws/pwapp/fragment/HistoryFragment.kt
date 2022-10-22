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
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.provider.PwsDataProviderContract.History.getContentUri
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter
import com.alelk.pws.pwapp.fragment.HistoryFragment

/**
 * History Fragment
 *
 * Created by Alex Elkin on 18.02.2016.
 */
class HistoryFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private var mRecyclerView: RecyclerView? = null
  private var mHistoryAdapter: HistoryRecyclerViewAdapter? = null
  private var mItemsLimit = 0
  private var menuClearHistory: MenuItem? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_history, null)
    mRecyclerView = v.findViewById(R.id.rv_history)
    val layoutManager = LinearLayoutManager(activity)
    mRecyclerView?.layoutManager = layoutManager
    mHistoryAdapter =
      HistoryRecyclerViewAdapter { psalmNumberId: Long ->
        val intentPsalmView = Intent(activity, PsalmActivity::class.java)
        intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentPsalmView)
      }
    mRecyclerView?.adapter = mHistoryAdapter
    loaderManager.initLoader(PWS_HISTORY_LOADER, null, this)
    return v
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    menuClearHistory = menu.add(R.string.menu_clear_history)
    menuClearHistory?.setIcon(R.drawable.ic_delete_black_24dp)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == menuClearHistory!!.itemId) {
      val deleted =
        requireActivity().contentResolver.delete(PwsDataProviderContract.History.CONTENT_URI, null, null)
      Log.d(LOG_TAG, "onOptionsItemSelected: remove history: removed $deleted items.")
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    mItemsLimit = DEFAULT_ITEMS_LIMIT
    if (arguments != null) {
      mItemsLimit = requireArguments().getInt(KEY_ITEMS_LIMIT, DEFAULT_ITEMS_LIMIT)
    }
  }

  override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> =
    when (loaderId) {
      PWS_HISTORY_LOADER -> CursorLoader(
        requireActivity(),
        getContentUri(mItemsLimit),
        null,
        null,
        null,
        null
      )
      else -> throw java.lang.IllegalStateException("unable to create loader")
    }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    mHistoryAdapter!!.swapCursor(data)
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mHistoryAdapter!!.swapCursor(null)
  }

  companion object {
    private val LOG_TAG = HistoryFragment::class.java.simpleName
    const val KEY_ITEMS_LIMIT = "com.alelk.pws.pwapp.historyItemsLimit"
    const val PWS_HISTORY_LOADER = 2
    private const val DEFAULT_ITEMS_LIMIT = 100
    fun newInstance(itemsLimit: Int): HistoryFragment {
      val args = Bundle()
      args.putInt(KEY_ITEMS_LIMIT, itemsLimit)
      val fragment = HistoryFragment()
      fragment.arguments = args
      return fragment
    }
  }
}