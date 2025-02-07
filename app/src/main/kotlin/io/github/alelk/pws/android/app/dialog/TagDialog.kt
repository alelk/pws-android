package io.github.alelk.pws.android.app.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import io.github.alelk.pws.domain.model.Color as DbColor
import com.alelk.pws.pwapp.R
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.domain.model.TagId
import kotlin.random.Random
import yuku.ambilwarna.AmbilWarnaDialog as ColorDialog

class TagDialog(private val context: Context) {

  fun showAddTagDialog(tagId: TagId, onResult: (tag: TagEntity) -> Unit) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)
    val tagNameInput = dialogView.findViewById<EditText>(R.id.category_name_input)
    val chooseColorButton = dialogView.findViewById<Button>(R.id.choose_color_button)
    val tagColorInput = dialogView.findViewById<ImageView>(R.id.icon_color_cyrcle)

    var selectedColor = getRandomColor()
    tagColorInput.setColorFilter(Color.parseColor(selectedColor.toString()))

    chooseColorButton.setOnClickListener {
      showSelectColorDialog(selectedColor) {
        selectedColor = it
        tagColorInput.setColorFilter(Color.parseColor(it.toString()))
      }
    }

    val dialog = AlertDialog.Builder(context)
      .setTitle(context.getString(R.string.lbl_add_category))
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val tagName = tagNameInput.text.toString()
        if (tagName.isNotEmpty()) {
          val tag = TagEntity(id = tagId, name = tagName, priority = 0, color = selectedColor, predefined = false)
          onResult(tag)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showEditTagDialog(tag: TagEntity, onResult: (tag: TagEntity) -> Unit) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)
    val tagNameInput = dialogView.findViewById<EditText>(R.id.category_name_input)
    val chooseColorButton = dialogView.findViewById<Button>(R.id.choose_color_button)
    val tagColorInput = dialogView.findViewById<ImageView>(R.id.icon_color_cyrcle)

    chooseColorButton.visibility = View.INVISIBLE
    tagColorInput.visibility = View.INVISIBLE
    tagNameInput.setText(tag.name)

    val dialog = AlertDialog.Builder(context)
      .setTitle(context.getString(R.string.lbl_edit_category_name))
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val tagName = tagNameInput.text.toString()
        if (tagName.isNotEmpty()) {
          val updatedTag = tag.copy(name = tagName)
          onResult(updatedTag)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showDeleteTagDialog(tag: TagEntity, onResult: (tag: TagEntity) -> Unit) {
    val dialog = AlertDialog.Builder(context)
      .setTitle(context.getString(R.string.lbl_delete_category))
      .setMessage(context.getString(R.string.msg_confirm_delete_category) + " \"${tag.name}\"?")
      .setPositiveButton(android.R.string.ok) { _, _ ->
        onResult(tag)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
    dialog.show()
  }

  fun showWarningUniqueNameDialog() {
    AlertDialog.Builder(context)
      .setTitle(context.getString(R.string.lbl_error))
      .setMessage(context.getString(R.string.msg_category_name_unique))
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  fun showSelectColorDialog(selectedColor: DbColor, onResult: (color: DbColor) -> Unit) {
    ColorDialog(context, Color.parseColor(selectedColor.toString()),
      object : ColorDialog.OnAmbilWarnaListener {
        override fun onCancel(dialog: ColorDialog) = Unit
        override fun onOk(dialog: ColorDialog, color: Int) = onResult(DbColor.parse(String.format("#%06X", 0xFFFFFF and color)))
      }).show()
  }

  private fun getRandomColor(): DbColor {
    val random = Random
    val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    return DbColor.parse(String.format("#%06X", 0xFFFFFF and color))
  }
}