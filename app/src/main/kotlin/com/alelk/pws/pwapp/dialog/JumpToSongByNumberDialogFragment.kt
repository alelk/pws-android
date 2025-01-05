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
package com.alelk.pws.pwapp.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.SongActivity
import com.alelk.pws.pwapp.databinding.DialogSearchPsalmNumberBinding
import com.alelk.pws.pwapp.model.BookViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Search Psalm Number Dialog Fragment
 *
 *
 * Created by Alex Elkin on 12.06.2016.
 */
@AndroidEntryPoint
class JumpToSongByNumberDialogFragment : DialogFragment() {

  private var binding: DialogSearchPsalmNumberBinding? = null
  private val bookViewModel: BookViewModel by viewModels()

  private var songNumberToJump: Int? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = DialogSearchPsalmNumberBinding.inflate(layoutInflater)

    bookViewModel
      .setBookExternalId(
        BookExternalId.parse(checkNotNull(requireArguments().getString(KEY_BOOK_ID)) { "book external id argument required" })
      )

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        bookViewModel.songNumbers.filterNotNull().collectLatest { numbers ->
          val minNumber = numbers.minByOrNull { it.number }?.number ?: 1
          val maxNumber = numbers.maxByOrNull { it.number }?.number ?: 1
          binding?.edittxtPsalmNumber?.hint = "$minNumber - $maxNumber"
        }
      }
    }

    binding?.edittxtPsalmNumber?.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        songNumberToJump = s.toString().toIntOrNull()
      }

      override fun afterTextChanged(s: Editable) {}
    })

    return AlertDialog.Builder(requireActivity())
      .setView(binding?.root)
      .setPositiveButton(R.string.lbl_ok) { dialog, _ ->
        val number = bookViewModel.songNumbers.value?.find { n -> n.number == songNumberToJump }
        if (number != null) {
          startActivity(
            Intent(requireActivity(), SongActivity::class.java).apply { putExtra(SongActivity.KEY_SONG_NUMBER_ID, number.id) }
          )
          dialog.cancel()
        } else {
          binding?.root?.let { Snackbar.make(it, R.string.msg_no_psalm_number_found, Snackbar.LENGTH_SHORT).setAction("Action", null).show() }
          dialog.dismiss()
        }
      }
      .setNegativeButton(R.string.lbl_cancel) { d, _ -> d.dismiss() }
      .create()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }

  companion object {
    const val KEY_BOOK_ID = "bookId"

    fun newInstance(bookId: BookExternalId): JumpToSongByNumberDialogFragment {
      return JumpToSongByNumberDialogFragment().apply {
        arguments = Bundle().apply { putString(KEY_BOOK_ID, bookId.identifier) }
      }
    }
  }
}