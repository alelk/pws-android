package com.alelk.pws.database.provider;

import android.net.Uri;

import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

import java.util.Arrays;

import static android.app.SearchManager.SUGGEST_URI_PATH_QUERY;
import static com.alelk.pws.database.table.PwsFavoritesTable.COLUMN_PSALMNUMBERID;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.COLUMN_PSALMID;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public interface PwsDataProviderContract {
    String SCHEME = "content";
    String AUTHORITY = "com.alelk.pws.database.provider";
    String DATABASE_NAME = "pws.db";
    int DATABASE_VERSION = 25;

    String HISTORY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    class Psalms {
        public static final String COLUMN_ID = PwsPsalmTable.COLUMN_ID;
        protected static final String COLUMN_PSALMID = "psalmid";
        public static final String COLUMN_PSALMNAME = "psalm" + PwsPsalmTable.COLUMN_NAME;
        public static final String COLUMN_PSALMTEXT = "psalm" + PwsPsalmTable.COLUMN_TEXT;
        public static final String COLUMN_PSALMANNOTATION = "psalm" + PwsPsalmTable.COLUMN_ANNOTATION;
        public static final String COLUMN_PSALMAUTHOR = "psalm" + PwsPsalmTable.COLUMN_AUTHOR;
        public static final String COLUMN_PSALMCOMPOSER = "psalm" + PwsPsalmTable.COLUMN_COMPOSER;
        public static final String COLUMN_PSALMTRANSLATOR = "psalm" + PwsPsalmTable.COLUMN_TRANSLATOR;
        public static final String COLUMN_PSALMTONALITIES = "psalm" + PwsPsalmTable.COLUMN_TONALITIES;

        public static final String PATH = TABLE_PSALMS;
        protected static final String PATH_ID = TABLE_PSALMS + "/#";
        protected static final int URI_MATCH = 20;
        protected static final int URI_MATCH_ID = 21;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

        public static class Suggestions {
            public static final String PATH = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY;
            protected static final String PATH_NUMBER = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/#";
            protected static final String PATH_NAME = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/*";
            protected static final int URI_MATCH_NUMBER = 25;
            protected static final int URI_MATCH_NAME= 26;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        }
        public static class Search {
            public static final String PATH = TABLE_PSALMS +  "/search";
            protected static final int URI_MATCH = 27;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        }
        public static class PsalmNumbers {
            protected static final String PATH = TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS;
            protected static final String PATH_ID = TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS + "/#";
            protected static final int URI_MATCH = 28;
            protected static final int URI_MATCH_ID = 29;
        }
    }

    class PsalmNumbers {
        public static final String PATH = TABLE_PSALMNUMBERS;
        public static final String COLUMN_PSALMNUMBER = "psalm" + PwsPsalmNumbersTable.COLUMN_NUMBER;
        public static final String COLUMN_BOOKID = PwsPsalmNumbersTable.COLUMN_BOOKID;
        protected static final String COLUMN_PSALMNUMBER_ID = "psalmnumberid";

        protected static final String PATH_ID = TABLE_PSALMNUMBERS + "/#";
        protected static final int URI_MATCH = 30;
        protected static final int URI_MATCH_ID = 31;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        public static class Psalm {
            public static final String PATH_SEGMENT = "psalm";
            public static final String COLUMN_ID = Psalms.COLUMN_ID;
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
            public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
            public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
            public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
            public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
            public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
            public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
            public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;

            protected static final String PATH = TABLE_PSALMNUMBERS + "/#/" + PATH_SEGMENT;
            protected static final int URI_MATCH = 32;

            public static final String DEFAULT_SELECTION = COLUMN_PSALMNUMBERID + "=?";
            protected static final String TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS;
            protected static final String[] PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " AS " + COLUMN_PSALMNAME,
                    "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
                    "p." + PwsPsalmTable.COLUMN_AUTHOR + " AS " + COLUMN_PSALMAUTHOR,
                    "p." + PwsPsalmTable.COLUMN_COMPOSER + " AS " + COLUMN_PSALMCOMPOSER,
                    "p." + PwsPsalmTable.COLUMN_TRANSLATOR + " AS " + COLUMN_PSALMTRANSLATOR,
                    "p." + PwsPsalmTable.COLUMN_TONALITIES + " AS " + COLUMN_PSALMTONALITIES,
                    "p." + PwsPsalmTable.COLUMN_ANNOTATION + " AS " + COLUMN_PSALMANNOTATION,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                    "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME
            };

            public static Uri getContentUri(long psalmNumberId) {
                return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT).build();
            }
        }
    }

    class Favorites {
        public static final String PATH = TABLE_FAVORITES;
        protected static final String PATH_ID = TABLE_FAVORITES + "/#";
        protected static final int URI_MATCH = 40;
        protected static final int URI_MATCH_ID = 41;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
    }

    class History {
        public static final String COLUMN_ID = PwsHistoryTable.COLUMN_ID;
        public static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;
        public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
        public static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
        public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
        public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
        public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
        public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
        public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
        public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
        public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
        public static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
        public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
        public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
        protected static final String COLUMN_HISTORYID = "historyid";
        public static final String COLUMN_HISTORYTIMESTAMP = "history" + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP;

        public static final String PATH = TABLE_HISTORY;
        protected static final String PATH_ID = TABLE_HISTORY + "/#";
        protected static final int URI_MATCH = 50;
        protected static final int URI_MATCH_ID = 51;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        protected static final String TABLES = TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS;
        protected static final String SORT_ORDER = COLUMN_HISTORYTIMESTAMP + " DESC";
        protected static final String GROUP_BY = "h." + PwsHistoryTable.COLUMN_ID;
        protected static final String[] PROJECTION = {
                "h." + PwsHistoryTable.COLUMN_ID + " AS " + COLUMN_ID,
                "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_PSALMID,
                "p." + PwsPsalmTable.COLUMN_NAME + " AS " + COLUMN_PSALMNAME,
                "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
                "p." + PwsPsalmTable.COLUMN_AUTHOR + " AS " + COLUMN_PSALMAUTHOR,
                "p." + PwsPsalmTable.COLUMN_COMPOSER + " AS " + COLUMN_PSALMCOMPOSER,
                "p." + PwsPsalmTable.COLUMN_TRANSLATOR + " AS " + COLUMN_PSALMTRANSLATOR,
                "p." + PwsPsalmTable.COLUMN_TONALITIES + " AS " + COLUMN_PSALMTONALITIES,
                "p." + PwsPsalmTable.COLUMN_ANNOTATION + " AS " + COLUMN_PSALMANNOTATION,
                "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                "h." + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP + " AS " + COLUMN_HISTORYTIMESTAMP
        };

        public static class Last {
            public static final String COLUMN_ID = Psalms.COLUMN_ID;
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
            public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
            public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
            public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
            public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
            public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
            public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
            public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            public static final String COLUMN_HISTORYTIMESTAMP = History.COLUMN_HISTORYTIMESTAMP;
            public static final String COLUMN_HISTORYID = History.COLUMN_HISTORYID;

            public static final String PATH = TABLE_HISTORY + "/last";
            protected static final int URI_MATCH = 53;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

            protected static final String TABLES = History.TABLES;
            protected static final String SORT_ORDER = COLUMN_HISTORYTIMESTAMP + " DESC";;
            protected static final String LIMIT = "1";
            protected static final String[] PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " AS " + COLUMN_PSALMNAME,
                    "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
                    "p." + PwsPsalmTable.COLUMN_AUTHOR + " AS " + COLUMN_PSALMAUTHOR,
                    "p." + PwsPsalmTable.COLUMN_COMPOSER + " AS " + COLUMN_PSALMCOMPOSER,
                    "p." + PwsPsalmTable.COLUMN_TRANSLATOR + " AS " + COLUMN_PSALMTRANSLATOR,
                    "p." + PwsPsalmTable.COLUMN_TONALITIES + " AS " + COLUMN_PSALMTONALITIES,
                    "p." + PwsPsalmTable.COLUMN_ANNOTATION + " AS " + COLUMN_PSALMANNOTATION,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                    "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                    "h." + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP + " AS " + COLUMN_HISTORYTIMESTAMP,
                    "h." + PwsHistoryTable.COLUMN_ID + " AS " + COLUMN_HISTORYID
            };
        }
    }

    class Books {
        public static final String COLUMN_BOOKEDITION = "book" + PwsBookTable.COLUMN_EDITION;
        public static final String COLUMN_BOOKDISPLAYNAME = "book" + PwsBookTable.COLUMN_DISPLAYNAME;
    }

    String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS =
            TABLE_PSALMS + " AS p " +
                    "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
                    " INNER JOIN " + TABLE_BOOKS + " as b " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    String TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS = TABLE_HISTORY + " AS h " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON h." + PwsHistoryTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_PSALMS + " as p " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID;
}
