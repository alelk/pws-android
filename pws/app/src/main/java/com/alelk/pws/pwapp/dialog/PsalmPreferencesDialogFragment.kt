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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.dialog.PsalmPreferencesDialogFragment
import com.alelk.pws.pwapp.preference.PsalmPreferences

/**
 * Psalm Preferences Dialog Fragment
 *
 * Created by Alex Elkin on 26.12.2016.
 */
class PsalmPreferencesDialogFragment : DialogFragment() {
  interface OnPsalmPreferencesChangedCallbacks {
    fun onPreferencesChanged(preferences: PsalmPreferences?)
    fun onApplyPreferences(preferences: PsalmPreferences?)
    fun onCancelPreferences(previousPreferences: PsalmPreferences?)
  }

  private var mLayout: View? = null
  private var mCallbacks: OnPsalmPreferencesChangedCallbacks? = null
  private var mDefaultPreferences: PsalmPreferences? = null
  private var mChangedPreferences: PsalmPreferences? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (activity == null) return
    mLayout = requireActivity().layoutInflater.inflate(R.layout.dialog_psalm_preferences, null)
    val skBrTextSize = mLayout!!.findViewById<SeekBar>(R.id.seek_bar_font_size)
    val switchIsPsalmTextExpanded = mLayout!!.findViewById<Switch>(R.id.swtch_expand_psalm_text)
    switchIsPsalmTextExpanded.isChecked = mDefaultPreferences!!.isExpandPsalmText
    skBrTextSize.progress =
      ((MAX_TEXT_SIZE - MIN_TEXT_SIZE) / 100 * (mDefaultPreferences!!.textSize - MIN_TEXT_SIZE)).toInt()
    switchIsPsalmTextExpanded.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
      mChangedPreferences!!.isExpandPsalmText = isChecked
      mCallbacks!!.onPreferencesChanged(mChangedPreferences)
    }
    skBrTextSize.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
        mChangedPreferences!!.textSize =
          i / (MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100 + MIN_TEXT_SIZE
        mCallbacks!!.onPreferencesChanged(mChangedPreferences)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)
    val builder = AlertDialog.Builder(activity)
    builder.setView(mLayout)
    builder.setPositiveButton(R.string.lbl_ok) { _: DialogInterface?, _: Int ->
      mCallbacks!!.onApplyPreferences(
        mChangedPreferences
      )
    }
    builder.setNegativeButton(R.string.lbl_cancel) { _: DialogInterface?, _: Int ->
      mCallbacks!!.onCancelPreferences(
        mDefaultPreferences
      )
    }
    return builder.create()
  }

  override fun onAttach(context: Context) {
    val METHOD_NAME = "onAttach"
    super.onAttach(context)
    try {
      mCallbacks = context as OnPsalmPreferencesChangedCallbacks
      init()
    } catch (ex: ClassCastException) {
      val message =
        context.toString() + " must implement " + OnPsalmPreferencesChangedCallbacks::class.java.canonicalName
      Log.e(LOG_TAG, "$METHOD_NAME: $message")
      throw ClassCastException(message)
    }
  }

  private fun init() {
    if (arguments != null) {
      mDefaultPreferences = PsalmPreferences(
        requireArguments().getFloat(KEY_TEXT_SIZE, 15f),
        requireArguments().getBoolean(KEY_EXPANDED_PSALM_TEXT, false)
      )
      mChangedPreferences = PsalmPreferences(
        mDefaultPreferences!!.textSize,
        mDefaultPreferences!!.isExpandPsalmText
      )
    }
  }

  companion object {
    const val KEY_TEXT_SIZE = "text_size"
    const val KEY_EXPANDED_PSALM_TEXT = "text_is_expanded"
    private const val MIN_TEXT_SIZE = 10f
    private const val MAX_TEXT_SIZE = 100f
    private val LOG_TAG = PsalmPreferencesDialogFragment::class.java.simpleName
    fun newInstance(preferences: PsalmPreferences): PsalmPreferencesDialogFragment {
      val args = Bundle()
      args.putFloat(KEY_TEXT_SIZE, preferences.textSize)
      args.putBoolean(KEY_EXPANDED_PSALM_TEXT, preferences.isExpandPsalmText)
      val dialogFragment = PsalmPreferencesDialogFragment()
      dialogFragment.arguments = args
      return dialogFragment
    }
  }
}