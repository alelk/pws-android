package com.alelk.pws.pwapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmPartType;
import com.alelk.pws.pwapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex Elkin on 13.07.2015.
 */
public class PsalmPartsAdapter extends BaseAdapter {

    private List<PsalmPart> mPsalmParts;
    private LayoutInflater mLayoutInflater;

    public PsalmPartsAdapter(Context context, List<PsalmPart> psalmParts) {
        mPsalmParts = psalmParts;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        if (mPsalmParts == null) return 0;
        return mPsalmParts.size();
    }

    public void addItem(PsalmPart psalmPart) {
        if (mPsalmParts == null) mPsalmParts = new ArrayList<>();
        mPsalmParts.add(psalmPart);
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        if (mPsalmParts == null) return null;
        return mPsalmParts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PsalmPart psalmPart = null;
        if (mPsalmParts != null) {
            psalmPart = mPsalmParts.get(position);
        }
        if (psalmPart != null) {
            if (convertView == null) {
                if (PsalmPartType.VERSE.toString().equals(psalmPart.getPsalmType().toString())) {
                    convertView = mLayoutInflater.inflate(R.layout.layout_psalmverse, null);
                } else if (PsalmPartType.CHORUS.toString().equals(psalmPart.getPsalmType().toString())) {
                    convertView = mLayoutInflater.inflate(R.layout.layout_psalmchorus, null);
                }
            }
            TextView txtPsalmPartText = (TextView) convertView.findViewById(R.id.txt_psalmpart);
            txtPsalmPartText.setText(psalmPart.getText());
        }
        return convertView;
    }
}
