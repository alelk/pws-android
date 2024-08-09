package com.alelk.pws.pwapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.CategoriesActivity
import com.alelk.pws.pwapp.holder.CategoryHolder
import com.alelk.pws.pwapp.model.Category

class CategoriesAdapter(
  private val activity: CategoriesActivity
) : RecyclerView.Adapter<CategoryHolder>() {

  private lateinit var categories: List<Category>
  private var isEditMode = false

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.song_category_item, parent, false)
    return CategoryHolder(itemView)
  }

  override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
    val category = categories[position]
    holder.bind(category, isEditMode)

    holder.itemView.setOnClickListener { activity.onCategorySelect(holder.data)}
    holder.editButton.setOnClickListener { activity.editCategory(holder.data) }
    holder.deleteButton.setOnClickListener { activity.deleteCategory(holder.data) }
    holder.colorIcon.setOnClickListener { activity.editCategoryColor(holder.data) }
  }

  override fun getItemCount(): Int {
    return categories.size
  }

  fun swapData(categories: List<Category>) {
    this.categories = categories
    notifyDataSetChanged()
  }

  fun switchEditMode(isEditMode: Boolean) {
    this.isEditMode = isEditMode
    notifyDataSetChanged()
  }

}