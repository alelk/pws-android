package com.alelk.pws.pwapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.dao.SongInfo
import com.alelk.pws.database.model.TagId
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.SongsInfoAdapter
import com.alelk.pws.pwapp.model.TagViewModel

class TagSongsActivity : AppCompatThemedActivity() {
  private lateinit var recyclerView: RecyclerView
  private lateinit var songsInfoAdapter: SongsInfoAdapter
  private val tagViewModel: TagViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_category_psalms)

    val tagId = checkNotNull(intent.getStringExtra(TAG_ID)) { "$TAG_ID required" }
    val tagName = intent.getStringExtra(TAG_NAME) ?: ""

    title = "${getString(R.string.title_activity_category_psalms)} $tagName"

    songsInfoAdapter = SongsInfoAdapter { songInfo -> onSongSelected(songInfo) }

    recyclerView = findViewById<RecyclerView>(R.id.category_psalms_recycler_view).apply {
      adapter = songsInfoAdapter
      layoutManager = LinearLayoutManager(this@TagSongsActivity)
    }

    tagViewModel.getTagSongs(TagId.parse(tagId)).asLiveData().observe(this) { songInfoList ->
      songsInfoAdapter.swapData(songInfoList)
    }
  }

  private fun onSongSelected(data: SongInfo) {
    val intentSongView = Intent(this, PsalmActivity::class.java).apply {
      putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, data.songNumberId)
    }
    startActivity(intentSongView)
  }

  companion object {
    const val TAG_NAME = "tag_name"
    const val TAG_ID = "tag_id"
  }
}