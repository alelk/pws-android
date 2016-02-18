package com.alelk.pws.pwapp.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.pwapp.MainActivity;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.PsalmListAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Alex Elkin on 17.02.2016.
 */
public class ReadNowFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_readnow, null);
        final ListView lvPsalmList = (ListView) v.findViewById(R.id.lview_readnow);

        PwsDataSource pwsDataSource = new PwsDataSourceImpl(getActivity().getBaseContext(), "pws.db", 9);
        pwsDataSource.open();
        List<Psalm> psalms = new ArrayList<>();
        try {
            Map<Integer, Psalm> psalms1 = pwsDataSource.getPsalms(BookEdition.PV3055);
            if (psalms1 != null) {
                psalms.addAll(psalms1.values());
            }
        } catch (PwsDatabaseIncorrectValueException e) {
            e.printStackTrace();
        }
        pwsDataSource.close();

        Collections.sort(psalms, Psalm.getNumberComparator(BookEdition.PV3055));

        PsalmListAdapter psalmListAdapter = new PsalmListAdapter(getActivity().getBaseContext(), R.layout.layout_psalms_list, psalms);
        lvPsalmList.setAdapter(psalmListAdapter);
        lvPsalmList.setOnItemClickListener(psalmListClickHandler);
        return v;
    }


    private AdapterView.OnItemClickListener psalmListClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Psalm psalm = (Psalm) parent.getItemAtPosition(position);

            Intent intent = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
            intent.putExtra("psalm", new PwsPsalmParcelable(psalm));
            intent.putExtra("bookEdition", BookEdition.PV3055.getSignature());
            startActivity(intent);
        }
    };
}
