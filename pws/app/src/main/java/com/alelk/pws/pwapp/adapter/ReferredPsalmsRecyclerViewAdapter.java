/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alelk.pws.pwapp.adapter;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alelk.pws.pwapp.R;

import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_BOOKDISPLAYNAME;
import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMNAME;
import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMNUMBER;
import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMNUMBER_ID;
import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMREF_REASON;
import static com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMREF_VOLUME;

/**
 * Referred Psalms Recycler View Adapter
 *
 * Created by Alex Elkin on 06.01.2017.
 */

public class ReferredPsalmsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final float MIN_HEADER_TEXT_SIZE = 10f;
    private float mHeaderTextSize;

    public interface OnItemClickListener {
        void onItemClick(long psalmNumberId);
    }

    private Cursor mCursor;
    private final OnItemClickListener mOnItemClickListener;

    public ReferredPsalmsRecyclerViewAdapter(final OnItemClickListener onItemClickListener, float headerTextSize) {
        mOnItemClickListener = onItemClickListener;
        mHeaderTextSize = headerTextSize;
    }

    public void swapCursor(Cursor cursor) {
        if (mCursor != null) mCursor.close();
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_referred_psalm_list_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_referred_psalm_list_item, parent, false);
        return new ReferredViewHolder(v, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            if (mHeaderTextSize < MIN_HEADER_TEXT_SIZE) return;
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.bind(mHeaderTextSize);
        } else {
            ReferredViewHolder referredViewHolder = (ReferredViewHolder) holder;
            if (mCursor != null && mCursor.moveToPosition(position - 1)) {
                referredViewHolder.bind(mCursor, mOnItemClickListener);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        if (mCursor == null || mCursor.isClosed() || mCursor.getCount() == 0) return 0;
        return mCursor.getCount() + 1;
    }

    private static class ReferredViewHolder extends RecyclerView.ViewHolder {
        TextView psalmName;
        TextView psalmNumber;
        TextView bookDisplayName;
        TextView reason;
        long psalmNumberId;
        final Context mContext;

        ReferredViewHolder(View itemView, Context context) {
            super(itemView);
            mContext = context;
            psalmName = itemView.findViewById(R.id.txt_psalm_name);
            psalmNumber = itemView.findViewById(R.id.txt_psalm_number);
            bookDisplayName = itemView.findViewById(R.id.txt_book_name);
            reason = itemView.findViewById(R.id.txt_reason);
        }

        void bind(final Cursor cursor, final OnItemClickListener onItemClickListener) {
            psalmNumber.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER)));
            psalmName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME)));
            bookDisplayName.setText(cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME)));
            psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID));
            String reasonTxt = "";
            if ("variation".equals(cursor.getString(cursor.getColumnIndex(COLUMN_PSALMREF_REASON)))) {
                reasonTxt = mContext.getString(R.string.lbl_another_variant,
                        cursor.getInt(cursor.getColumnIndex(COLUMN_PSALMREF_VOLUME)));
            }
            reason.setText(reasonTxt);
            itemView.setOnClickListener(v -> onItemClickListener.onItemClick(psalmNumberId));
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView txtReferences;

        HeaderViewHolder(View itemView) {
            super(itemView);
            txtReferences = itemView.findViewById(R.id.lbl_psalm_references);
        }

        void bind(float textSize) {
            txtReferences.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }
}
