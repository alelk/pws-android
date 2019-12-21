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

package com.alelk.pws.pwapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.alelk.pws.pwapp.fragment.PsalmTextFragment;
import com.alelk.pws.pwapp.preference.PsalmPreferences;

import java.util.ArrayList;

/**
 * Psalm Text Fragment State Pager Adapter
 *
 * Created by Alex Elkin on 31.05.2016.
 */
public class PsalmTextFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();
    private ArrayList<Long> mPsalmNumberIdList;
    private PsalmPreferences mPsalmPreferences;

    public PsalmTextFragmentStatePagerAdapter(FragmentManager fm, ArrayList<Long> psalmNumberIdList, PsalmPreferences psalmPreferences) {
        super(fm);
        mPsalmNumberIdList = psalmNumberIdList;
        mPsalmPreferences = psalmPreferences;
    }

    @Override
    public Fragment getItem(int position) {
        return PsalmTextFragment.newInstance(mPsalmNumberIdList.get(position), mPsalmPreferences);
    }

    @Override
    public int getCount() {
        return mPsalmNumberIdList != null ? mPsalmNumberIdList.size() : 0;
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mRegisteredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mRegisteredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public SparseArray<Fragment> getRegisteredFragments() {
        return mRegisteredFragments;
    }

    public void applyPsalmPreferences(PsalmPreferences preferences) {
        mPsalmPreferences = preferences;
        for (int i = 0; i < mRegisteredFragments.size(); i++) {
            final PsalmTextFragment fragment = (PsalmTextFragment) mRegisteredFragments.valueAt(i);
            fragment.applyPsalmPreferences(mPsalmPreferences);
        }
    }
}
