package com.alelk.pws.pwapp.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmPartType;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.PsalmPartsAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;
import com.alelk.pws.pwapp.util.PwsUtils;

import org.w3c.dom.Text;

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
        final View psalmFooterView = inflater.inflate(R.layout.layout_psalmfooter, null);
        final ListView listView = (ListView) v.findViewById(R.id.lview_psalmparts);
        final TextView txtPsalmNumber = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmnumber);
        final TextView txtBookEdition = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_bookedition);
        final TextView txtPsalmName = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmname);
        final TextView txtPsalmAnnotation = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmannotation);
        final TextView txtPsalmTonalities = (TextView) psalmHeaderView.findViewById(R.id.txt_psalmheader_psalmtonalities);
        final TextView txtPsalmInfo = (TextView) psalmFooterView.findViewById(R.id.txt_psalmfooter_psalminfo);

        PsalmActivity psalmActivity = (PsalmActivity) getActivity();
        PwsPsalmParcelable psalmParcelable = psalmActivity.getIntent().getParcelableExtra("psalm");
        BookEdition bookEdition = BookEdition.getInstanceBySignature(psalmActivity.getIntent().getStringExtra("bookEdition"));
        SortedMap<Integer, PsalmPart> psalmParts = psalmParcelable.getPsalmParts();
        SortedMap<BookEdition, Integer> psalmNumbers = psalmParcelable.getNumbers();
        List<PsalmPart> lPsalmParts = new ArrayList<>();
        List<Integer> lDisplayPsalmPartNumbers = new ArrayList<>();
        int lastVerseNumber = 0;
        int lastChorusNumber = 0;
        for (PsalmPart psalmPart : psalmParts.values()) {
            if (psalmPart.getPsalmType() == PsalmPartType.VERSE) {
                lPsalmParts.add(psalmPart);
                lDisplayPsalmPartNumbers.add(++lastVerseNumber);
            } else if (psalmPart.getPsalmType() == PsalmPartType.CHORUS) {
                if (!lPsalmParts.contains(psalmPart)) lastChorusNumber ++;
                lPsalmParts.add(psalmPart);
                lDisplayPsalmPartNumbers.add(lastChorusNumber);
            }
        }
        PsalmPartsAdapter psalmPartsArrayAdapter = new PsalmPartsAdapter(psalmActivity.getBaseContext(), lPsalmParts, lDisplayPsalmPartNumbers);

        txtPsalmName.setText(psalmParcelable.getName());
        txtBookEdition.setText(psalmActivity.getIntent().getStringExtra("bookName"));
        if (psalmNumbers != null) {
            if (bookEdition == null) bookEdition = psalmNumbers.firstKey();
            txtPsalmNumber.setText("" + psalmNumbers.get(bookEdition));
        }
        if (psalmParcelable.getAnnotation() != null) {
            txtPsalmAnnotation.setText(psalmParcelable.getAnnotation());
        } else txtPsalmAnnotation.setVisibility(View.GONE);
        if (psalmParcelable.getTonalities() != null) {
            txtPsalmTonalities.setText(Html.fromHtml("<b>" + TextUtils.join(", ", psalmParcelable.getTonalities().toArray()) + "</b>"));
        } else txtPsalmAnnotation.setVisibility(View.GONE);

        String psalmInfo = "";
        if (psalmParcelable.getAuthor() != null) psalmInfo += "  <b>Слова: </b>" + psalmParcelable.getAuthor();
        if (psalmParcelable.getTranslator() != null) psalmInfo += "  <b>Перевод: </b>" + psalmParcelable.getTranslator();
        if (psalmParcelable.getComposer() != null) psalmInfo += "  <b>Музыка: </b>" + psalmParcelable.getComposer() + "  ";
        if (psalmInfo.length() > 5) txtPsalmInfo.setText(Html.fromHtml(psalmInfo));
        else txtPsalmInfo.setVisibility(View.GONE);

        listView.addHeaderView(psalmHeaderView);
        listView.addFooterView(psalmFooterView);
        listView.setAdapter(psalmPartsArrayAdapter);
        return v;
    }
}
