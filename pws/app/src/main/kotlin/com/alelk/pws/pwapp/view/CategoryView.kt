package com.alelk.pws.pwapp.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.alelk.pws.pwapp.model.Category
import com.google.android.flexbox.FlexboxLayout

class CategoryView(
  context: Context,
  private val category: Category,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

  companion object {
    private const val DELETE_ICON = "   \u2716"
  }

  private val textView: TextView

  init {
    // Set card view properties
    cardElevation = 4f
    radius = 8f
    setContentPadding(16, 8, 16, 8)
    setBackgroundColor(Color.parseColor(category.color))

    textView = TextView(context)
    textView.layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    textView.gravity = Gravity.CENTER
    addView(textView)

    // Set layout params with margins
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
      setMargins(8, 8, 8, 8) // Add left and right margins of 8dp
    }

    textView.text = category.name
  }

  override fun setBackgroundColor(color: Int) {
    setCardBackgroundColor(color)
  }

  fun setTextColor(color: Int) {
    textView.setTextColor(color)
  }

  fun deleteMode() {
    textView.text = "${textView.text}$DELETE_ICON"
  }

  fun addMode() {
    textView.text = "${textView.text}"
  }
}