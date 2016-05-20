package com.alelk.pws.pwapp.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alelk.pws.pwapp.R;


/**
 * Created by Alex Elkin on 17.02.2016.
 */
public class ReadNowFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_readnow, null);
        final ListView lvPsalmList = (ListView) v.findViewById(R.id.lview_readnow);
        lvPsalmList.setOnItemClickListener(psalmListClickHandler);
        return v;
    }


    private AdapterView.OnItemClickListener psalmListClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    };
}
