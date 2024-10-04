package com.alelk.pws.pwapp.loader

import android.app.Activity
import android.content.ContentValues
import com.alelk.pws.database.provider.PwsDataProviderContract.Categories
import com.alelk.pws.database.provider.PwsDataProviderContract.Categories.BY_PSALM_NUMBER_SELECTION
import com.alelk.pws.database.table.PwsPsalmTagTable
import com.alelk.pws.pwapp.model.TagEntity
import com.alelk.pws.pwapp.util.CursorUtils
import java.util.SortedSet

class CategoryLoader(
  private val activity: Activity
) {
  private lateinit var categoriesList: SortedSet<TagEntity>

  fun loadData(): SortedSet<TagEntity> {
    categoriesList = sortedSetOf(compareBy<TagEntity> { it.predefined }.thenBy { it.priority }.thenBy { it.id })
    val cursor = activity.contentResolver.query(
      Categories.TAG_URI, null, null, null, null
    )
    CursorUtils.getCategoriesFromCursor(cursor!!, categoriesList)
    cursor.close()
    return categoriesList
  }

  fun loadCategoriesForPsalm(psalmNumberId: String): SortedSet<TagEntity> {
    categoriesList = sortedSetOf(compareBy<TagEntity> { it.predefined }.thenBy { it.priority }.thenBy { it.id })
    val cursor = activity.contentResolver.query(
      Categories.PSALM_TAG_URI, null, BY_PSALM_NUMBER_SELECTION, arrayOf(psalmNumberId), null
    )
    CursorUtils.getCategoriesFromCursor(cursor!!, categoriesList)
    cursor.close()
    return categoriesList
  }


  fun updateCategoriesForPsalm(
      psalmNumberId: String,
      categoriesToAdd: Set<TagEntity>,
      categoriesToRemove: Set<TagEntity>
  ) {
    // Add new categories
    for (category in categoriesToAdd) {
      val contentValues = ContentValues().apply {
        put(PwsPsalmTagTable.COLUMN_PSALM_NUMBER_ID, psalmNumberId)
        put(PwsPsalmTagTable.COLUMN_TAG_ID, category.id)
      }
      activity.contentResolver.insert(Categories.PSALM_TAG_URI, contentValues)
    }

    // Remove unassigned categories
    for (category in categoriesToRemove) {
      val whereClause =
        "${PwsPsalmTagTable.COLUMN_PSALM_NUMBER_ID} = ? AND ${PwsPsalmTagTable.COLUMN_TAG_ID} = ?"
      val whereArgs = arrayOf(psalmNumberId, category.id.toString())
      activity.contentResolver.delete(Categories.PSALM_TAG_URI, whereClause, whereArgs)
    }
  }


  private fun reloadData(): SortedSet<TagEntity> {
    return loadData()
  }

}