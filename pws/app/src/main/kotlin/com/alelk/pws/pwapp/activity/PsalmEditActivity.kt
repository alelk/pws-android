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
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.alelk.pws.database.data.Tonality
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.table.PwsPsalmTable
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.fragment.PsalmTextFragment
import com.alelk.pws.pwapp.holder.PsalmHolder

class PsalmEditActivity : AppCompatThemedActivity() {
  companion object {
    const val KEY_PSALM_NUMBER_ID = PsalmTextFragment.KEY_PSALM_NUMBER_ID
  }

  private var mPsalmNumberId = -1L
  private var mPsalmHolder: PsalmHolder? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_psalm_edit)
    // Initialization
    init()
    // UI setup
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val saveButton = findViewById<Button>(R.id.saveButton)
    saveButton.setOnClickListener {
      savePsalmDetails()
    }
    populateUIFromPsalmHolder()
  }

  private fun savePsalmDetails() {
    val name = findViewById<EditText>(R.id.psalmNameEdit).text.toString()
    val text = findViewById<EditText>(R.id.psalmTextEdit).text.toString()
    val bibleRef = findViewById<EditText>(R.id.bibleRefEdit).text.toString()
    val tonality = findViewById<Spinner>(R.id.psalmTonalitiesSpinner).selectedItem.toString()

    val values = ContentValues().apply {
      if (mPsalmHolder?.psalmName != name) put(PwsPsalmTable.COLUMN_NAME, name)
      if (mPsalmHolder?.psalmText != text) put(PwsPsalmTable.COLUMN_TEXT, text)
      if (mPsalmHolder?.bibleRef != bibleRef) put(PwsPsalmTable.COLUMN_ANNOTATION, bibleRef)
      val prevTonality = mPsalmHolder?.psalmTonalities?.firstOrNull()?.split(";")?.firstOrNull()?.trim()
      if (tonality != prevTonality) {
        // If the tonality is not defined, set it to null
        put(PwsPsalmTable.COLUMN_TONALITIES, Tonality.getInstanceBySignature(tonality)?.signature)
      }
    }

    if (values.size() > 0)
      contentResolver.update(
        PwsDataProviderContract.Psalms.getContentUri(mPsalmHolder!!.psalmId),
        values.apply { put(PwsPsalmTable.COLUMN_EDITED, true) },
        null,
        null
      )

    val intent = Intent()
    intent.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, mPsalmNumberId)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        val intent = Intent()
        intent.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, mPsalmNumberId)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }


  private fun init() {
    if (mPsalmNumberId >= 0) return
    mPsalmNumberId = intent.getLongExtra(KEY_PSALM_NUMBER_ID, -10L)
    var cursor: Cursor? = null
    try {
      cursor = contentResolver.query(
        PwsDataProviderContract.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId),
        null,
        null,
        null,
        null
      )
      if (cursor == null || !cursor.moveToFirst()) {
        return
      }
      mPsalmHolder = PsalmHolder(cursor, false)
    } finally {
      cursor?.close()
    }
  }

  private fun populateUIFromPsalmHolder() {
    mPsalmHolder?.let { holder ->
      // Retrieve each view by its ID
      val psalmNameEdit = findViewById<EditText>(R.id.psalmNameEdit)
      val psalmTextEdit = findViewById<EditText>(R.id.psalmTextEdit)
      val bibleRefEdit = findViewById<EditText>(R.id.bibleRefEdit)
      val psalmTonalitiesSpinner = findViewById<Spinner>(R.id.psalmTonalitiesSpinner)

      // Set the values for each EditText
      psalmNameEdit.setText(holder.psalmName)
      psalmTextEdit.setText(holder.psalmText)
      bibleRefEdit.setText(holder.bibleRef)

      // Setup ArrayAdapter for the Spinner with the array of tonalities
      val tonalities = Tonality.values().map { (it.signature) } + resources.getString(R.string.tonality_not_defined)
      val tonalitiesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tonalities)
      tonalitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      psalmTonalitiesSpinner.adapter = tonalitiesAdapter
      val currentTonality =
        holder.psalmTonalities.firstOrNull()?.split(';')?.firstOrNull()?.trim()?.let(Tonality::getInstanceBySignature)?.signature
          ?: resources.getString(R.string.tonality_not_defined)
      psalmTonalitiesSpinner.setSelection(tonalities.indexOf(currentTonality))
    }
  }

}