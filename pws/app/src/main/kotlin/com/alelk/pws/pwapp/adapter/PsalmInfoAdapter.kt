package com.alelk.pws.pwapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.TagSongsActivity
import com.alelk.pws.pwapp.holder.PsalmInfoHolder
import com.alelk.pws.pwapp.model.PsalmInfo

class PsalmInfoAdapter(
  private val activity: TagSongsActivity
) : RecyclerView.Adapter<PsalmInfoHolder>() {

  private var psalmInfoList: List<PsalmInfo> = emptyList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PsalmInfoHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_psalm_list_item, parent, false)
    return PsalmInfoHolder(itemView)
  }

  override fun onBindViewHolder(holder: PsalmInfoHolder, position: Int) {
    val psalmInfo = psalmInfoList[position]
    holder.bind(psalmInfo)
    holder.psalmItem.setOnClickListener { activity.onPsalmSelected(holder.data) }
  }

  override fun getItemCount(): Int {
    return psalmInfoList.size
  }

  fun swapData(psalmInfoList: List<PsalmInfo>) {
    this.psalmInfoList = psalmInfoList
    notifyDataSetChanged()
  }
}