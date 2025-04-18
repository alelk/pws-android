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
package io.github.alelk.pws.android.app.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.alelk.pws.android.app.fragment.SongTextFragment
import io.github.alelk.pws.domain.model.SongNumberId

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
  override fun createFragment(position: Int): Fragment = SongTextFragment.newInstance(allSongNumberIds[position])
}