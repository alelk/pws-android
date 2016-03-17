package com.alelk.pws.pwapp.adapter;

import static com.alelk.pws.database.provider.PwsDataProviderContract.History.*;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.pwapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by Alex Elkin on 11.03.2016.
 */
public class HistoryCursorAdapter extends CursorAdapter {

    private LayoutInflater mLayoutInflater;

    public HistoryCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.layout_history_list_item, parent, false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView txtPsalmName = (TextView) view.findViewById(R.id.txt_history_psalm_name);
        TextView txtPsalmNumber = (TextView) view.findViewById(R.id.txt_history_psalm_number);
        TextView txtTimestamp = (TextView) view.findViewById(R.id.txt_history_timestamp);
        txtPsalmName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME)));
        txtPsalmNumber.setText(cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER)) + " " +
                cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME)));

        String timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_HISTORYTIMESTAMP));
        SimpleDateFormat df = new SimpleDateFormat(PwsDataProviderContract.HISTORY_DATE_FORMAT);
        try {
            timestamp = (String) DateUtils.getRelativeTimeSpanString(df.parse(timestamp).getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
            txtTimestamp.setText(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
