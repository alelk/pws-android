package com.alelk.pws.pwapp.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.PsalmPartsAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;
import com.alelk.pws.pwapp.util.PwsUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
        final View psalmHeaderView = inflater.inflate(R.layout.layout_psalmheader, null);
        final ListView listView = (ListView) v.findViewById(R.id.lview_psalmparts);
        final TextView txtPsalmNumber = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmnumber);
        final TextView txtBookEdition = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_bookedition);
        final TextView txtPsalmName = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmname);

        PsalmActivity psalmActivity = (PsalmActivity) getActivity();
        PwsPsalmParcelable psalmParcelable = psalmActivity.getIntent().getParcelableExtra("psalm");
        BookEdition bookEdition = BookEdition.valueOf(psalmActivity.getIntent().getStringExtra("bookEdition"));
        SortedMap<Integer, PsalmPart> psalmParts = psalmParcelable.getPsalmParts();
        List<PsalmPart> lPsalmParts = new ArrayList<>();
        lPsalmParts.addAll(psalmParts.values());
        PsalmPartsAdapter psalmPartsArrayAdapter = new PsalmPartsAdapter(psalmActivity.getBaseContext(), lPsalmParts);

        txtPsalmName.setText(psalmParcelable.getName());
        if (bookEdition != null) {
            txtBookEdition.setText(bookEdition.getSignature());
        }

        HashMap<BookEdition, Integer> pslmNumbers = PwsUtils.parsePsalmNumbers(psalmActivity.getIntent().getStringExtra("psalmNumbers"));

        if (pslmNumbers != null) {
            txtPsalmNumber.setText(pslmNumbers.get(bookEdition));
        }

        listView.addHeaderView(psalmHeaderView);
        listView.setAdapter(psalmPartsArrayAdapter);
        return v;
    }
}
