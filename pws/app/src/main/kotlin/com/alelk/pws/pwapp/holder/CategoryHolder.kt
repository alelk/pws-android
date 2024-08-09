package com.alelk.pws.pwapp.holder

import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.model.Category

class CategoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView), PwsHolder {
  lateinit var data: Category
  private val categoryText: TextView = itemView.findViewById(R.id.category_text_view)
  val colorIcon: ImageView = itemView.findViewById(R.id.category_color_icon)
  val editButton: ImageButton = itemView.findViewById(R.id.edit_button)
  val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

  fun bind(data: Category, isEditMode: Boolean) {
    this.data = data
    if (isEditMode && data.predefined) {
      itemView.visibility = View.INVISIBLE
      return
    }
    itemView.visibility = View.VISIBLE

    categoryText.text = data.name
    colorIcon.setColorFilter(Color.parseColor(data.color))

    editButton.visibility = if (isEditMode) View.VISIBLE else View.INVISIBLE
    deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
    colorIcon.visibility = if (isEditMode) View.GONE else View.VISIBLE
  }

}