package io.github.alelk.pws.android.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookWithFavorite

class SongsInfoAdapter(
  private val onSongSelected: (SongNumberWithSongWithBookWithFavorite) -> Unit
) : RecyclerView.Adapter<SongsInfoAdapter.SongInfoHolder>() {

  private var songInfoList: List<SongNumberWithSongWithBookWithFavorite> = emptyList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongInfoHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_song_list_item, parent, false)
    return SongInfoHolder(itemView)
  }

  override fun onBindViewHolder(holder: SongInfoHolder, position: Int) {
    val songInfo = songInfoList[position]
    holder.bind(songInfo)

    holder.itemView.setOnClickListener {
      onSongSelected(songInfo)
    }
  }

  override fun getItemCount(): Int = songInfoList.size

  fun swapData(newSongInfoList: List<SongNumberWithSongWithBookWithFavorite>) {
    songInfoList = newSongInfoList
    notifyDataSetChanged()
  }

  class SongInfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookName: TextView = itemView.findViewById(R.id.txt_book_name)

    fun bind(songInfo: SongNumberWithSongWithBookWithFavorite) {
      songName.text = songInfo.song.name
      songNumber.text = songInfo.songNumber.number.toString()
      bookName.text = songInfo.book.displayName
    }
  }
}