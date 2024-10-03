package com.alelk.pws.pwapp.dialog

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.model.Category
import com.alelk.pws.pwapp.view.CategoryView
import com.google.android.flexbox.FlexboxLayout
import java.util.SortedSet

class EditSongTagsDialog(private val activity: Activity) {

  private lateinit var dialogView: View
  private lateinit var assignedCategoriesFlexbox: FlexboxLayout
  private lateinit var unassignedCategoriesFlexbox: FlexboxLayout
  private var assignedCategories = sortedSetOf(
    compareBy<Category> { it.predefined }.thenBy { it.id }
  )
  private var unassignedCategories = sortedSetOf(
    compareBy<Category> { it.predefined }.thenBy { it.id }
  )

  fun showEditCategoryDialog(
    initialAssignedCategories: SortedSet<Category>,
    allCategories: SortedSet<Category>,
    onResult: (SortedSet<Category>) -> Unit
  ) {
    assignedCategories.addAll(initialAssignedCategories)
    unassignedCategories.addAll(
      allCategories.filterNot { category -> assignedCategories.any { it.name == category.name } }
    )

    setupDialogView()

    val dialog = AlertDialog.Builder(activity)
      .setTitle(activity.getString(R.string.lbl_edit_categories_for_song))
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { dialog, _ ->
        onResult(assignedCategories)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  private fun setupDialogView() {
    dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_psalm_category, null)
    assignedCategoriesFlexbox = dialogView.findViewById(R.id.assigned_categories)
    unassignedCategoriesFlexbox = dialogView.findViewById(R.id.unassigned_categories)

    updateCategoryViews()
  }

  private fun updateCategoryViews() {
    assignedCategoriesFlexbox.removeAllViews()
    unassignedCategoriesFlexbox.removeAllViews()

    assignedCategoriesView(assignedCategoriesFlexbox, assignedCategories) { category ->
      assignedCategories.removeAll { it.name == category.name }
      unassignedCategories.add(category)
      updateCategoryViews()
    }

    unassignedCategoriesView(unassignedCategoriesFlexbox, unassignedCategories) { category ->
      assignedCategories.add(category)
      unassignedCategories.removeAll { it.name == category.name }
      updateCategoryViews()
    }
  }

  private fun assignedCategoriesView(
    assignedCategoriesFlexBox: FlexboxLayout,
    assignedCategories: SortedSet<Category>,
    unAssignCategoryListener: (category: Category) -> Unit
  ) {
    for (category in assignedCategories) {
      val categoryView = CategoryView(activity, category)
      categoryView.setOnClickListener {
        unAssignCategoryListener(category)
      }
      categoryView.deleteMode()
      assignedCategoriesFlexBox.addView(categoryView)
    }
  }

  private fun unassignedCategoriesView(
    unassignedCategoriesFlexbox: FlexboxLayout,
    unassignedCategories: SortedSet<Category>,
    assignCategoryListener: (category: Category) -> Unit
  ) {
    for (category in unassignedCategories) {
      val categoryView = CategoryView(activity, category)
      categoryView.setOnClickListener {
        assignCategoryListener(category)
      }
      categoryView.addMode()
      unassignedCategoriesFlexbox.addView(categoryView)
    }
  }
}