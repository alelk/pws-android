package io.github.alelk.pws.android.app.feature.songs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.alelk.pws.android.app.feature.songs.SongTextFragment
import io.github.alelk.pws.domain.core.ids.SongNumberId

/**
 * Song Text Fragment State Pager Adapter
 *
 * Created by Alex Elkin on 31.05.2016.
 */
class SongTextFragmentPagerAdapter(
    fragmentActivity: FragmentActivity,
    var allSongNumberIds: List<SongNumberId>
) : FragmentStateAdapter(fragmentActivity) {

  override fun getItemCount(): Int = allSongNumberIds.size
  override fun createFragment(position: Int): Fragment = SongTextFragment.Companion.newInstance(allSongNumberIds[position])
}