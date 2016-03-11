package com.alelk.pws.database.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.net.Uri;

import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;

import java.net.URI;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public final class PwsDataProviderContract {
    public static final String SCHEME = "content";
    public static final String AUTHORITY = "com.alelk.pws.database.provider";
    public static final String DATABASE_NAME = "pws.db";
    public static final int DATABASE_VERSION = 13;

    public static final String HISTORY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String COLUMN_PSALM_NAME = "psalmname";
    public static final String COLUMN_BOOK_NAME = "bookname";

    public static class Favorites {
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PwsFavoritesTable.TABLE_FAVORITES).build();
    }
    public static class History {
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PwsHistoryTable.TABLE_HISTORY).build();
    }
}
