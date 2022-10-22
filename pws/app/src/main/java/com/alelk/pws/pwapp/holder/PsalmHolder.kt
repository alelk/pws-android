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
package com.alelk.pws.pwapp.holder

import android.database.Cursor
import android.text.TextUtils
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.Psalm
import java.util.*

/**
 * Psalm Holder
 *
 * Created by Alex Elkin on 27.05.2016.
 */
class PsalmHolder(cursor: Cursor?, _isFavoritePsalm: Boolean) : PwsHolder {
  var psalmNumberId: Long = 0
  var psalmNumber = 0
  var psalmName: String? = null
  var psalmText: String? = null
  var psalmAuthor: String? = null
  var psalmTranslator: String? = null
  var psalmComposer: String? = null
  var psalmLocale: String? = null
  var psalmTonalities: Array<String> = emptyArray()
  var bibleRef: String? = null
  var bookName: String? = null
  var isFavoritePsalm = false

  init {
    isFavoritePsalm = _isFavoritePsalm
    if (cursor != null && !cursor.isClosed) {
      var index: Int
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMNUMBER_ID).also { index = it } != -1) {
        psalmNumberId = cursor.getLong(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMNUMBER).also { index = it } != -1) {
        psalmNumber = cursor.getInt(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMNAME).also { index = it } != -1) {
        psalmName = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMTEXT).also { index = it } != -1) {
        psalmText = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMAUTHOR).also { index = it } != -1) {
        psalmAuthor = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMTRANSLATOR).also { index = it } != -1) {
        psalmTranslator = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMCOMPOSER).also { index = it } != -1) {
        psalmComposer = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMLOCALE).also { index = it } != -1) {
        psalmLocale = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMANNOTATION).also { index = it } != -1) {
        bibleRef = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_BOOKDISPLAYNAME).also { index = it } != -1) {
        bookName = cursor.getString(index)
      }
      if (cursor.getColumnIndex(Psalm.COLUMN_PSALMTONALITIES).also { index = it } != -1) {
        val tons = cursor.getString(index)
        if (!TextUtils.isEmpty(tons)) {
          psalmTonalities = tons.split("\\|").toTypedArray()
        }
      }
    }
  }

  override fun toString(): String {
    return "PsalmHolder{" +
      "mPsalmNumberId=" + psalmNumberId +
      ", mPsalmNumber=" + psalmNumber +
      ", mPsalmName='" + psalmName + '\'' +
      ", mPsalmText: " + (if (psalmText == null) null else psalmText!!.length.toString() + " symbols") +
      ", mPsalmAuthor='" + psalmAuthor + '\'' +
      ", mPsalmTranslator='" + psalmTranslator + '\'' +
      ", mPsalmComposer='" + psalmComposer + '\'' +
      ", mPsalmLocale='" + psalmLocale + '\'' +
      ", mPsalmTonalities=" + Arrays.toString(psalmTonalities) +
      ", mBibleRef='" + bibleRef + '\'' +
      ", mBookName='" + bookName + '\'' +
      ", isFavoritePsalm=" + isFavoritePsalm +
      '}'
  }
}