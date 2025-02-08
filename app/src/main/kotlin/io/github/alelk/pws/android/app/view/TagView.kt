package io.github.alelk.pws.android.app.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import io.github.alelk.pws.database.entity.TagEntity

class TagView(
  context: Context,
  private val tag: TagEntity,
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

    textView.text = tag.name

    val backgroundColor = Color.parseColor(tag.color.toString())
    val textColor = getAdaptiveTextColorForBackground(backgroundColor)
    setBackgroundColor(backgroundColor)
    setTextColor(textColor)
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

  private fun getAdaptiveTextColorForBackground(backgroundColor: Int): Int {
    val darkness = 1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255
    return if (darkness < 0.5) {
      // Light background, use dark text
      Color.BLACK
    } else {
      // Dark background, use light text
      Color.WHITE
    }
  }
}