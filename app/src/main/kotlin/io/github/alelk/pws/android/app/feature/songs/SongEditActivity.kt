package io.github.alelk.pws.android.app.feature.songs

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
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.AppCompatThemedActivity
import io.github.alelk.pws.android.app.feature.songs.SongTextFragment
import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.tonality.Tonality
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SongEditActivity : AppCompatThemedActivity() {
  companion object {
    const val KEY_SONG_NUMBER_ID = SongTextFragment.Companion.KEY_SONG_NUMBER_ID
  }

  private val songViewModel: SongViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_song_edit)
    val songNumberId = intent.getStringExtra(KEY_SONG_NUMBER_ID)?.let(SongNumberId.Companion::parse)
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
    val bibleRef = findViewById<EditText>(R.id.bibleRefEdit).text.toString().takeIf { it.isNotBlank() }?.let {
        BibleRef(it)
    }
    val tonality = findViewById<Spinner>(R.id.songTonalitiesSpinner).selectedItem.toString()

    val nextTonality = if (tonality != "---") Tonality.Companion.fromIdentifier(tonality) else null

    songViewModel.update { it.copy(name = name, lyric = lyric, bibleRef = bibleRef, tonalities = listOfNotNull(nextTonality)) }

    val intent = Intent().apply { putExtra(SongActivity.Companion.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value?.toString()) }
    setResult(RESULT_OK, intent)
    finish()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        val intent = Intent().apply {
          putExtra(SongActivity.Companion.KEY_SONG_NUMBER_ID, songViewModel.songNumberId.value?.toString())
        }
        setResult(RESULT_CANCELED, intent)
        finish()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}