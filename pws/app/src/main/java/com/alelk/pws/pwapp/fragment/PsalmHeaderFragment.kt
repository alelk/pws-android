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
import com.alelk.pws.pwapp.R

/**
 * Psalm Header Fragment
 *
 * Created by Alex Elkin on 11.05.2016.
 */
class PsalmHeaderFragment : Fragment() {
  private var mPsalmName: String? = null
  private var mBookName: String? = null
  private var mBibleRef: String? = null
  private var vPsalmName: TextView? = null
  private var vBookName: TextView? = null
  private var vBibleRef: TextView? = null
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    val v = inflater.inflate(R.layout.fragment_psalm_header, null)
    vPsalmName = v.findViewById(R.id.txt_psalm_name)
    vBookName = v.findViewById(R.id.txt_book_name)
    vBibleRef = v.findViewById(R.id.txt_bible_ref)
    updateUi()
    return v
  }

  fun updateUi(psalmName: String?, bookName: String?, bibleRef: String?) {
    mPsalmName = psalmName
    mBookName = bookName
    mBibleRef = bibleRef
    updateUi()
  }

  private fun updateUi() {
    vPsalmName!!.text = if (mPsalmName == null) "" else mPsalmName
    vBookName!!.text = if (mBookName == null) "" else mBookName
    vBibleRef!!.text = if (mBibleRef == null) "" else mBibleRef
  }

  companion object {
    const val KEY_PSALM_NAME = "com.alelk.pws.pwapp.psalmName"
    const val KEY_BOOK_NAME = "com.alelk.pws.pwapp.bookName"
    const val KEY_BIBLE_REF = "com.alelk.pws.pwapp.bibleRef"
    fun newInstance(
      psalmName: String?,
      bookName: String?,
      bibleRef: String?
    ): PsalmHeaderFragment {
      val args = Bundle()
      args.putString(KEY_PSALM_NAME, psalmName)
      args.putString(KEY_BOOK_NAME, bookName)
      args.putString(KEY_BIBLE_REF, bibleRef)
      val fragment = PsalmHeaderFragment()
      fragment.arguments = args
      return fragment
    }
  }
}