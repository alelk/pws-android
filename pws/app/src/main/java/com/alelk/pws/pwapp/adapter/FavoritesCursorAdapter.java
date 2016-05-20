package com.alelk.pws.pwapp.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 24.02.2016.
 */
public class FavoritesCursorAdapter extends CursorAdapter {

    private LayoutInflater mLayoutInflater;

    public FavoritesCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.layout_favorites_list_item, parent, false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView txtPsalmName = (TextView) view.findViewById(R.id.txt_psalm_name);
        TextView txtPsalmNumber = (TextView) view.findViewById(R.id.txt_psalm_number);
        txtPsalmName.setText(cursor.getString(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_PSALMNAME)));
        txtPsalmNumber.setText(cursor.getLong(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_PSALMNUMBER)) + " " +
                cursor.getString(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_BOOKDISPLAYNAME)));

    }
}
