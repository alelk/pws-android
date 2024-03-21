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
package com.alelk.pws.pwapp.adapter

import com.alelk.pws.pwapp.preference.PsalmPreferences
import androidx.fragment.app.FragmentStatePagerAdapter
import android.util.SparseArray
import com.alelk.pws.pwapp.fragment.PsalmTextFragment
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.util.ArrayList

/**
 * Psalm Text Fragment State Pager Adapter
 *
 * Created by Alex Elkin on 31.05.2016.
 */
class PsalmTextFragmentStatePagerAdapter(
    fm: FragmentManager?,
    private val mPsalmNumberIdList: ArrayList<Long>?,
    private var mPsalmPreferences: PsalmPreferences
) : FragmentStatePagerAdapter(
    fm!!
) {
    val registeredFragments = SparseArray<Fragment>()
    override fun getItem(position: Int): Fragment {
        return PsalmTextFragment.newInstance(mPsalmNumberIdList!![position], mPsalmPreferences)
    }

    override fun getCount(): Int {
        return mPsalmNumberIdList?.size ?: 0
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun applyPsalmPreferences(preferences: PsalmPreferences) {
        mPsalmPreferences = preferences
        for (i in 0 until registeredFragments.size()) {
            val fragment = registeredFragments.valueAt(i) as PsalmTextFragment
            fragment.applyPsalmPreferences(mPsalmPreferences)
        }
    }
}