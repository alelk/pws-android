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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.model.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/**
 * Psalm Header Fragment
 *
 * Created by Alex Elkin on 11.05.2016.
 */
@AndroidEntryPoint
class SongHeaderFragment : Fragment() {
  private val songViewModel: SongViewModel by activityViewModels()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_psalm_header, container, false)

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        songViewModel.song.filterNotNull().mapLatest { it.song to it.book }.distinctUntilChanged().collectLatest { (song, book) ->
          view.findViewById<TextView>(R.id.txt_psalm_name).text = song.name
          view.findViewById<TextView>(R.id.txt_book_name).text = book.displayName
          view.findViewById<TextView>(R.id.txt_bible_ref).text = song.bibleRef ?: ""
        }
      }
    }
  }
}