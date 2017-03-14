package com.alelk.pws.pwapp.adapter;

import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alelk.pws.pwapp.R;


import static com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.*;

/**
 * Created by Alex Elkin on 23.05.2016.
 */
public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.SearchViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(long psalmNumberId);
    }

    private Cursor mCursor;
    private final OnItemClickListener mClickListener;

    public SearchRecyclerViewAdapter(OnItemClickListener onItemClickListener) {
        mClickListener = onItemClickListener;
    }

    public SearchRecyclerViewAdapter(Cursor cursor, OnItemClickListener onItemClickListener) {
        mCursor = cursor;
        mClickListener = onItemClickListener;
    }

    public void swapCursor(Cursor cursor) {
        if (mCursor != null) mCursor.close();
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_search_list_item, parent, false);
        return new SearchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(SearchViewHolder holder, int position) {
        if (mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)) {
            holder.bind(mCursor, mClickListener);
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor == null || mCursor.isClosed()) return 0;
        return mCursor.getCount();
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView psalmName;
        TextView psalmNumber;
        TextView bookDisplayName;
        TextView text;
        long psalmNumberId;

        public SearchViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_search);
            psalmName = (TextView) itemView.findViewById(R.id.txt_psalm_name);
            psalmNumber = (TextView) itemView.findViewById(R.id.txt_psalm_number);
            bookDisplayName = (TextView) itemView.findViewById(R.id.txt_book_name);
            text = (TextView) itemView.findViewById(R.id.txt_text);
        }

        public void bind (final Cursor cursor, final OnItemClickListener onItemClickListener) {
            psalmNumber.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER)));
            psalmName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME)));
            bookDisplayName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME)));
            final String snippet = cursor.getString(cursor.getColumnIndex(COLUMN_SNIPPET));
            if (snippet != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    text.setText(Html.fromHtml(snippet, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    text.setText(Html.fromHtml(snippet));
                }
            }
            psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(psalmNumberId);
                }
            });
        }
    }
}
