package com.alelk.pws.pwapp.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.alelk.pws.pwapp.fragment.PsalmTextFragment;

import java.util.ArrayList;

/**
 * Created by Alex Elkin on 31.05.2016.
 */
public class PsalmTextFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private SparseArray<Fragment> mRegisteredFragments = new SparseArray<>();
    private ArrayList<Long> mPsalmNumberIdList;
    private float mPsalmTextSize = -1;

    public PsalmTextFragmentStatePagerAdapter(FragmentManager fm, ArrayList<Long> psalmNumberIdList, float psalmTextSize) {
        super(fm);
        mPsalmNumberIdList = psalmNumberIdList;
        mPsalmTextSize = psalmTextSize;
    }

    @Override
    public Fragment getItem(int position) {
        return PsalmTextFragment.newInstance(mPsalmNumberIdList.get(position), mPsalmTextSize);
    }

    @Override
    public int getCount() {
        return mPsalmNumberIdList != null ? mPsalmNumberIdList.size() : 0;
    }

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

    public void applyPsalmTextPreferences(float psalmTextSize) {
        mPsalmTextSize = psalmTextSize;
        for (int i = 0; i < mRegisteredFragments.size(); i++) {
            final PsalmTextFragment fragment = (PsalmTextFragment) mRegisteredFragments.valueAt(i);
            fragment.setPsalmTextSize(psalmTextSize);
        }
    }
}