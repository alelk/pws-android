package com.alelk.pws.pwapp.adapter;

import android.database.Cursor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 19.05.2016.
 */
public class FavoritesRecyclerViewAdapter extends RecyclerView.Adapter<FavoritesRecyclerViewAdapter.FavoriteViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(long psalmNumberId);
    }

    private final Cursor mCursor;
    private final OnItemClickListener mOnItemClickListener;


    public FavoritesRecyclerViewAdapter(Cursor cursor, OnItemClickListener onItemClickListener) {
        mCursor = cursor;
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext().getApplicationContext()).inflate(R.layout.layout_favorites_list_item, parent, false);
        return new FavoriteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FavoriteViewHolder holder, int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            holder.bind(mCursor, mOnItemClickListener);
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) return 0;
        return mCursor.getCount();
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView psalmName;
        TextView psalmNumber;
        TextView bookDisplayName;
        long psalmNumberId;

        public FavoriteViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_favorite);
            psalmName = (TextView) itemView.findViewById(R.id.txt_psalm_name);
            psalmNumber = (TextView) itemView.findViewById(R.id.txt_psalm_number);
            bookDisplayName = (TextView) itemView.findViewById(R.id.txt_book_name);
        }

        public void bind (final Cursor cursor, final OnItemClickListener onItemClickListener) {
            psalmNumber.setText(cursor.getString(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_PSALMNUMBER)));
            psalmName.setText(cursor.getString(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_PSALMNAME)));
            bookDisplayName.setText(cursor.getString(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_BOOKDISPLAYNAME)));
            psalmNumberId = cursor.getLong(cursor.getColumnIndex(PwsDataProvider.Favorites.COLUMN_PSALMNUMBER_ID));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(psalmNumberId);
                }
            });
        }
    }
}
