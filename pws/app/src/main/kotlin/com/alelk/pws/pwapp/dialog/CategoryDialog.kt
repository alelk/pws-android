package com.alelk.pws.pwapp.dialog

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.CategoriesActivity
import com.alelk.pws.pwapp.model.Category
import kotlin.random.Random
import yuku.ambilwarna.AmbilWarnaDialog as ColorDialog

class CategoryDialog(private val activity: CategoriesActivity) {

  fun showAddCategoryDialog(onResult: (category: Category) -> Unit) {
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_category, null)
    val categoryNameInput = dialogView.findViewById<EditText>(R.id.category_name_input)
    val chooseColorButton = dialogView.findViewById<Button>(R.id.choose_color_button)
    val categoryColorInput = dialogView.findViewById<ImageView>(R.id.icon_color_cyrcle)

    var selectedColor = getRandomColor()
    categoryColorInput.setColorFilter(Color.parseColor(selectedColor))

    chooseColorButton.setOnClickListener {
      showSelectColorDialog(selectedColor) {
        selectedColor = it
        categoryColorInput.setColorFilter(Color.parseColor(it))
      }
    }

    val dialog = AlertDialog.Builder(activity)
      .setTitle("Add category")
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { dialog, _ ->
        val categoryName = categoryNameInput.text.toString()
        if (categoryName.isNotEmpty()) {
          val category = Category(-1L, categoryName, selectedColor)
          onResult(category)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showEditCategoryDialog(category: Category, onResult: (category: Category) -> Unit) {
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_category, null)
    val categoryNameInput = dialogView.findViewById<EditText>(R.id.category_name_input)
    val chooseColorButton = dialogView.findViewById<Button>(R.id.choose_color_button)
    val categoryColorInput = dialogView.findViewById<ImageView>(R.id.icon_color_cyrcle)

    chooseColorButton.visibility = View.INVISIBLE
    categoryColorInput.visibility = View.INVISIBLE
    categoryNameInput.setText(category.name)

    val dialog = AlertDialog.Builder(activity)
      .setTitle("Edit category name")
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { dialog, _ ->
        val categoryName = categoryNameInput.text.toString()
        if (categoryName.isNotEmpty()) {
          val updatedCategory = Category(category.id, categoryName, category.color)
          onResult(updatedCategory)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showDeleteCategoryDialog(category: Category, onResult: (category: Category) -> Unit) {
    val dialog = AlertDialog.Builder(activity)
      .setTitle("Delete category")
      .setMessage("Are you sure you want to delete the category \"${category.name}\"?")
      .setPositiveButton(android.R.string.ok) { dialog, _ ->
        onResult(category)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showWarningUniqueNameDialog() {
    AlertDialog.Builder(activity)
      .setTitle("Error")
      .setMessage("Category name must be unique")
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  fun showSelectColorDialog(selectedColor: String, onResult: (color: String) -> Unit) {
    ColorDialog(activity, Color.parseColor(selectedColor),
      object : ColorDialog.OnAmbilWarnaListener {
        override fun onCancel(dialog: ColorDialog) {
          // No action needed
        }

        override fun onOk(dialog: ColorDialog, color: Int) {
          onResult(String.format("#%06X", 0xFFFFFF and color))
        }
      }).show()
  }


  private fun getRandomColor(): String {
    val random = Random
    val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    return String.format("#%06X", 0xFFFFFF and color)
  }
}