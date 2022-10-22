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
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.dialog.SearchPsalmNumberDialogFragment
import com.google.android.material.snackbar.Snackbar

/**
 * Search Psalm Number Dialog Fragment
 *
 *
 * Created by Alex Elkin on 12.06.2016.
 */
class SearchPsalmNumberDialogFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Cursor?> {
  interface SearchPsalmNumberDialogListener {
    fun onPositiveButtonClick(psalmNumberId: Long)
    fun onNegativeButtonClick()
  }

  private var mCurrentPsalmNumberId: Long = 0
  private var mPsalmNumberId: Long = -1
  private var mMinNumber = 0
  private var mMaxNumber = 0
  private var mPsalmNumberIdMap: HashMap<Int, Long?>? = null
  private var mListener: SearchPsalmNumberDialogListener? = null
  private var mTxtPsalmNumber: EditText? = null
  private var mView: View? = null
  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
    return if (id == PWS_PSALM_NUMBER_LOADER) {
      CursorLoader(
        requireActivity().baseContext,
        getContentUri(mCurrentPsalmNumberId),
        PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.PROJECTION,
        null, null, null
      )
    } else throw java.lang.IllegalStateException("unable to create cursor loader")
  }

  override fun onLoadFinished(loader: Loader<Cursor?>, cursor: Cursor?) {
    if (cursor != null && cursor.moveToFirst()) {
      mMinNumber =
        cursor.getInt(cursor.getColumnIndex(PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_MIN_PSALMNUMBER))
      mMaxNumber =
        cursor.getInt(cursor.getColumnIndex(PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_MAX_PSALMNUMBER))
      val psalmNumberList =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBER_LIST))
          .split(",").toTypedArray()
      val psalmNumberIdList =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST))
          .split(",").toTypedArray()
      mPsalmNumberIdMap = HashMap(psalmNumberList.size)
      for (i in psalmNumberList.indices) {
        try {
          mPsalmNumberIdMap!![psalmNumberList[i].toInt()] = psalmNumberIdList[i].toLong()
        } catch (ex: NumberFormatException) {
          Log.d(
            LOG_TAG,
            "onLoadFinished: cannot parse int (long) value: ${psalmNumberList[i]} ${psalmNumberIdList[i].toLong()}"
          )
        }
      }
    }
    updateUi()
  }

  private fun updateUi() {
    if (mMinNumber > 0 && mMaxNumber > 0 && mTxtPsalmNumber != null) {
      mTxtPsalmNumber!!.hint = "$mMinNumber - $mMaxNumber"
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor?>) {}
  override fun onAttach(context: Context) {
    val METHOD_NAME = "onAttach"
    super.onAttach(context)
    try {
      mListener = context as SearchPsalmNumberDialogListener
      init()
    } catch (ex: ClassCastException) {
      val message =
        "$context must implement ${SearchPsalmNumberDialogListener::class.java.canonicalName}"
      Log.e(LOG_TAG, "$METHOD_NAME: $message")
      throw ClassCastException(message)
    }
  }

  private fun init() {
    if (arguments != null) {
      mCurrentPsalmNumberId = requireArguments().getLong(KEY_CURRENT_PSALM_NUMBER_ID)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mView = requireActivity().layoutInflater.inflate(R.layout.dialog_search_psalm_number, null)
    mTxtPsalmNumber = mView?.findViewById(R.id.edittxt_psalm_number)
    mTxtPsalmNumber?.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        mPsalmNumberId = try {
          val number = s.toString().toInt()
          if (mPsalmNumberIdMap!![number] == null) -1 else mPsalmNumberIdMap!![number]!!
        } catch (ex: NumberFormatException) {
          -1
        }
      }

      override fun afterTextChanged(s: Editable) {}
    })
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    LoaderManager.getInstance(this).initLoader(PWS_PSALM_NUMBER_LOADER, null, this)
    val builder = AlertDialog.Builder(requireActivity())
    builder.setView(mView)
    builder.setPositiveButton(R.string.lbl_ok) { dialog: DialogInterface, _: Int ->
      if (mPsalmNumberId == -1L) {
        Snackbar.make(mView!!, R.string.msg_no_psalm_number_found, Snackbar.LENGTH_SHORT)
          .setAction("Action", null).show()
        dialog.dismiss()
        return@setPositiveButton
      }
      mListener!!.onPositiveButtonClick(mPsalmNumberId)
    }
    builder.setNegativeButton(R.string.lbl_cancel) { _: DialogInterface?, _: Int -> mListener!!.onNegativeButtonClick() }
    return builder.create()
  }

  companion object {
    const val KEY_CURRENT_PSALM_NUMBER_ID = "com.alelk.pws.pwapp.dialog.currentPsalmNumberId"
    const val PWS_PSALM_NUMBER_LOADER = 20
    private val LOG_TAG = SearchPsalmNumberDialogFragment::class.java.simpleName
    fun newInstance(currentPsalmNumberId: Long): SearchPsalmNumberDialogFragment {
      val args = Bundle()
      args.putLong(KEY_CURRENT_PSALM_NUMBER_ID, currentPsalmNumberId)
      val dialog = SearchPsalmNumberDialogFragment()
      dialog.arguments = args
      return dialog
    }
  }
}