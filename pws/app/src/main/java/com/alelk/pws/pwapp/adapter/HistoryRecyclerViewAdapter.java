package com.alelk.pws.pwapp.adapter;

import android.database.Cursor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static com.alelk.pws.database.provider.PwsDataProviderContract.History.*;

import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.pwapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * History Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
public class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<HistoryRecyclerViewAdapter.HistoryViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(long psalmNumberId);
    }

    private Cursor mCursor;
    private final OnItemClickListener mOnItemClickListener;

    public HistoryRecyclerViewAdapter(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public HistoryRecyclerViewAdapter(Cursor cursor, OnItemClickListener onItemClickListener) {
        mCursor = cursor;
        mOnItemClickListener = onItemClickListener;
    }

    public void swapCursor(Cursor cursor) {
        if (mCursor != null) mCursor.close();
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_history_list_item, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            holder.bind(mCursor, mOnItemClickListener);
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor == null || mCursor.isClosed()) return 0;
        return mCursor.getCount();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView psalmName;
        TextView psalmNumber;
        TextView bookDisplayName;
        TextView timestamp;
        long psalmNumberId;

        HistoryViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_history);
            psalmName = (TextView) itemView.findViewById(R.id.txt_psalm_name);
            psalmNumber = (TextView) itemView.findViewById(R.id.txt_psalm_number);
            bookDisplayName = (TextView) itemView.findViewById(R.id.txt_book_name);
            timestamp = (TextView) itemView.findViewById(R.id.txt_timestamp);
        }

        public void bind (final Cursor cursor, final OnItemClickListener onItemClickListener) {
            psalmNumber.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER)));
            psalmName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME)));
            bookDisplayName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME)));
            psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID));
            String accessTime = cursor.getString(cursor.getColumnIndex(COLUMN_HISTORYTIMESTAMP));
            SimpleDateFormat df = new SimpleDateFormat(PwsDataProviderContract.HISTORY_TIMESTAMP_FORMAT);
            try {
                accessTime = (String) DateUtils.getRelativeTimeSpanString(df.parse(accessTime).getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
                timestamp.setText(accessTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(psalmNumberId);
                }
            });
        }
    }
}
