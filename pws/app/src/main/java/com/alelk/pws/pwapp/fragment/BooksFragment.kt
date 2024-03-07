/*
 * Copyright (C) 2024 The P&W Songs Open Source Project
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
import com.alelk.pws.pwapp.adapter.BooksRecyclerViewAdapter

/**
 * Books Fragment
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024
 */
class BooksFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private var mBooksAdapter: BooksRecyclerViewAdapter? = null
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_books, null)
    val recyclerView = v.findViewById<RecyclerView>(R.id.rv_books)
    val layoutManager = LinearLayoutManager(
      requireActivity().applicationContext
    )
    recyclerView.layoutManager = layoutManager
    mBooksAdapter =
      BooksRecyclerViewAdapter { psalmNumberId: Long ->
        val intentBooksView = Intent(requireActivity().baseContext, PsalmActivity::class.java)
        intentBooksView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentBooksView)
      }
    recyclerView.adapter = mBooksAdapter
    loaderManager.initLoader(PWS_BOOKS_LOADER, null, this)
    return v
  }

  override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> =
    when (loaderId) {
      PWS_BOOKS_LOADER -> CursorLoader(
        requireActivity().baseContext,
        PwsDataProviderContract.Books.CONTENT_URI,
        null,
        null,
        null,
        null
      )
      else -> throw java.lang.IllegalStateException("unable to create loader")
    }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    mBooksAdapter!!.swapCursor(data)
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mBooksAdapter!!.swapCursor(null)
  }

  companion object {
    const val PWS_BOOKS_LOADER = 70
  }
}