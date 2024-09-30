package com.alelk.pws.pwapp.loader

import android.app.Activity
import android.content.ContentValues
import com.alelk.pws.database.provider.PwsDataProviderContract.Categories
import com.alelk.pws.database.provider.PwsDataProviderContract.Categories.BY_PSALM_NUMBER_SELECTION
import com.alelk.pws.database.provider.PwsDataProviderContract.Categories.BY_TAG_SELECTION
import com.alelk.pws.database.table.PwsPsalmTagTable
import com.alelk.pws.pwapp.model.Category
import com.alelk.pws.pwapp.model.PsalmInfo
import com.alelk.pws.pwapp.util.CursorUtils
import java.util.Locale
import java.util.SortedSet

class CategoryLoader(
  private val activity: Activity
) {
  private lateinit var categoriesList: SortedSet<Category>

  fun loadData(): SortedSet<Category> {
    categoriesList = sortedSetOf(compareBy<Category> { it.predefined }.thenBy { it.priority }.thenBy { it.id })
    val cursor = activity.contentResolver.query(
      Categories.TAG_URI, null, null, null, null
    )
    CursorUtils.getCategoriesFromCursor(cursor!!, categoriesList)
    cursor.close()
    return categoriesList
  }

  fun loadCategoriesForPsalm(psalmNumberId: String): SortedSet<Category> {
    categoriesList = sortedSetOf(compareBy<Category> { it.predefined }.thenBy { it.priority }.thenBy { it.id })
    val cursor = activity.contentResolver.query(
      Categories.PSALM_TAG_URI, null, BY_PSALM_NUMBER_SELECTION, arrayOf(psalmNumberId), null
    )
    CursorUtils.getCategoriesFromCursor(cursor!!, categoriesList)
    cursor.close()
    return categoriesList
  }

  fun loadPsalmsByCategory(categoryId: String): List<PsalmInfo> {
    val cursor = activity.contentResolver.query(
      Categories.PSALM_TAG_URI, null, BY_TAG_SELECTION, arrayOf(categoryId), null
    )
    val psalmInfoList = CursorUtils.getPsalmsInfoFromCursor(cursor!!)
    cursor.close()
    return psalmInfoList
  }

  fun saveData(category: Category): SortedSet<Category> {
    val values = ContentValues().apply {
      put(Categories.COLUMN_CATEGORY_NAME, category.name)
      put(Categories.COLUMN_CATEGORY_COLOR, category.color)
    }
    activity.contentResolver.insert(Categories.TAG_URI, values)
    return reloadData()
  }

  fun updateData(category: Category): SortedSet<Category> {
    val values = ContentValues().apply {
      put(Categories.COLUMN_CATEGORY_NAME, category.name)
      put(Categories.COLUMN_CATEGORY_COLOR, category.color)
    }
    activity.contentResolver.update(
      Categories.TAG_URI, values, Categories.SELECTION_ID_MATCH, arrayOf(category.id)
    )
    return reloadData()
  }

  fun updateCategoriesForPsalm(
    psalmNumberId: String,
    categoriesToAdd: Set<Category>,
    categoriesToRemove: Set<Category>
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

  fun deleteData(category: Category): SortedSet<Category> {
    val whereClause = "${PwsPsalmTagTable.COLUMN_TAG_ID} = ?"
    val whereArgs = arrayOf(category.id.toString())
    activity.contentResolver.delete(Categories.PSALM_TAG_URI, whereClause, whereArgs)

    activity.contentResolver.delete(
      Categories.TAG_URI, Categories.SELECTION_ID_MATCH, arrayOf(category.id.toString())
    )
    return reloadData()
  }

  private fun reloadData(): SortedSet<Category> {
    return loadData()
  }

  fun isCategoryNameUnique(name: String): Boolean {
    return categoriesList.none { it.name.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
  }
}