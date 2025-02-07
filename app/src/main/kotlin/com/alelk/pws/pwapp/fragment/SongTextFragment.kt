/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alelk.pws.pwapp.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.alelk.pws.database.data.Tonality.Companion.getInstanceBySignature
import com.alelk.pws.database.util.PwsSongUtil
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.SongActivity
import com.alelk.pws.pwapp.adapter.SongReferencesRecyclerViewAdapter
import com.alelk.pws.pwapp.adapter.SongTextFragmentPagerAdapter
import com.alelk.pws.pwapp.model.AppPreferencesViewModel
import com.alelk.pws.pwapp.model.SongViewModel
import com.alelk.pws.pwapp.model.textDocument
import com.alelk.pws.pwapp.model.textDocumentHtml
import com.alelk.pws.pwapp.view.TagView
import com.google.android.flexbox.FlexboxLayout
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Created by Alex Elkin on 18.04.2015.
 */
@AndroidEntryPoint
class SongTextFragment : Fragment() {

  private val songViewModel: SongViewModel by viewModels()
  private val appPreferencesViewModel: AppPreferencesViewModel by viewModels()

  private lateinit var songText: TextView
  private lateinit var songInfo: TextView
  private lateinit var tonalities: TextView
  private lateinit var songTagsCard: CardView
  private lateinit var songTags: FlexboxLayout

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    songViewModel.setSongNumberId(arguments?.getLong(KEY_SONG_NUMBER_ID))
    return inflater.inflate(R.layout.fragment_song_text, container, false)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    songText = view.findViewById(R.id.txt_song_text)
    songInfo = view.findViewById(R.id.txt_song_info)
    tonalities = view.findViewById(R.id.txt_song_tonalities)
    songTagsCard = view.findViewById(R.id.cv_categories)
    songTags = view.findViewById(R.id.categories)

    val songReferencesAdapter = SongReferencesRecyclerViewAdapter(-1.0f) { songNumberId: Long ->
      val intent =
        Intent(requireActivity().baseContext, SongActivity::class.java)
          .apply { putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId) }
      startActivity(intent)
    }
    val songReferencesView = view.findViewById<RecyclerView>(R.id.rv_referred_songs).apply {
      layoutManager = LinearLayoutManager(requireContext())
      isNestedScrollingEnabled = false
      adapter = songReferencesAdapter
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          songViewModel.song.filterNotNull()
            .flatMapLatest { songInfo -> appPreferencesViewModel.songTextExpanded.mapLatest { songInfo.song to it } }
            .distinctUntilChanged()
            .collectLatest { (song, expanded) -> updateUi(song, expanded) }
        }
        launch {
          songViewModel.tags.filterNotNull().collectLatest { updateUi(it) }
        }
        launch {
          songViewModel.references.filterNotNull().collectLatest { refs ->
            if (refs.isEmpty()) songReferencesView.visibility = View.GONE
            else {
              songReferencesView.visibility = View.VISIBLE
              songReferencesAdapter.submitList(refs)
            }
          }
        }
        launch {
          appPreferencesViewModel.songTextSize.filterNotNull().collectLatest(::applySongTextSize)
        }
      }
    }

    registerForContextMenu(songText)
  }

  /** Update ui for song info */
  private fun updateUi(song: SongEntity, isTextExpanded: Boolean) = kotlin.runCatching {
    Timber.d("update ui for song text: songId=${song.id}")
    val songTextHtml = PwsSongUtil.songTextToHtml(song.locale, song.lyric, isTextExpanded)
    songText.text =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml(songTextHtml, Html.FROM_HTML_MODE_COMPACT)
      else Html.fromHtml(songTextHtml)

    val songInfoHtml = PwsSongUtil.buildSongInfoHtml(song.locale, song.author?.name, song.translator?.name, song.composer?.name)
    if (songInfoHtml == null)
      this.songInfo.visibility = View.GONE
    else
      this.songInfo.text =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml(songInfoHtml, Html.FROM_HTML_MODE_COMPACT)
        else Html.fromHtml(songInfoHtml)

    val tonalities = song.tonalities?.joinToString(", ") { tonality ->
      getInstanceBySignature(tonality.identifier)?.getLabel(requireActivity()) ?: ""
    }
    this.tonalities.text = tonalities ?: getString(R.string.tonality_not_defined)
  }.onFailure { e -> Timber.e(e, "error updating ui from song #${song.id} info") }

  /** Update ui for song tags */
  private fun updateUi(tags: List<TagEntity>) = kotlin.runCatching {
    Timber.d("updating ui for song tags: ${tags.joinToString(", ") { it.id.toString() }}")
    songTags.removeAllViews()
    if (tags.isEmpty()) {
      songTagsCard.visibility = View.GONE
    } else {
      songTagsCard.visibility = View.VISIBLE
      for (tag in tags) {
        songTags.addView(TagView(requireActivity(), tag).apply { addMode() })
      }
      // todo: setup on-click listener for editing categories
    }
  }.onFailure { e -> Timber.e(e, "error updating ui for song tags: ${tags.joinToString(", ") { it.id.toString() }}") }

  private fun applySongTextSize(textSize: Float) {
    if (textSize >= 10 || textSize <= 100) {
      songText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
      tonalities.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize / 1.5f)
      songInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize / 1.5f)
    }
  }

  private var historyJob = MutableStateFlow<Job?>(null)

  override fun onResume() {
    super.onResume()
    historyJob.value?.cancel()
    historyJob.value = viewLifecycleOwner.lifecycleScope.launch {
      delay(ADD_TO_HISTORY_DELAY)
      songViewModel.addToHistory()
    }
  }

  override fun onPause() {
    super.onPause()
    historyJob.value?.cancel()
  }

  override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
    requireActivity().menuInflater.inflate(R.menu.menu_song_text_context, menu)
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    if (R.id.menu_copy == item.itemId) {
      val viewPager = requireActivity().findViewById<ViewPager2>(R.id.pager_song_text)
      val adapter = viewPager.adapter as? SongTextFragmentPagerAdapter
      val currentSongNumberId = adapter?.allSongNumberIds?.getOrNull(viewPager.currentItem)
      
      if (currentSongNumberId != arguments?.getLong(KEY_SONG_NUMBER_ID)) {
        Timber.v("Ignoring context menu in non-visible fragment (songId=${arguments?.getLong(KEY_SONG_NUMBER_ID)})")
        return false
      }

      val clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
      val song = songViewModel.song.value ?: return false
      Timber.d("copying song# ${song.songNumber.number} from book ${song.book.externalId} with name ${song.song.name}")
      val clip = ClipData.newHtmlText(getString(R.string.app_name), song.textDocument, song.textDocumentHtml)
      clipboardManager.setPrimaryClip(clip)
      return true
    }
    return super.onContextItemSelected(item)
  }

  companion object {
    const val KEY_SONG_NUMBER_ID = "songNumberId"
    private val ADD_TO_HISTORY_DELAY = 7.seconds

    fun newInstance(songNumberId: Long): SongTextFragment {
      return SongTextFragment().apply {
        arguments = Bundle().apply {
          putLong(KEY_SONG_NUMBER_ID, songNumberId)
        }
      }
    }
  }
}