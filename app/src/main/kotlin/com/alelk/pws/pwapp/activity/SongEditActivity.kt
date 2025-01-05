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
package com.alelk.pws.pwapp.activity

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
import com.alelk.pws.database.data.Tonality
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.fragment.SongTextFragment
import com.alelk.pws.pwapp.model.SongInfo
import com.alelk.pws.pwapp.model.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
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
    setContentView(R.layout.activity_psalm_edit)
    val songNumberId = intent.getLongExtra(KEY_SONG_NUMBER_ID, -1).takeIf { it > 0 }
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
    val songNameEdit = findViewById<EditText>(R.id.psalmNameEdit)
    val songLyricEdit = findViewById<EditText>(R.id.psalmTextEdit)
    val bibleRefEdit = findViewById<EditText>(R.id.bibleRefEdit)
    val songTonalitiesSpinner = findViewById<Spinner>(R.id.psalmTonalitiesSpinner)

    songNameEdit.setText(song.song.name)
    songLyricEdit.setText(song.song.lyric)
    bibleRefEdit.setText(song.song.bibleRef)

    // fixme: migrate to new tonality model
    val tonalities = Tonality.entries.map { it.signature.lowercase() } + resources.getString(R.string.tonality_not_defined)
    val tonalitiesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tonalities)
    tonalitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    songTonalitiesSpinner.adapter = tonalitiesAdapter

    val currentTonality = song.song.tonalities?.firstOrNull()?.identifier ?: resources.getString(R.string.tonality_not_defined)
    songTonalitiesSpinner.setSelection(tonalities.indexOf(currentTonality))
  }

  private suspend fun saveSong() {
    val name = findViewById<EditText>(R.id.psalmNameEdit).text.toString()
    val lyric = findViewById<EditText>(R.id.psalmTextEdit).text.toString()
    val bibleRef = findViewById<EditText>(R.id.bibleRefEdit).text.toString()
    val tonality = findViewById<Spinner>(R.id.psalmTonalitiesSpinner).selectedItem.toString()

    val nextTonality =
      if (tonality != getString(R.string.tonality_not_defined)) io.github.alelk.pws.domain.model.Tonality.fromIdentifier(tonality) else null

    songViewModel.update { it.copy(name = name, lyric = lyric, bibleRef = bibleRef, tonalities = listOfNotNull(nextTonality)) }

    val intent = Intent().apply { putExtra(SongActivity.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value) }
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        val intent = Intent().apply {
          putExtra(SongActivity.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value)
        }
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}