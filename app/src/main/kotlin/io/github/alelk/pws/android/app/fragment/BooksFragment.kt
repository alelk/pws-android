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
package io.github.alelk.pws.android.app.fragment

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
import io.github.alelk.pws.android.app.activity.SongActivity
import io.github.alelk.pws.android.app.adapter.BooksRecyclerViewAdapter
import io.github.alelk.pws.android.app.model.BooksViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Books Fragment
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024
 */
@AndroidEntryPoint
class BooksFragment @Inject constructor() : Fragment() {

  private lateinit var booksAdapter: BooksRecyclerViewAdapter
  private val bookViewModel: BooksViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_books, container, false)
    val recyclerView = view.findViewById<RecyclerView>(R.id.rv_books)
    val layoutManager = LinearLayoutManager(requireContext())
    recyclerView.layoutManager = layoutManager
    booksAdapter = BooksRecyclerViewAdapter { songNumberId: Long ->
      val intent = Intent(requireActivity(), SongActivity::class.java)
      intent.putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId)
      startActivity(intent)
    }
    recyclerView.adapter = booksAdapter
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        bookViewModel.allActiveBooks.filterNotNull().collectLatest { books ->
          booksAdapter.submitList(books)
        }
      }
    }
    return view
  }
}

