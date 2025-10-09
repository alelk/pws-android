package io.github.alelk.pws.android.app.feature.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.model.SongNumberId

/**
 * Favorites Recycler View Adapter
 *
 * Created by Alex Elkin on 19.05.2016.
 */
class FavoritesRecyclerViewAdapter(
  private val onItemClickListener: (songNumberId: SongNumberId) -> Unit
) : ListAdapter<FavoriteInfo, FavoritesRecyclerViewAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_song_list_item, parent, false)
    return FavoriteViewHolder(view)
  }

  override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
    val favorite = getItem(position)
    holder.bind(favorite, onItemClickListener)
  }

  class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private var songNumberId: SongNumberId? = null

    fun bind(favorite: FavoriteInfo, onItemClickListener: (songNumberId: SongNumberId) -> Unit) {
      songNumber.text = favorite.songNumber.toString()
      songName.text = favorite.songName
      bookDisplayName.text = favorite.bookDisplayName
      songNumberId = favorite.songNumberId

      itemView.setOnClickListener {
        songNumberId?.let { onItemClickListener(it) }
      }
    }
  }

  class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteInfo>() {
    override fun areItemsTheSame(oldItem: FavoriteInfo, newItem: FavoriteInfo): Boolean =
      oldItem.songNumberId == newItem.songNumberId

    override fun areContentsTheSame(oldItem: FavoriteInfo, newItem: FavoriteInfo): Boolean =
      oldItem == newItem
  }
}