package com.alelk.pws.pwapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.dao.SongInfo
import com.alelk.pws.pwapp.R

class SongsInfoAdapter(
  private val onSongSelected: (SongInfo) -> Unit
) : RecyclerView.Adapter<SongsInfoAdapter.SongInfoHolder>() {

  private var songInfoList: List<SongInfo> = emptyList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongInfoHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_psalm_list_item, parent, false)
    return SongInfoHolder(itemView)
  }

  override fun onBindViewHolder(holder: SongInfoHolder, position: Int) {
    val psalmInfo = songInfoList[position]
    holder.bind(psalmInfo)

    holder.itemView.setOnClickListener {
      onSongSelected(psalmInfo)
    }
  }

  override fun getItemCount(): Int = songInfoList.size

  fun swapData(newSongInfoList: List<SongInfo>) {
    songInfoList = newSongInfoList
    notifyDataSetChanged()
  }

  class SongInfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val psalmName: TextView = itemView.findViewById(R.id.txt_psalm_name)
    private val psalmNumber: TextView = itemView.findViewById(R.id.txt_psalm_number)
    private val bookName: TextView = itemView.findViewById(R.id.txt_book_name)

    fun bind(songInfo: SongInfo) {
      psalmName.text = songInfo.songName
      psalmNumber.text = songInfo.songNumber.toString()
      bookName.text = songInfo.bookDisplayName
    }
  }
}