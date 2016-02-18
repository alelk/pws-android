package com.alelk.pws.pwapp.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 18.02.2016.
 */
public class FavoritesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_favorite, null);
        return v;
    }
}
