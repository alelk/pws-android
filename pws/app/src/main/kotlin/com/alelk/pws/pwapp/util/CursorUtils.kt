package com.alelk.pws.pwapp.util

import android.database.Cursor
import com.alelk.pws.pwapp.model.Category
import com.alelk.pws.pwapp.model.PsalmInfo
import com.alelk.pws.pwapp.model.toCategory
import com.alelk.pws.pwapp.model.toPsalmInfo
import java.util.SortedSet

object CursorUtils {
  fun getCategoriesFromCursor(cursor: Cursor, categoriesList: SortedSet<Category>) {
    cursor.use {
      while (it.moveToNext()) {
        categoriesList.add(it.toCategory())
      }
    }
  }

  fun getPsalmsInfoFromCursor(cursor: Cursor): List<PsalmInfo> {
    val psalmInfoList = mutableListOf<PsalmInfo>()
    cursor.use {
      while (it.moveToNext()) {
        psalmInfoList.add(it.toPsalmInfo())
      }
    }
    return psalmInfoList
  }
}