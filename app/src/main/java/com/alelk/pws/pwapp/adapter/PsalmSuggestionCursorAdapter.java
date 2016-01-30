package com.alelk.pws.pwapp.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.pwapp.R;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by Alex Elkin on 20.05.2015.
 */
public class PsalmSuggestionCursorAdapter extends CursorAdapter {

    public PsalmSuggestionCursorAdapter(Context context, Cursor c) {
        super(context, c, false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_psalms_list, parent, false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView txtPsalmNumber = (TextView) view.findViewById(R.id.txt_psalm_number);
        TextView txtPsalmName = (TextView) view.findViewById(R.id.txt_psalm_name);
        txtPsalmName.setText(cursor.getString(cursor.getColumnIndex("name")));
        //txtPsalmNumber.setText(cursor.getString(cursor.getColumnIndex("number")));
    }
}
