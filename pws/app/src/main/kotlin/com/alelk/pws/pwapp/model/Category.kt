package com.alelk.pws.pwapp.model

import android.database.Cursor
import com.alelk.pws.database.provider.PwsDataProviderContract

class Category(
  val id: String,
  val name: String,
  val color: String,
  val priority: Int,
  val predefined: Boolean = false
) {
  override fun toString(): String {
    return "Category(id=$id, name='$name', color='$color', predefined=$predefined)"
  }
}

fun Cursor.toCategory(): Category {
  val id = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_ID))
  val name = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_CATEGORY_NAME))
  val color = getString(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_CATEGORY_COLOR))
  val priority = getInt(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_CATEGORY_PRIORITY))
  val predefined = getInt(getColumnIndexOrThrow(PwsDataProviderContract.Categories.COLUMN_CATEGORY_PREDEFINED)) == 1

  return Category(id, name, color, priority, predefined)
}