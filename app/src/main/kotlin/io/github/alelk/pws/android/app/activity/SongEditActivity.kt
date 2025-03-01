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
package io.github.alelk.pws.android.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.fragment.SongTextFragment
import io.github.alelk.pws.android.app.model.SongInfo
import io.github.alelk.pws.android.app.model.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.SongNumberId
import io.github.alelk.pws.domain.model.Tonality
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SongEditActivity : AppCompatThemedActivity() {
  companion object {
    const val KEY_SONG_NUMBER_ID = SongTextFragment.KEY_SONG_NUMBER_ID
  }

  private val songViewModel: SongViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_song_edit)
    val songNumberId = intent.getStringExtra(KEY_SONG_NUMBER_ID)?.let(SongNumberId::parse)
    songViewModel.setSongNumberId(songNumberId)
    findViewById<Button>(R.id.saveButton).setOnClickListener {
      lifecycleScope.launch {
        saveSong()
      }
    }
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        songViewModel.song.filterNotNull().collectLatest { populateUIFromSong(it) }
      }
    }
  }

  private fun populateUIFromSong(song: SongInfo) {
    val songNameEdit = findViewById<EditText>(R.id.songNameEdit)
    val songLyricEdit = findViewById<EditText>(R.id.songTextEdit)
    val bibleRefEdit = findViewById<EditText>(R.id.bibleRefEdit)
    val songTonalitiesSpinner = findViewById<Spinner>(R.id.songTonalitiesSpinner)

    songNameEdit.setText(song.song.name)
    songLyricEdit.setText(song.song.lyric)
    bibleRefEdit.setText(song.song.bibleRef?.text)

    val tonalities = Tonality.entries.map { it.identifier } + "---"
    val tonalitiesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tonalities)
    tonalitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    songTonalitiesSpinner.adapter = tonalitiesAdapter

    val currentTonality = song.song.tonalities?.firstOrNull()?.identifier ?: "---"
    songTonalitiesSpinner.setSelection(tonalities.indexOf(currentTonality))
  }

  private suspend fun saveSong() {
    val name = findViewById<EditText>(R.id.songNameEdit).text.toString()
    val lyric = findViewById<EditText>(R.id.songTextEdit).text.toString()
    val bibleRef = findViewById<EditText>(R.id.bibleRefEdit).text.toString().takeIf { it.isNotBlank() }?.let { BibleRef(it) }
    val tonality = findViewById<Spinner>(R.id.songTonalitiesSpinner).selectedItem.toString()

    val nextTonality = if (tonality != "---") Tonality.fromIdentifier(tonality) else null

    songViewModel.update { it.copy(name = name, lyric = lyric, bibleRef = bibleRef, tonalities = listOfNotNull(nextTonality)) }

    val intent = Intent().apply { putExtra(SongActivity.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value?.toString()) }
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        val intent = Intent().apply {
          putExtra(SongActivity.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value?.toString())
        }
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}