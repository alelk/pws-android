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
package io.github.alelk.pws.android.app.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.databinding.DialogSongPreferencesBinding
import io.github.alelk.pws.android.app.model.AppPreferencesViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Song Preferences Dialog Fragment
 *
 * Created by Alex Elkin on 26.12.2016.
 */
class SongPreferencesDialogFragment : DialogFragment() {
  private val viewModel: AppPreferencesViewModel by viewModels()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val binding = DialogSongPreferencesBinding.inflate(layoutInflater)
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          val songTextSize = viewModel.songTextSize.firstOrNull()
          val textExpanded = viewModel.songTextExpanded.firstOrNull()
          if (songTextSize != null) binding.seekBarFontSize.progress = ((songTextSize - MIN_TEXT_SIZE) / (MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100).toInt()
          if (textExpanded != null) binding.swtchExpandSongText.isChecked = textExpanded
        }
      }
    }

    binding.seekBarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val textSize = MIN_TEXT_SIZE + (progress / 100f) * (MAX_TEXT_SIZE - MIN_TEXT_SIZE)
        lifecycleScope.launch {
          viewModel.setSongTextSize(textSize)
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {}
      override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })

    binding.swtchExpandSongText.setOnCheckedChangeListener { _, isChecked ->
      lifecycleScope.launch {
        Timber.d("is expanded: $isChecked")
        viewModel.setSongTextExpanded(isChecked)
      }
    }
    return AlertDialog.Builder(requireContext())
      .setView(binding.root)
      .setPositiveButton(R.string.lbl_ok) { _, _ -> }
      .setNegativeButton(R.string.lbl_cancel) { _, _ -> }
      .create()
  }

  companion object {
    const val MIN_TEXT_SIZE = 10f
    const val MAX_TEXT_SIZE = 100f
  }
}