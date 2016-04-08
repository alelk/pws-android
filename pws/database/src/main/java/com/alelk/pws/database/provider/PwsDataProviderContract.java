package com.alelk.pws.database.provider;

import android.net.Uri;

import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_1;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_2;
import static android.app.SearchManager.SUGGEST_URI_PATH_QUERY;
import static com.alelk.pws.database.table.PwsBookStatisticTable.TABLE_BOOKSTATISTIC;
import static com.alelk.pws.database.table.PwsPsalmFtsTable.TABLE_PSALMS_FTS;
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
    int DATABASE_VERSION = 34;

    String HISTORY_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    class Psalms {
        public static final String COLUMN_ID = "_id";
        protected static final String COLUMN_PSALMID = "psalm_id";
        public static final String COLUMN_PSALMNAME = "psalm_name";
        public static final String COLUMN_PSALMTEXT = "psalm_text";
        public static final String COLUMN_PSALMANNOTATION = "psalm_annotation";
        public static final String COLUMN_PSALMAUTHOR = "psalm_author";
        public static final String COLUMN_PSALMCOMPOSER = "psalm_composer";
        public static final String COLUMN_PSALMTRANSLATOR = "psalm_translator";
        public static final String COLUMN_PSALMTONALITIES = "psalm_tonalities";
        public static final String COLUMN_PSALMNUMBER = "psalm_number";

        public static final String PATH = TABLE_PSALMS;
        protected static final String PATH_ID = TABLE_PSALMS + "/#";
        protected static final int URI_MATCH = 20;
        protected static final int URI_MATCH_ID = 21;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

        public static class Suggestions {
            public static final String COLUMN_ID = "_id";
            protected static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;
            protected static final String COLUMN_PSALMNUMBER = Psalms.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKSTATISTICPREFERENCE = BookStatistic.COLUMN_BOOKSTATISTICPREFERENCE;

            public static final String PATH = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY;
            protected static final String PATH_NUMBER = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/#";
            protected static final String PATH_NAME = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/*";
            protected static final int URI_MATCH_NUMBER = 25;
            protected static final int URI_MATCH_NAME= 26;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

            protected static final String SGNAME_TABLE = "psugg";
            protected static final String SGNAME_RAW1_ORDERBY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE;
            protected static final String SELECTION_PREFERRED_BOOKS_ONLY = BookStatistic.SELECTION_PREFERRED_BOOKS_ONLY;
            protected static final String SGNAME_TABLES = TABLE_PSALMS_JOIN_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC;
            protected static final String[] SGNAME_RAW1_PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " as psalmid",
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as _id",
                    "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " as " + COLUMN_BOOKSTATISTICPREFERENCE ,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as psalmname"
            };
            protected static final String SGNAME_RAW2_GROUPBY = SGNAME_TABLE + ".psalmid";
            protected static final String[] SGNAME_PROJECTION = {
                    SGNAME_TABLE + ".psalmname AS " + SUGGEST_COLUMN_TEXT_1,
                    SGNAME_TABLE + "._id AS " + SUGGEST_COLUMN_INTENT_DATA_ID,
                    SGNAME_TABLE + "._id AS _id",
            };
            protected static final String SGNUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC;
            protected static final String SGNUM_SORT_ORDER = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC";
            protected static final String[] SGNUM_PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + SUGGEST_COLUMN_TEXT_2,
                    "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " as " + COLUMN_BOOKSTATISTICPREFERENCE,
                    "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
            };

            protected static String getSgNameSelection(String searchText) {
                return TABLE_PSALMS_FTS + "." + PwsPsalmTable.COLUMN_NAME + " match \'" + searchText + '\'';
            }
            protected static String getSgNumberSelection(String psalmNumber) {
                return COLUMN_PSALMNUMBER + "=" + psalmNumber;
            }
        }
        public static class Search {
            public static final String COLUMN_ID = "_id";
            protected static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMNUMBER_ID = PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            public static final String COLUMN_BOOKSTATISTICPREFERENCE = BookStatistic.COLUMN_BOOKSTATISTICPREFERENCE;
            public static final String COLUMN_SNIPPET = "snippet";
            protected static final String COLUMN_MATCHINFO = "matchinfo";

            public static final String PATH = TABLE_PSALMS +  "/search";
            protected static final int URI_MATCH = 27;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

            protected static final String SNUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC;
            protected static final String SNUM_ORDER_BY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC";
            protected static final String SNUM_SELECTION = COLUMN_PSALMNUMBER + "=?";
            protected static final String STXT_RAW1_TABLES = TABLE_PSALMS_JOIN_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC;
            public static final String STXT_SELECTION = TABLE_PSALMS_FTS + " match ?";
            protected static final String STXT_RAW1_SELECTION_PREFERRED_BOOKS_ONLY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " > 0";
            protected static final String STXT_RAW2_GROUP_BY = "search." + COLUMN_PSALMID;
            protected static final String STXT_RAW1_ORDER_BY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC";
            protected static final String STXT_RAW2_ORDER_BY = "search." + COLUMN_MATCHINFO + " DESC";
            protected static final String[] STXT_RAW1_PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_PSALMID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + PwsPsalmTable.COLUMN_NAME,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + PwsPsalmNumbersTable.COLUMN_NUMBER,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + PwsPsalmNumbersTable.COLUMN_ID,
                    "b." + PwsBookTable.COLUMN_EDITION + " as " + PwsBookTable.COLUMN_EDITION,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + PwsBookTable.COLUMN_DISPLAYNAME,
                    "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " as " + PwsBookStatisticTable.COLUMN_USERPREFERENCE,
                    "snippet(" + TABLE_PSALMS_FTS + ", '<b><font color=#247b34>', '</font></b>', '...') as " + COLUMN_SNIPPET,
                    "matchinfo(" + TABLE_PSALMS_FTS + ", 'x') as " + COLUMN_MATCHINFO
            };
            protected static final String[] STXT_PROJECTION = {
                    "search." + COLUMN_PSALMID + " as " + COLUMN_PSALMID,
                    "search." + COLUMN_PSALMID + " as " + COLUMN_ID,
                    "search." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
                    "search." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "search." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
                    "search." + PwsBookTable.COLUMN_EDITION + " as " + COLUMN_BOOKEDITION,
                    "search." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
                    "search." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " as " + COLUMN_BOOKSTATISTICPREFERENCE,
                    "search." + COLUMN_SNIPPET + " as " + COLUMN_SNIPPET
            };
            protected static final String[] SNUM_PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_PSALMID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
                    "b." + PwsBookTable.COLUMN_EDITION + " as " + COLUMN_BOOKEDITION,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
                    "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " as " + COLUMN_BOOKSTATISTICPREFERENCE,
                    "substr(p." + PwsPsalmTable.COLUMN_TEXT + ", 1, 100) as " + COLUMN_SNIPPET
            };
            public static final String[] getSelectionArgs(String searchText) {
                return new String[] {searchText};
            }
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
        public static final String COLUMN_PSALMNUMBER = "psalm_number";
        public static final String COLUMN_BOOKID = "book_id";
        public static final String COLUMN_PSALMNUMBER_ID = "psalmnumberid";

        protected static final String PATH_ID = TABLE_PSALMNUMBERS + "/#";
        protected static final int URI_MATCH = 30;
        protected static final int URI_MATCH_ID = 31;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

        protected static final String[] PROJECTION = {
              "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as _id",
              "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
              "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
              "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " as " + COLUMN_BOOKID,
        };
        public static class Psalm {
            public static final String PATH_SEGMENT = "psalm";
            public static final String COLUMN_ID = "_id";
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
            public static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;

            protected static final String PATH = TABLE_PSALMNUMBERS + "/#/" + PATH_SEGMENT;
            protected static final int URI_MATCH = 32;

            public static final String DEFAULT_SELECTION = COLUMN_PSALMNUMBER_ID + "=?";
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
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                    "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
            };

            public static Uri getContentUri(long psalmNumberId) {
                return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT).build();
            }
        }
    }

    class Favorites {
        public static final String COLUMN_ID = "_id";
        protected static final String COLUMN_FAVORITEID = "favorite_id";
        public static final String COLUMN_FAVORITEPOSITION = "favorite_position";
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

        public static final String PATH = TABLE_FAVORITES;
        protected static final String PATH_ID = TABLE_FAVORITES + "/#";
        protected static final int URI_MATCH = 40;
        protected static final int URI_MATCH_ID = 41;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        protected static final String TABLES = TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS;
        protected static final String SORT_ORDER = COLUMN_FAVORITEPOSITION + " DESC";
        protected static final String GROUP_BY = COLUMN_FAVORITEPOSITION;
        protected static final String SELECTION_ID_MATCH = COLUMN_ID + " match ?";
        protected static final String[] PROJECTION = {
                "f." + PwsFavoritesTable.COLUMN_ID + " AS " + COLUMN_ID,
                "f." + PwsFavoritesTable.COLUMN_POSITION + " AS " + COLUMN_FAVORITEPOSITION,
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
                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME
        };

    }

    class History {
        public static final String COLUMN_ID = "_id";
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
        public static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;
        protected static final String COLUMN_HISTORYID = "history_id";
        public static final String COLUMN_HISTORYTIMESTAMP = "history_timestamp";

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
                "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
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
        public static final String COLUMN_BOOKID = "book_id";
        public static final String COLUMN_BOOKEDITION = "book_edition";
        public static final String COLUMN_BOOKDISPLAYNAME = "book_display_name";
        public static final String COLUMN_BOOKDISPLAYSHORTNAME = "book_display_short_name";
    }

    class BookStatistic {
        public static final String COLUMN_BOOKSTATISTICPREFERENCE = "bookstatistic" + PwsBookStatisticTable.COLUMN_USERPREFERENCE;

        public static final String SELECTION_PREFERRED_BOOKS_ONLY = COLUMN_BOOKSTATISTICPREFERENCE + " > 0";
    }

    String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS =
            TABLE_PSALMS + " AS p " +
                    "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
                    " INNER JOIN " + TABLE_BOOKS + " as b " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC =
            TABLE_PSALMS + " AS p " +
                    "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
                    " INNER JOIN " + TABLE_BOOKS + " as b " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
                    " INNER JOIN " + TABLE_BOOKSTATISTIC + " as bs " +
                    "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    String TABLE_PSALMS_JOIN_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC =
            TABLE_PSALMS + " AS p " +
                    "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
                    " INNER JOIN " + TABLE_PSALMS_FTS +
                    " ON docid=p." + PwsPsalmTable.COLUMN_ID +
                    " INNER JOIN " + TABLE_BOOKS + " as b " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
                    " INNER JOIN " + TABLE_BOOKSTATISTIC + " as bs " +
                    "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    String TABLE_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMNUMBERS + " AS pn " +
            "INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    String TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS = TABLE_HISTORY + " AS h " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON h." + PwsHistoryTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_PSALMS + " as p " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID;

    String TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS = TABLE_FAVORITES + " AS f " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON f." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_PSALMS + " as p " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID;
}
