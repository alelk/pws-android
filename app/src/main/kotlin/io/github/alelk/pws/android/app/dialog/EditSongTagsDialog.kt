package io.github.alelk.pws.android.app.dialog

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import io.github.alelk.pws.android.app.view.TagView
import com.google.android.flexbox.FlexboxLayout
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.common.entity.TagEntity
import java.util.SortedSet

class EditSongTagsDialog(private val activity: Activity) {

  private lateinit var dialogView: View
  private lateinit var assignedCategoriesFlexbox: FlexboxLayout
  private lateinit var unassignedCategoriesFlexbox: FlexboxLayout
  private var assignedCategories = sortedSetOf(compareBy<TagEntity> { it.predefined }.thenBy { it.id.toString() })
  private var unassignedCategories = sortedSetOf(compareBy<TagEntity> { it.predefined }.thenBy { it.id.toString() })

  fun show(
    initialAssignedCategories: List<TagEntity>,
    allCategories: List<TagEntity>,
    onResult: (assigned: List<TagEntity>) -> Unit
  ) {
    assignedCategories.addAll(initialAssignedCategories)
    unassignedCategories.addAll(allCategories.toSet() - initialAssignedCategories.toSet())

    setupDialogView()

    val dialog = AlertDialog.Builder(activity)
      .setTitle(activity.getString(R.string.lbl_edit_categories_for_song))
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        onResult(assignedCategories.toList())
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  private fun setupDialogView() {
    dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_song_category, null)
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
    assignedCategories: SortedSet<TagEntity>,
    unAssignCategoryListener: (category: TagEntity) -> Unit
  ) {
    for (category in assignedCategories) {
      val categoryView = TagView(activity, category)
      categoryView.setOnClickListener {
        unAssignCategoryListener(category)
      }
      categoryView.deleteMode()
      assignedCategoriesFlexBox.addView(categoryView)
    }
  }

  private fun unassignedCategoriesView(
    unassignedCategoriesFlexbox: FlexboxLayout,
    unassignedCategories: SortedSet<TagEntity>,
    assignCategoryListener: (category: TagEntity) -> Unit
  ) {
    for (category in unassignedCategories) {
      val categoryView = TagView(activity, category)
      categoryView.setOnClickListener {
        assignCategoryListener(category)
      }
      categoryView.addMode()
      unassignedCategoriesFlexbox.addView(categoryView)
    }
  }
}