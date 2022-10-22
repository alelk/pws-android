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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProvider
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.adapter.SearchRecyclerViewAdapter

/**
 * Search Result Fragment
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class SearchResultsFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private var mQuery: String? = null
  private var mRecyclerView: RecyclerView? = null
  private var mSearchResultsAdapter: SearchRecyclerViewAdapter? = null
  private var mLayoutSearchProgress: View? = null
  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (arguments != null) {
      mQuery = requireArguments().getString(KEY_QUERY)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_search_results, null)
    mRecyclerView = v.findViewById(R.id.rv_search_results)
    mLayoutSearchProgress = v.findViewById(R.id.layout_search_progress)
    val layoutManager = LinearLayoutManager(
      requireActivity().applicationContext
    )
    mRecyclerView?.layoutManager = layoutManager
    mSearchResultsAdapter =
      SearchRecyclerViewAdapter { psalmNumberId: Long ->
        val intentPsalmView = Intent(requireActivity().baseContext, PsalmActivity::class.java)
        intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentPsalmView)
      }
    mRecyclerView?.adapter = mSearchResultsAdapter
    mLayoutSearchProgress?.visibility = View.VISIBLE
    loaderManager.initLoader(PWS_SEARCH_RESULTS_LOADER, null, this)
    return v
  }

  override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> {
    if (TextUtils.isEmpty(mQuery)) throw java.lang.IllegalStateException("unable to create cursor loader")
    when (loaderId) {
      PWS_SEARCH_RESULTS_LOADER -> return CursorLoader(
        requireActivity().baseContext,
        PwsDataProviderContract.Psalms.Search.CONTENT_URI,
        null,
        null, arrayOf(mQuery),
        null
      )
      else -> throw java.lang.IllegalStateException("unable to create cursor loader")
    }
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    mSearchResultsAdapter!!.swapCursor(data)
    mLayoutSearchProgress!!.visibility = View.GONE
    mRecyclerView!!.visibility = View.VISIBLE
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mSearchResultsAdapter!!.swapCursor(null)
  }

  fun updateQuery(query: String?) {
    mQuery = query
    loaderManager.restartLoader(PWS_SEARCH_RESULTS_LOADER, null, this)
    if (mLayoutSearchProgress != null) mLayoutSearchProgress!!.visibility = View.VISIBLE
    if (mRecyclerView != null) mRecyclerView!!.visibility = View.INVISIBLE
  }

  companion object {
    const val KEY_QUERY = "com.alelk.pws.pwapp.query"
    const val PWS_SEARCH_RESULTS_LOADER = 4
    fun newInstance(query: String?): SearchResultsFragment {
      val args = Bundle()
      args.putString(KEY_QUERY, query)
      val fragment = SearchResultsFragment()
      fragment.arguments = args
      return fragment
    }
  }
}