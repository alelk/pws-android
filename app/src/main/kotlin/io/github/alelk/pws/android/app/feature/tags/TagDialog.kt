package io.github.alelk.pws.android.app.feature.tags

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.domain.core.ids.TagId
import yuku.ambilwarna.AmbilWarnaDialog
import kotlin.random.Random

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
          val tag = TagEntity(
              id = tagId,
              name = tagName,
              priority = 0,
              color = selectedColor,
              predefined = false
          )
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

  fun showSelectColorDialog(selectedColor: io.github.alelk.pws.domain.core.Color, onResult: (color: io.github.alelk.pws.domain.core.Color) -> Unit) {
    AmbilWarnaDialog(
        context, Color.parseColor(selectedColor.toString()),
        object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) = Unit
            override fun onOk(dialog: AmbilWarnaDialog, color: Int) = onResult(
                io.github.alelk.pws.domain.core.Color.Companion.parse(
                    String.format(
                        "#%06X",
                        0xFFFFFF and color
                    )
                )
            )
        }).show()
  }

  private fun getRandomColor(): io.github.alelk.pws.domain.core.Color {
    val random = Random.Default
    val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    return io.github.alelk.pws.domain.core.Color.Companion.parse(String.format("#%06X", 0xFFFFFF and color))
  }
}