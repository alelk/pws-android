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

package com.alelk.pws.database.provider;

import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.alelk.pws.database.BuildConfig;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmPsalmReferencesTable;
import com.alelk.pws.database.table.PwsPsalmTable;

import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_1;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_2;
import static android.app.SearchManager.SUGGEST_URI_PATH_QUERY;
import static com.alelk.pws.database.table.PwsBookStatisticTable.TABLE_BOOKSTATISTIC;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;
import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;
import static com.alelk.pws.database.table.PwsPsalmFtsTable.TABLE_PSALMS_FTS;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmPsalmReferencesTable.TABLE_PSALMPSALMREFERENCES;

/**
 * Pws Data Provider Contract
 *
 * Created by Alex Elkin on 21.05.2015.
 */
public interface PwsDataProviderContract {
    String SCHEME = "content";
    String AUTHORITY = BuildConfig.DB_AUTHORITY;
    String QUERY_PARAMETER_LIMIT = "limit";

    String HISTORY_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    class Psalms {
        public static final String COLUMN_ID = "_id";
        static final String COLUMN_PSALMID = "psalm_id";
        static final String COLUMN_PSALMNAME = "psalm_name";
        static final String COLUMN_PSALMTEXT = "psalm_text";
        static final String COLUMN_PSALMANNOTATION = "psalm_annotation";
        static final String COLUMN_PSALMAUTHOR = "psalm_author";
        static final String COLUMN_PSALMCOMPOSER = "psalm_composer";
        static final String COLUMN_PSALMTRANSLATOR = "psalm_translator";
        static final String COLUMN_PSALMTONALITIES = "psalm_tonalities";
        static final String COLUMN_PSALMNUMBER = "psalm_number";
        static final String COLUMN_PSALMLOCALE = "psalm_locale";

        static final String PATH = "psalms";
        static final String PATH_ID = PATH + "/#";
        static final int URI_MATCH = 20;
        static final int URI_MATCH_ID = 21;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

        static class Suggestions {
            public static final String COLUMN_ID = "_id";
            public static final String COLUMN_PSALMNUMBER = Psalms.COLUMN_PSALMNUMBER;
            public static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;

            static final String PATH = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY;
            static final String PATH_NUMBER = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/#";
            static final String PATH_NAME = TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/*";
            static final int URI_MATCH_NUMBER = 25;
            static final int URI_MATCH_NAME= 26;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
            // Suggestions for psalm number
            static final String SGNUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC;
            static final String SGNUM_SORT_ORDER = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC";
            static final String[] SGNUM_PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ', ') as " + SUGGEST_COLUMN_TEXT_2,
                    "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
                    "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_PSALMID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
            };
            static String getSgNumberSelection(String psalmNumber) {
                return COLUMN_PSALMNUMBER + "=" + psalmNumber + " and bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0";
            }
            // Suggestions for psalm name
            static final String SG_NAME_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED;
            static final String[] SG_NAME_PROJECTION = {
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + SUGGEST_COLUMN_INTENT_DATA_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + SUGGEST_COLUMN_TEXT_1,
                    "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ' | ') as " + SUGGEST_COLUMN_TEXT_2,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
            };
            static final String SG_NAME_GROUPBY = "p.docid";
            static String getSgNameSelection(@NonNull String searchText) {
                return "p." + PwsPsalmTable.COLUMN_NAME + " MATCH '" + searchText.trim().replaceAll("\\s+", "*") + "*'";
            }
            // Suggestions for psalm text
            static final String SG_TXT_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED;
            static final String[] SG_TXT_PROJECTION = {
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + SUGGEST_COLUMN_INTENT_DATA_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + SUGGEST_COLUMN_TEXT_1,
                    "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ' | ') as " + SUGGEST_COLUMN_TEXT_2,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
            };
            static final String SG_TXT_GROUPBY = "p.docid";
            static String getSgTextSelection(@NonNull String searchText) {
                return TABLE_PSALMS_FTS + " MATCH '" + searchText.trim().replaceAll("\\s+", "*") + "*'";
            }
        }
        public static class Search {
            public static final String COLUMN_ID = "_id";
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMNUMBER_ID = PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            public static final String COLUMN_SNIPPET = "snippet";
            static final String COLUMN_MATCHINFO = "matchinfo";

            static final String PATH = TABLE_PSALMS +  "/search";
            static final int URI_MATCH = 27;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

            /** Search number **/
            static final String S_NUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED;
            static final String S_NUM_ORDER_BY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC";
            static final String[] S_NUM_PROJECTION = {
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
                    "substr(p." + PwsPsalmTable.COLUMN_TEXT + ", 1, 100) as " + COLUMN_SNIPPET
            };
            static String getSNumSelection(int psalmNumber) {
                return COLUMN_PSALMNUMBER + "=" + psalmNumber;
            }
            /** Search text **/
            static final String S_TXT_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED;
            static final String S_TXT_ORDER_BY = COLUMN_MATCHINFO + " DESC";
            static final String[] S_TXT_PROJECTION = {
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
                    "snippet(" + TABLE_PSALMS_FTS + ", '<b><font color=#247b34>', '</font></b>', '...') as " + COLUMN_SNIPPET,
                    "matchinfo(" + TABLE_PSALMS_FTS + ", 'x') as " + COLUMN_MATCHINFO
            };
            static String getSTxtSelection(String searchText) {
                return TABLE_PSALMS_FTS + " match '" + searchText + "'";
            }

        }
        static class PsalmNumbers {
            static final String PATH = TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS;
            static final String PATH_ID = TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS + "/#";
            static final int URI_MATCH = 28;
            static final int URI_MATCH_ID = 29;
        }
    }

    class PsalmNumbers {
        static final String PATH = "psalmnumbers";
        static final String COLUMN_PSALMNUMBER = "psalm_number";
        static final String COLUMN_BOOKID = "book_id";
        public static final String COLUMN_PSALMNUMBER_ID = "psalmnumberid";

        static final String PATH_ID = PATH + "/#";
        static final int URI_MATCH = 30;
        static final int URI_MATCH_ID = 31;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

        public static final String[] PROJECTION = {
              "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as _id",
              "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
              "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
              "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " as " + COLUMN_BOOKID,
        };
        public static class Psalm {
            static final String PATH_SEGMENT = "psalm";
            public static final String COLUMN_ID = "_id";
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
            public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
            public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
            public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
            public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
            public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
            public static final String COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE;
            public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
            static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
            static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;

            static final String PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT;
            static final int URI_MATCH = 32;

            static final String DEFAULT_SELECTION = COLUMN_PSALMNUMBER_ID + "=?";
            static final String TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS;
            static final String[] PROJECTION = {
                    "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_NAME + " AS " + COLUMN_PSALMNAME,
                    "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
                    "p." + PwsPsalmTable.COLUMN_AUTHOR + " AS " + COLUMN_PSALMAUTHOR,
                    "p." + PwsPsalmTable.COLUMN_COMPOSER + " AS " + COLUMN_PSALMCOMPOSER,
                    "p." + PwsPsalmTable.COLUMN_TRANSLATOR + " AS " + COLUMN_PSALMTRANSLATOR,
                    "p." + PwsPsalmTable.COLUMN_TONALITIES + " AS " + COLUMN_PSALMTONALITIES,
                    "p." + PwsPsalmTable.COLUMN_ANNOTATION + " AS " + COLUMN_PSALMANNOTATION,
                    "p." + PwsPsalmTable.COLUMN_LOCALE + " AS " + COLUMN_PSALMLOCALE,
                    "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                    "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                    "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                    "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                    "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
            };

            public static final String[] PROJECTION_PSALM_TEXT = {
                    "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_ID,
                    "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
                    "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID
            };

            public static Uri getContentUri(long psalmNumberId) {
                return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
                        PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT).build();
            }
        }

        public static class Book {
            static final String PATH_SEGMENT = "book";
            static final String PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT;

            /**
             * Provider contract for URI: psalmnumbers/#/book/bookpsalmnumbers
             */
            public static class BookPsalmNumbers {
                static final String PATH_SEGMENT = "bookpsalmnumbers";
                static final String PATH = PsalmNumbers.Book.PATH + "/" + PATH_SEGMENT;
                static final int URI_MATCH = 331;

                public static final String COLUMN_ID = "_id";
                static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
                public static Uri getContentUri(long psalmNumberId) {
                    return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
                            PsalmNumbers.PATH + "/" + psalmNumberId + "/" +
                                    PsalmNumbers.Book.PATH_SEGMENT + "/" +
                                    PATH_SEGMENT).build();
                }
                static final String[] PROJECTION = {
                        "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
                        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
                };
                static final String ORDER_BY = "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER;
                static String buildRawTables(long psalmNumberId) {
                    return "(" + SQLiteQueryBuilder.buildQueryString(false,
                            TABLE_PSALMNUMBERS_JOIN_BOOKS,
                            new String[] {"b." + PwsBookTable.COLUMN_ID},
                            "pn." + PwsPsalmNumbersTable.COLUMN_ID + "=" + psalmNumberId,
                            null, null,
                            null,
                            "1") + ") as b inner join " +
                            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " as pn on " +
                            "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " = b._id";
                }
                public static class Info {
                    static final String PATH_SEGMENT = "info";
                    static final String PATH = PsalmNumbers.Book.BookPsalmNumbers.PATH + "/" + PATH_SEGMENT;
                    static final int URI_MATCH = 3311;

                    static final String COLUMN_COUNT_OF_ITEMS = "count_of_items";
                    public static final String COLUMN_MAX_PSALMNUMBER = "max_psalm_number";
                    public static final String COLUMN_MIN_PSALMNUMBER = "min_psalm_number";
                    public static final String COLUMN_PSALMNUMBERID_LIST = "psalmnumberids_list";
                    public static final String COLUMN_PSALMNUMBER_LIST = "psalm_numbers_list";
                    public static Uri getContentUri(long psalmNumberId) {
                        return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
                                PsalmNumbers.PATH + "/" + psalmNumberId + "/" +
                                        PsalmNumbers.Book.PATH_SEGMENT + "/" +
                                        PsalmNumbers.Book.BookPsalmNumbers.PATH_SEGMENT + "/" +
                                        PATH_SEGMENT).build();
                    }
                    static String buildRawTables(long psalmNumberId) {
                        return "(" + SQLiteQueryBuilder.buildQueryString(false,
                                BookPsalmNumbers.buildRawTables(psalmNumberId),
                                BookPsalmNumbers.PROJECTION,
                                null, null, null, BookPsalmNumbers.ORDER_BY, null) + ")";
                    }
                    static final String[] PROJECTION_PSALMNUMBER_ID = {
                            "max (" + BookPsalmNumbers.COLUMN_PSALMNUMBER + ") as " + COLUMN_MAX_PSALMNUMBER,
                            "group_concat (" + BookPsalmNumbers.COLUMN_ID + ") as " + COLUMN_PSALMNUMBERID_LIST,
                            "count (" + BookPsalmNumbers.COLUMN_ID + ") as " + COLUMN_COUNT_OF_ITEMS
                    };
                    public static final String[] PROJECTION = {
                            "max (" + BookPsalmNumbers.COLUMN_PSALMNUMBER + ") as " + COLUMN_MAX_PSALMNUMBER,
                            "min (" + BookPsalmNumbers.COLUMN_PSALMNUMBER + ") as " + COLUMN_MIN_PSALMNUMBER,
                            "group_concat (" + BookPsalmNumbers.COLUMN_ID + ") as " + COLUMN_PSALMNUMBERID_LIST,
                            "group_concat (" + BookPsalmNumbers.COLUMN_PSALMNUMBER + ") as " + COLUMN_PSALMNUMBER_LIST,
                            "count (" + BookPsalmNumbers.COLUMN_ID + ") as " + COLUMN_COUNT_OF_ITEMS
                    };
                }
            }
        }
        public static class ReferencePsalms {
            static final String PATH_SEGMENT = "referencepsalms";
            public static final String COLUMN_ID = "_id";
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALM_ID = Psalms.COLUMN_PSALMID;
            public static final String COLUMN_PSALMREF_VOLUME = "reference_psalms_volume";
            public static final String COLUMN_PSALMREF_REASON = "reference_psalms_reason";
            public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            private static final String COLUMN_CURRENT_PSALMNUMBER_ID = "current_psalmnumber_id";
            public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            public static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;

            static final String PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT;
            static final int URI_MATCH = 34;

            private static final String RAW_TABLES_ALIAS = "refs";
            static final String[] PROJECTION= {
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALMNUMBER_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                    RAW_TABLES_ALIAS + "." + COLUMN_ID + " AS " + COLUMN_ID,
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALMNUMBER + " AS " + COLUMN_PSALMNUMBER,
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALMNAME + " AS " + COLUMN_PSALMNAME,
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALM_ID + " AS " + COLUMN_PSALM_ID,
                    RAW_TABLES_ALIAS + "." + COLUMN_BOOKDISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                    RAW_TABLES_ALIAS + "." + COLUMN_BOOKDISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALMREF_REASON + " AS " + COLUMN_PSALMREF_REASON,
                    RAW_TABLES_ALIAS + "." + COLUMN_PSALMREF_VOLUME + " AS " + COLUMN_PSALMREF_VOLUME,
            };
            public static Uri getContentUri(long psalmNumberId) {
                return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
                        PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT).build();
            }
            public static String buildRawTables(long currentPsalmNumberId) {
                return "(" + SQLiteQueryBuilder.buildQueryString(false,
                        TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED_JOIN_REFERENCEPSALMS,
                        new String[] {
                                "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                                "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_ID,
                                "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                                "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_PSALM_ID,
                                "p." + PwsPsalmTable.COLUMN_NAME + " AS " + COLUMN_PSALMNAME,
                                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                                "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
                                "ppr." + PwsPsalmPsalmReferencesTable.COLUMN_REASON + " AS " + COLUMN_PSALMREF_REASON,
                                "ppr." + PwsPsalmPsalmReferencesTable.COLUMN_VOLUME + " AS " + COLUMN_PSALMREF_VOLUME,
                                "cpn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_CURRENT_PSALMNUMBER_ID
                        },
                        COLUMN_CURRENT_PSALMNUMBER_ID + "=" + currentPsalmNumberId,
                        null, null,
                        "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE, null) + ") as " + RAW_TABLES_ALIAS;
            }
        }
    }

    class Favorites {
        public static final String COLUMN_ID = "_id";
        protected static final String COLUMN_FAVORITEID = "favorite_id";
        static final String COLUMN_FAVORITEPOSITION = "favorite_position";
        static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;
        public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
        static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
        static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
        static final String COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE;
        static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
        static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
        static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
        static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
        public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
        public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
        static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
        static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
        public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;

        static final String PATH = "favorites";
        static final String PATH_ID = PATH + "/#";
        static final int URI_MATCH = 40;
        static final int URI_MATCH_ID = 41;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        static final String TABLES = TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS;
        static final String SORT_ORDER = COLUMN_FAVORITEPOSITION + " DESC";
        static final String GROUP_BY = COLUMN_FAVORITEPOSITION;
        static final String SELECTION_ID_MATCH = COLUMN_ID + " match ?";
        static final String[] PROJECTION = {
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
                "p." + PwsPsalmTable.COLUMN_LOCALE + " AS " + COLUMN_PSALMLOCALE,
                "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME
        };

    }

    class History {
        public static final String COLUMN_ID = "_id";
        static final String COLUMN_PSALMID = Psalms.COLUMN_PSALMID;
        public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
        static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
        public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
        public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
        public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
        public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
        public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
        public static final String COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE;
        public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
        public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
        public static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
        public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
        public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
        public static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;
        protected static final String COLUMN_HISTORYID = "history_id";
        public static final String COLUMN_HISTORYTIMESTAMP = "history_timestamp";

        public static final String PATH = "history";
        protected static final String PATH_ID = PATH + "/#";
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
                "p." + PwsPsalmTable.COLUMN_LOCALE + " AS " + COLUMN_PSALMLOCALE,
                "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + COLUMN_PSALMNUMBER,
                "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID,
                "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + COLUMN_BOOKID,
                "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
                "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
                "h." + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP + " AS " + COLUMN_HISTORYTIMESTAMP
        };

        public static Uri getContentUri(int limit) {
            return new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).appendQueryParameter(QUERY_PARAMETER_LIMIT, String.valueOf(limit)).build();
        }

        public static class Last {
            public static final String COLUMN_ID = Psalms.COLUMN_ID;
            public static final String COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME;
            public static final String COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT;
            public static final String COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION;
            public static final String COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR;
            public static final String COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER;
            public static final String COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR;
            public static final String COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES;
            public static final String COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE;
            public static final String COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID;
            public static final String COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER;
            public static final String COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID;
            public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
            public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
            public static final String COLUMN_HISTORYTIMESTAMP = History.COLUMN_HISTORYTIMESTAMP;
            public static final String COLUMN_HISTORYID = History.COLUMN_HISTORYID;

            public static final String PATH = History.PATH + "/last";
            protected static final int URI_MATCH = 53;
            public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();

            protected static final String TABLES = History.TABLES;
            protected static final String SORT_ORDER = COLUMN_HISTORYTIMESTAMP + " DESC";
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
                    "p." + PwsPsalmTable.COLUMN_LOCALE + " AS " + COLUMN_PSALMLOCALE,
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
        public static final String COLUMN_BOOKSTATISTIC_ID = "bookstatistic_id";
        public static final String COLUMN_BOOKSTATISTIC_PREFERENCE = "bookstatistic_userpref";
        public static final String COLUMN_BOOKSTATISTIC_READINGS = "bookstatistic_readings";
        public static final String COLUMN_BOOKSTATISTIC_RAITING = "bookstatistic_raiting";
        public static final String COLUMN_BOOKID = Books.COLUMN_BOOKID;
        public static final String COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME;
        public static final String COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION;
        public static final String COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME;
        public static final String PATH = "bookstatistic";
        static final String PATH_ID = PATH + "/#";
        static final String PATH_TEXT = PATH + "/*";
        static final int URI_MATCH = 60;
        static final int URI_MATCH_ID = 61;
        static final int URI_MATCH_TEXT = 62;
        public static final Uri CONTENT_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build();
        public static Uri getBookStatisticBookEditionUri(String bookEdition) {
            return CONTENT_URI.buildUpon().appendPath(bookEdition).build();
        }
        static final String TABLES = TABLE_BOOKS_JOIN_BOOKSTATISTIC;
        static final String SORT_ORDER = COLUMN_BOOKSTATISTIC_PREFERENCE + " DESC";
        static final String[] PROJECTION = {
                "bs." + PwsBookStatisticTable.COLUMN_ID + " AS " + COLUMN_BOOKSTATISTIC_ID,
                "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " AS " + COLUMN_BOOKSTATISTIC_PREFERENCE,
                "bs." + PwsBookStatisticTable.COLUMN_READINGS + " AS " + COLUMN_BOOKSTATISTIC_READINGS,
                "bs." + PwsBookStatisticTable.COLUMN_RATING + " AS " + COLUMN_BOOKSTATISTIC_RAITING,
                "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
                "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME
        };
        public static final String SELECTION_PREFERRED_BOOKS_ONLY = COLUMN_BOOKSTATISTIC_PREFERENCE + " > 0";

        static final String[] RAW_PROJECTION = {
                "b." + PwsBookTable.COLUMN_ID
        };
        static final String RAW_TABLES = TABLE_BOOKS_JOIN_BOOKSTATISTIC;
        static String getRawSelection(String bookEdition) {
            return "b." + PwsBookTable.COLUMN_EDITION + " LIKE '" + bookEdition + "'";
        }
    }

    String TABLE_BOOKS_JOIN_BOOKSTATISTIC = TABLE_BOOKS + " as b " +
            " INNER JOIN " + TABLE_BOOKSTATISTIC + " as bs " +
            "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

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

    String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED =
            TABLE_BOOKS + " AS b " +
                    " INNER JOIN (SELECT " +
                    PwsBookStatisticTable.COLUMN_BOOKID +
                    ", " + PwsBookStatisticTable.COLUMN_USERPREFERENCE +
                    " FROM " + TABLE_BOOKSTATISTIC +
                    " WHERE " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0 " +
                    " ORDER BY " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ") AS bs " +
                    " ON b." + PwsBookTable.COLUMN_ID + "=bs." + PwsBookStatisticTable.COLUMN_BOOKID +
                    " INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
                    " INNER JOIN " + TABLE_PSALMS +  " AS p " +
                    " ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID;

    String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED_JOIN_REFERENCEPSALMS =
            TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED +
                    " INNER JOIN " + TABLE_PSALMPSALMREFERENCES + " AS ppr " +
                    "ON p." + PwsPsalmTable.COLUMN_ID + " = ppr." + PwsPsalmPsalmReferencesTable.COLUMN_REFPSALMID +
                    " INNER JOIN " + TABLE_PSALMNUMBERS + " AS cpn " +
                    "ON cpn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " = ppr." + PwsPsalmPsalmReferencesTable.COLUMN_PSALMID;

    String TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED =
            TABLE_BOOKS + " AS b " +
                    " INNER JOIN (SELECT " +
                    PwsBookStatisticTable.COLUMN_BOOKID +
                    " FROM " + TABLE_BOOKSTATISTIC +
                    " WHERE " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0 " +
                    " ORDER BY " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ") AS bs " +
                    " ON b." + PwsBookTable.COLUMN_ID + "=bs." + PwsBookStatisticTable.COLUMN_BOOKID +
                    " INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
                    "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
                    " INNER JOIN " + TABLE_PSALMS_FTS +  " AS p " +
                    " ON p.docid=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID;

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
