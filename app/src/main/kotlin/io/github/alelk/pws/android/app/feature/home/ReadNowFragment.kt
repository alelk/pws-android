package io.github.alelk.pws.android.app.feature.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.history.HistoryRecyclerViewAdapter
import io.github.alelk.pws.android.app.feature.history.HistoryViewModel
import io.github.alelk.pws.android.app.feature.songs.SongActivity
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Read Now Fragment
 *
 * Created by Alex Elkin on 17.02.2016.
 */
@AndroidEntryPoint
class ReadNowFragment @Inject constructor() : Fragment(R.layout.fragment_readnow) {

  private val historyViewModel: HistoryViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val historyAdapter = HistoryRecyclerViewAdapter { songNumberId: SongNumberId ->
      val intentSongView = Intent(activity, SongActivity::class.java)
      intentSongView.putExtra(SongActivity.Companion.KEY_SONG_NUMBER_ID, songNumberId.identifier)
      startActivity(intentSongView)
    }

    view.findViewById<RecyclerView>(R.id.rv_recent).apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = historyAdapter

    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        historyViewModel.historyItems.collectLatest { historyItems ->
          historyAdapter.submitList(historyItems.take(DEFAULT_RECENT_LIMIT))
        }
      }
    }

    setHasOptionsMenu(true)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val menuClearHistory = menu.add(R.string.menu_clear_history)
    menuClearHistory.setIcon(R.drawable.ic_delete_black_24dp)
    menuClearHistory.setOnMenuItemClickListener {
      showClearHistoryConfirmationDialog()
      true
    }
    return super.onCreateOptionsMenu(menu, inflater)
  }

  private fun showClearHistoryConfirmationDialog() {
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.clear_history_title)
      .setMessage(R.string.clear_history_message)
      .setPositiveButton(R.string.clear) { _, _ ->
        viewLifecycleOwner.lifecycleScope.launch {
          historyViewModel.clearHistory()
        }
      }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  companion object {
    private const val DEFAULT_RECENT_LIMIT = 10
    private const val MENU_CLEAR_HISTORY = Menu.FIRST
  }
}