package com.alelk.pws.pwapp.model

import android.database.Cursor
import com.alelk.pws.database.provider.PwsDataProviderContract

class PsalmInfo(
  val tagId: String,
  val psalmNumberId: Long,
  val bookName: String,
  val psalmNumber: Int,
  val psalmName: String
)

fun Cursor.toPsalmInfo(): PsalmInfo {
  val tagId = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_TAG_ID))
  val psalmNumberId = getLong(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_PSALM_NUMBER_ID))
  val bookName = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_BOOK_NAME))
  val psalmNumber = getInt(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_PSALM_NUMBER))
  val psalmName = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_PSALM_NAME))
  return PsalmInfo(tagId, psalmNumberId, bookName, psalmNumber, psalmName)
}