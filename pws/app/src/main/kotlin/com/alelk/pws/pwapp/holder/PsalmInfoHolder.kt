package com.alelk.pws.pwapp.holder

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.model.PsalmInfo

class PsalmInfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView), PwsHolder {
  lateinit var data: PsalmInfo
  val psalmItem: CardView = itemView.findViewById(R.id.cv_psalm_item)
  private val psalmName = itemView.findViewById<TextView>(R.id.txt_psalm_name)
  private val psalmNumber = itemView.findViewById<TextView>(R.id.txt_psalm_number)
  private val bookName = itemView.findViewById<TextView>(R.id.txt_book_name)

  fun bind(data: PsalmInfo) {
    this.data = data
    bookName.text = data.bookName
    psalmNumber.text = data.psalmNumber.toString()
    psalmName.text = data.psalmName
  }

}