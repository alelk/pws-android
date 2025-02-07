package io.github.alelk.pws.android.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.common.entity.TagEntity

class TagsAdapter(
  private val onTagSelect: (TagEntity) -> Unit,
  private val onEditTag: (TagEntity) -> Unit,
  private val onEditTagColor: (TagEntity) -> Unit,
  private val onDeleteTag: (TagEntity) -> Unit
) : RecyclerView.Adapter<TagsAdapter.TagHolder>() {

  private var tags: List<TagEntity> = emptyList()
  private var isEditMode = false

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.song_category_item, parent, false)
    return TagHolder(itemView)
  }

  override fun onBindViewHolder(holder: TagHolder, position: Int) {
    val tag = tags[position]
    holder.bind(tag, isEditMode)

    holder.itemView.setOnClickListener { onTagSelect(tag) }
    holder.editButton.setOnClickListener { onEditTag(tag) }
    holder.deleteButton.setOnClickListener { onDeleteTag(tag) }
    holder.colorIcon.setOnClickListener { onEditTagColor(tag) }
  }

  override fun getItemCount(): Int {
    return tags.size
  }

  fun swapData(newTags: List<TagEntity>) {
    tags = newTags
    notifyDataSetChanged()
  }

  fun switchEditMode(editMode: Boolean) {
    isEditMode = editMode
    notifyDataSetChanged()
  }

  class TagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val categoryText: TextView = itemView.findViewById(R.id.category_text_view)
    val colorIcon: ImageView = itemView.findViewById(R.id.category_color_icon)
    val editButton: ImageButton = itemView.findViewById(R.id.edit_button)
    val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

    lateinit var data: TagEntity

    fun bind(tag: TagEntity, isEditMode: Boolean) {
      data = tag
      if (isEditMode && data.predefined) {
        itemView.visibility = View.INVISIBLE
        return
      }
      itemView.visibility = View.VISIBLE
      categoryText.text = data.name
      colorIcon.setColorFilter(Color.parseColor(data.color.toString()))
      editButton.visibility = if (isEditMode) View.VISIBLE else View.INVISIBLE
      deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
      colorIcon.visibility = if (isEditMode) View.GONE else View.VISIBLE
    }
  }
}