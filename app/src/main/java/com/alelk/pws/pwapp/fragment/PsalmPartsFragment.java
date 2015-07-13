package com.alelk.pws.pwapp.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.PsalmPartsAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Created by Alex Elkin on 13.07.2015.
 */
public class PsalmPartsFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_psalmparts, null);
        final ListView listView = (ListView) v.findViewById(R.id.lview_psalmparts);

        PsalmActivity psalmActivity = (PsalmActivity) getActivity();
        PwsPsalmParcelable psalmParcelable = psalmActivity.getIntent().getParcelableExtra("psalm");
        SortedMap<Integer, PsalmPart> psalmParts = psalmParcelable.getPsalmParts();
        List<PsalmPart> lPsalmParts = new ArrayList<>();
        lPsalmParts.addAll(psalmParts.values());
        PsalmPartsAdapter psalmPartsArrayAdapter = new PsalmPartsAdapter(psalmActivity.getBaseContext(), lPsalmParts);
        listView.setAdapter(psalmPartsArrayAdapter);
        return v;
    }
}
