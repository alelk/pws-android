package io.github.alelk.pws.android.app.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.adapter.SongsInfoAdapter
import io.github.alelk.pws.android.app.model.TagsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookWithFavorite
import io.github.alelk.pws.domain.model.TagId

@AndroidEntryPoint
class TagSongsActivity : AppCompatThemedActivity() {
  private lateinit var recyclerView: RecyclerView
  private lateinit var songsInfoAdapter: SongsInfoAdapter
  private val tagsViewModel: TagsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_category_songs)

    val tagId = checkNotNull(intent.getStringExtra(TAG_ID)) { "$TAG_ID required" }
    val tagName = intent.getStringExtra(TAG_NAME) ?: ""

    title = "${getString(R.string.title_activity_category_songs)} $tagName"

    songsInfoAdapter = SongsInfoAdapter { songInfo -> onSongSelected(songInfo) }

    recyclerView = findViewById<RecyclerView>(R.id.category_songs_recycler_view).apply {
      adapter = songsInfoAdapter
      layoutManager = LinearLayoutManager(this@TagSongsActivity)
    }

    tagsViewModel.getTagSongs(TagId.parse(tagId)).asLiveData().observe(this) { songInfoList ->
      songsInfoAdapter.swapData(songInfoList)
    }
  }

  private fun onSongSelected(data: SongNumberWithSongWithBookWithFavorite) {
    val intentSongView = Intent(this, SongActivity::class.java).apply {
      putExtra(SongActivity.KEY_SONG_NUMBER_ID, data.songNumberId)
    }
    startActivity(intentSongView)
  }

  companion object {
    const val TAG_NAME = "tag_name"
    const val TAG_ID = "tag_id"
  }
}