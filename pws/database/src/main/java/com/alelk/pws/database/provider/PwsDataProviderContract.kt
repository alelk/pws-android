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
package com.alelk.pws.database.provider

import android.app.SearchManager
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.alelk.pws.database.BuildConfig
import com.alelk.pws.database.table.PwsBookStatisticTable
import com.alelk.pws.database.table.PwsBookTable
import com.alelk.pws.database.table.PwsFavoritesTable
import com.alelk.pws.database.table.PwsHistoryTable
import com.alelk.pws.database.table.PwsPsalmFtsTable
import com.alelk.pws.database.table.PwsPsalmNumbersTable
import com.alelk.pws.database.table.PwsPsalmPsalmReferencesTable
import com.alelk.pws.database.table.PwsPsalmTable

/**
 * Pws Data Provider Contract
 *
 * Created by Alex Elkin on 21.05.2015.
 */
interface PwsDataProviderContract {
  object Psalms {
    const val COLUMN_ID = "_id"
    const val COLUMN_PSALMID = "psalm_id"
    const val COLUMN_PSALMNAME = "psalm_name"
    const val COLUMN_PSALMTEXT = "psalm_text"
    const val COLUMN_PSALMANNOTATION = "psalm_annotation"
    const val COLUMN_PSALMAUTHOR = "psalm_author"
    const val COLUMN_PSALMCOMPOSER = "psalm_composer"
    const val COLUMN_PSALMTRANSLATOR = "psalm_translator"
    const val COLUMN_PSALMTONALITIES = "psalm_tonalities"
    const val COLUMN_PSALMNUMBER = "psalm_number"
    const val COLUMN_PSALMLOCALE = "psalm_locale"
    const val PATH = "psalms"
    const val PATH_ID = PATH + "/#"
    const val URI_MATCH = 20
    const val URI_MATCH_ID = 21
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()
    @JvmStatic
    fun getContentUri(psalmId: Long): Uri {
      return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path("$PATH/$psalmId").build()
    }

    internal object Suggestions {
      const val COLUMN_ID = "_id"
      const val COLUMN_PSALMNUMBER = Psalms.COLUMN_PSALMNUMBER
      const val COLUMN_PSALMID = Psalms.COLUMN_PSALMID
      const val PATH = PwsPsalmTable.TABLE_PSALMS + "/" + SearchManager.SUGGEST_URI_PATH_QUERY
      const val PATH_NUMBER =
        PwsPsalmTable.TABLE_PSALMS + "/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/#"
      const val PATH_NAME =
        PwsPsalmTable.TABLE_PSALMS + "/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*"
      const val URI_MATCH_NUMBER = 25
      const val URI_MATCH_NAME = 26
      val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()

      // Suggestions for psalm number
      const val SGNUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC
      const val SGNUM_SORT_ORDER =
        "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC"
      val SGNUM_PROJECTION = arrayOf(
        "p." + PwsPsalmTable.COLUMN_ID + " as " + COLUMN_ID,
        "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
        "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ', ') as " + SearchManager.SUGGEST_COLUMN_TEXT_2,
        "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
        "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_PSALMID,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
      )

      fun getSgNumberSelection(psalmNumber: String): String {
        return COLUMN_PSALMNUMBER + "=" + psalmNumber + " and bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0"
      }

      // Suggestions for psalm name
      const val SG_NAME_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED
      val SG_NAME_PROJECTION = arrayOf(
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
        "p." + PwsPsalmTable.COLUMN_NAME + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1,
        "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ' | ') as " + SearchManager.SUGGEST_COLUMN_TEXT_2,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
      )
      const val SG_NAME_GROUPBY = "p.docid"
      fun getSgNameSelection(searchText: String): String {
        return "p." + PwsPsalmTable.COLUMN_NAME + " MATCH '" + searchText.trim { it <= ' ' }
          .replace("\\s+".toRegex(), "*") + "*'"
      }

      // Suggestions for psalm text
      const val SG_TXT_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED
      val SG_TXT_PROJECTION = arrayOf(
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
        "p." + PwsPsalmTable.COLUMN_NAME + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1,
        "group_concat(b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + ", ' | ') as " + SearchManager.SUGGEST_COLUMN_TEXT_2,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
      )
      const val SG_TXT_GROUPBY = "p.docid"
      fun getSgTextSelection(searchText: String): String {
        return PwsPsalmFtsTable.TABLE_PSALMS_FTS + " MATCH '" + searchText.trim { it <= ' ' }
          .replace("\\s+".toRegex(), "*") + "*'"
      }
    }

    object Search {
      const val COLUMN_ID = "_id"
      const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
      const val COLUMN_PSALMNUMBER_ID =
        PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID
      const val COLUMN_PSALMNUMBER = PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER
      const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
      const val COLUMN_SNIPPET = "snippet"
      const val COLUMN_MATCHINFO = "matchinfo"
      const val PATH = PwsPsalmTable.TABLE_PSALMS + "/search"
      const val URI_MATCH = 27

      @JvmField
      val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()

      /** Search number  */
      const val S_NUM_TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED
      const val S_NUM_ORDER_BY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " DESC"
      val S_NUM_PROJECTION = arrayOf(
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID,
        "p." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
        "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
        "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
        "substr(p." + PwsPsalmTable.COLUMN_TEXT + ", 1, 100) as " + COLUMN_SNIPPET
      )

      fun getSNumSelection(psalmNumber: Int): String {
        return COLUMN_PSALMNUMBER + "=" + psalmNumber
      }

      /** Search text  */
      const val S_TXT_TABLES = TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED
      const val S_TXT_ORDER_BY = COLUMN_MATCHINFO + " DESC"
      val S_TXT_PROJECTION = arrayOf(
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID,
        "p." + PwsPsalmTable.COLUMN_NAME + " as " + COLUMN_PSALMNAME,
        "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
        "b." + PwsBookTable.COLUMN_DISPLAYNAME + " as " + COLUMN_BOOKDISPLAYNAME,
        "snippet(" + PwsPsalmFtsTable.TABLE_PSALMS_FTS + ", '<b><font color=#247b34>', '</font></b>', '...') as " + COLUMN_SNIPPET,
        "matchinfo(" + PwsPsalmFtsTable.TABLE_PSALMS_FTS + ", 'x') as " + COLUMN_MATCHINFO
      )

      fun getSTxtSelection(searchText: String): String {
        return PwsPsalmFtsTable.TABLE_PSALMS_FTS + " match '" + searchText + "'"
      }
    }

    internal object PsalmNumbers {
      const val PATH =
        PwsPsalmTable.TABLE_PSALMS + "/#/" + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS
      const val PATH_ID =
        PwsPsalmTable.TABLE_PSALMS + "/#/" + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "/#"
      const val URI_MATCH = 28
      const val URI_MATCH_ID = 29
    }
  }

  object PsalmNumbers {
    const val PATH = "psalmnumbers"
    const val COLUMN_PSALMNUMBER = "psalm_number"
    const val COLUMN_BOOKID = "book_id"
    const val COLUMN_PSALMNUMBER_ID = "psalmnumberid"
    const val PATH_ID = PATH + "/#"
    const val URI_MATCH = 30
    const val URI_MATCH_ID = 31
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()

    @JvmField
    val PROJECTION = arrayOf(
      "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as _id",
      "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
      "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
      "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " as " + COLUMN_BOOKID
    )

    object Psalm {
      const val PATH_SEGMENT = "psalm"
      const val COLUMN_ID = "_id"
      const val COLUMN_PSALMID = Psalms.COLUMN_PSALMID
      const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
      const val COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT
      const val COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION
      const val COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR
      const val COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER
      const val COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR
      const val COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES
      const val COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE
      const val COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID
      const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
      const val COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID
      const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
      const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
      const val COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME
      const val PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT
      const val URI_MATCH = 32
      const val DEFAULT_SELECTION = COLUMN_PSALMNUMBER_ID + "=?"
      const val TABLES = TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS
      val PROJECTION = arrayOf(
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
        "pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + COLUMN_PSALMID,
        "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
        "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
        "b." + PwsBookTable.COLUMN_DISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME
      )
      val PROJECTION_PSALM_TEXT = arrayOf(
        "p." + PwsPsalmTable.COLUMN_ID + " AS " + COLUMN_ID,
        "p." + PwsPsalmTable.COLUMN_TEXT + " AS " + COLUMN_PSALMTEXT,
        "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + COLUMN_PSALMNUMBER_ID
      )

      @JvmStatic
      fun getContentUri(psalmNumberId: Long): Uri {
        return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
          PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT
        ).build()
      }
    }

    object Book {
      const val PATH_SEGMENT = "book"
      const val PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT

      /**
       * Provider contract for URI: psalmnumbers/#/book/bookpsalmnumbers
       */
      object BookPsalmNumbers {
        const val PATH_SEGMENT = "bookpsalmnumbers"
        const val PATH = Book.PATH + "/" + PATH_SEGMENT
        const val URI_MATCH = 331
        const val COLUMN_ID = "_id"
        const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
        fun getContentUri(psalmNumberId: Long): Uri {
          return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
            PsalmNumbers.PATH + "/" + psalmNumberId + "/" +
              Book.PATH_SEGMENT + "/" +
              PATH_SEGMENT
          ).build()
        }

        val PROJECTION = arrayOf(
          "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
          "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_ID
        )
        const val ORDER_BY = "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER
        fun buildRawTables(psalmNumberId: Long): String {
          return "(" + SQLiteQueryBuilder.buildQueryString(
            false,
            TABLE_PSALMNUMBERS_JOIN_BOOKS, arrayOf("b." + PwsBookTable.COLUMN_ID),
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + "=" + psalmNumberId,
            null, null,
            null,
            "1"
          ) + ") as b inner join " +
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " as pn on " +
            "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " = b._id"
        }

        object Info {
          const val PATH_SEGMENT = "info"
          const val PATH = BookPsalmNumbers.PATH + "/" + PATH_SEGMENT
          const val URI_MATCH = 3311
          const val COLUMN_COUNT_OF_ITEMS = "count_of_items"
          const val COLUMN_MAX_PSALMNUMBER = "max_psalm_number"
          const val COLUMN_MIN_PSALMNUMBER = "min_psalm_number"
          const val COLUMN_PSALMNUMBERID_LIST = "psalmnumberids_list"
          const val COLUMN_PSALMNUMBER_LIST = "psalm_numbers_list"

          @JvmStatic
          fun getContentUri(psalmNumberId: Long): Uri {
            return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
              PsalmNumbers.PATH + "/" + psalmNumberId + "/" +
                Book.PATH_SEGMENT + "/" +
                BookPsalmNumbers.PATH_SEGMENT + "/" +
                PATH_SEGMENT
            ).build()
          }

          fun buildRawTables(psalmNumberId: Long): String {
            return "(" + SQLiteQueryBuilder.buildQueryString(
              false,
              BookPsalmNumbers.buildRawTables(psalmNumberId),
              BookPsalmNumbers.PROJECTION,
              null, null, null, ORDER_BY, null
            ) + ")"
          }

          val PROJECTION_PSALMNUMBER_ID = arrayOf(
            "max (" + COLUMN_PSALMNUMBER + ") as " + COLUMN_MAX_PSALMNUMBER,
            "group_concat (" + COLUMN_ID + ") as " + COLUMN_PSALMNUMBERID_LIST,
            "count (" + COLUMN_ID + ") as " + COLUMN_COUNT_OF_ITEMS
          )

          @JvmField
          val PROJECTION = arrayOf(
            "max (" + COLUMN_PSALMNUMBER + ") as " + COLUMN_MAX_PSALMNUMBER,
            "min (" + COLUMN_PSALMNUMBER + ") as " + COLUMN_MIN_PSALMNUMBER,
            "group_concat (" + COLUMN_ID + ") as " + COLUMN_PSALMNUMBERID_LIST,
            "group_concat (" + COLUMN_PSALMNUMBER + ") as " + COLUMN_PSALMNUMBER_LIST,
            "count (" + COLUMN_ID + ") as " + COLUMN_COUNT_OF_ITEMS
          )
        }
      }
    }

    object ReferencePsalms {
      const val PATH_SEGMENT = "referencepsalms"
      const val COLUMN_ID = "_id"
      const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
      const val COLUMN_PSALM_ID = Psalms.COLUMN_PSALMID
      const val COLUMN_PSALMREF_VOLUME = "reference_psalms_volume"
      const val COLUMN_PSALMREF_REASON = "reference_psalms_reason"
      const val COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID
      private const val COLUMN_CURRENT_PSALMNUMBER_ID = "current_psalmnumber_id"
      const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
      const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
      const val COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME
      const val PATH = PsalmNumbers.PATH + "/#/" + PATH_SEGMENT
      const val URI_MATCH = 34
      private const val RAW_TABLES_ALIAS = "refs"
      val PROJECTION = arrayOf(
        RAW_TABLES_ALIAS + "." + COLUMN_PSALMNUMBER_ID + " AS " + COLUMN_PSALMNUMBER_ID,
        RAW_TABLES_ALIAS + "." + COLUMN_ID + " AS " + COLUMN_ID,
        RAW_TABLES_ALIAS + "." + COLUMN_PSALMNUMBER + " AS " + COLUMN_PSALMNUMBER,
        RAW_TABLES_ALIAS + "." + COLUMN_PSALMNAME + " AS " + COLUMN_PSALMNAME,
        RAW_TABLES_ALIAS + "." + COLUMN_PSALM_ID + " AS " + COLUMN_PSALM_ID,
        RAW_TABLES_ALIAS + "." + COLUMN_BOOKDISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME,
        RAW_TABLES_ALIAS + "." + COLUMN_BOOKDISPLAYSHORTNAME + " AS " + COLUMN_BOOKDISPLAYSHORTNAME,
        RAW_TABLES_ALIAS + "." + COLUMN_PSALMREF_REASON + " AS " + COLUMN_PSALMREF_REASON,
        RAW_TABLES_ALIAS + "." + COLUMN_PSALMREF_VOLUME + " AS " + COLUMN_PSALMREF_VOLUME
      )

      @JvmStatic
      fun getContentUri(psalmNumberId: Long): Uri {
        return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(
          PsalmNumbers.PATH + "/" + psalmNumberId + "/" + PATH_SEGMENT
        ).build()
      }

      fun buildRawTables(currentPsalmNumberId: Long): String {
        return "(" + SQLiteQueryBuilder.buildQueryString(
          false,
          TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED_JOIN_REFERENCEPSALMS,
          arrayOf(
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
          ),
          COLUMN_CURRENT_PSALMNUMBER_ID + "=" + currentPsalmNumberId,
          null,
          null,
          "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE,
          null
        ) + ") as " + RAW_TABLES_ALIAS
      }
    }
  }

  object Favorites {
    const val COLUMN_ID = "_id"
    internal const val COLUMN_FAVORITEID = "favorite_id"
    const val COLUMN_FAVORITEPOSITION = "favorite_position"
    const val COLUMN_PSALMID = Psalms.COLUMN_PSALMID
    const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
    const val COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT
    const val COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION
    const val COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE
    const val COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR
    const val COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER
    const val COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR
    const val COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES
    const val COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID
    const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
    const val COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID
    const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
    const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
    const val PATH = "favorites"
    const val PATH_ID = PATH + "/#"
    const val URI_MATCH = 40
    const val URI_MATCH_ID = 41

    @JvmField
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()
    const val TABLES = TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS
    const val SORT_ORDER = COLUMN_FAVORITEPOSITION + " DESC"
    const val GROUP_BY = COLUMN_FAVORITEPOSITION
    const val SELECTION_ID_MATCH = COLUMN_ID + " match ?"
    val PROJECTION = arrayOf(
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
    )
  }

  object History {
    const val COLUMN_ID = "_id"
    const val COLUMN_PSALMID = Psalms.COLUMN_PSALMID
    const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
    const val COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT
    const val COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION
    const val COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR
    const val COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER
    const val COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR
    const val COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES
    const val COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE
    const val COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID
    const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
    const val COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID
    const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
    const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
    const val COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME
    internal const val COLUMN_HISTORYID = "history_id"
    const val COLUMN_HISTORYTIMESTAMP = "history_timestamp"
    const val PATH = "history"
    internal const val PATH_ID = PATH + "/#"
    internal const val URI_MATCH = 50
    internal const val URI_MATCH_ID = 51

    @JvmField
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()
    internal const val TABLES = TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS
    internal const val SORT_ORDER = COLUMN_HISTORYTIMESTAMP + " DESC"
    internal const val GROUP_BY = "h." + PwsHistoryTable.COLUMN_ID
    internal val PROJECTION = arrayOf(
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
    )

    @JvmStatic
    fun getContentUri(limit: Int): Uri {
      return Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH)
        .appendQueryParameter(
          QUERY_PARAMETER_LIMIT, limit.toString()
        ).build()
    }

    object Last {
      const val COLUMN_ID = Psalms.COLUMN_ID
      const val COLUMN_PSALMNAME = Psalms.COLUMN_PSALMNAME
      const val COLUMN_PSALMTEXT = Psalms.COLUMN_PSALMTEXT
      const val COLUMN_PSALMANNOTATION = Psalms.COLUMN_PSALMANNOTATION
      const val COLUMN_PSALMAUTHOR = Psalms.COLUMN_PSALMAUTHOR
      const val COLUMN_PSALMCOMPOSER = Psalms.COLUMN_PSALMCOMPOSER
      const val COLUMN_PSALMTRANSLATOR = Psalms.COLUMN_PSALMTRANSLATOR
      const val COLUMN_PSALMTONALITIES = Psalms.COLUMN_PSALMTONALITIES
      const val COLUMN_PSALMLOCALE = Psalms.COLUMN_PSALMLOCALE
      const val COLUMN_PSALMNUMBER_ID = PsalmNumbers.COLUMN_PSALMNUMBER_ID
      const val COLUMN_PSALMNUMBER = PsalmNumbers.COLUMN_PSALMNUMBER
      const val COLUMN_BOOKID = PsalmNumbers.COLUMN_BOOKID
      const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
      const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
      const val COLUMN_HISTORYTIMESTAMP = History.COLUMN_HISTORYTIMESTAMP
      const val COLUMN_HISTORYID = History.COLUMN_HISTORYID
      const val PATH = History.PATH + "/last"
      internal const val URI_MATCH = 53

      @JvmField
      val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()
      internal const val TABLES = History.TABLES
      internal const val SORT_ORDER = COLUMN_HISTORYTIMESTAMP + " DESC"
      internal const val LIMIT = "1"
      internal val PROJECTION = arrayOf(
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
      )
    }
  }

  object Books {
    const val COLUMN_BOOKID = "book_id"
    const val COLUMN_BOOKEDITION = "book_edition"
    const val COLUMN_BOOKDISPLAYNAME = "book_display_name"
    const val COLUMN_BOOKDISPLAYSHORTNAME = "book_display_short_name"
    const val PATH = "books"
    const val URI_MATCH = 70
    const val COLUMN_DISPLAY_NAME = PwsBookTable.COLUMN_DISPLAYNAME
    const val COLUMN_DISPLAY_SHORT_NAME = PwsBookTable.COLUMN_DISPLAYSHORTNAME
    const val PSALM_NUMBER_ID = "psalmId"

    const val TABLE = "${PwsBookTable.TABLE_BOOKS} as b " +
      "inner join ${PwsBookStatisticTable.TABLE_BOOKSTATISTIC} as bs " +
      "on b.${PwsBookTable.COLUMN_ID} = bs.${PwsBookStatisticTable.COLUMN_BOOKID} " +
      "inner join ${PwsPsalmNumbersTable.TABLE_PSALMNUMBERS} as pn " +
      "on b.${PwsBookTable.COLUMN_ID} = pn.${PwsPsalmNumbersTable.COLUMN_BOOKID}"
    const val ACTIVE = "bs.${PwsBookStatisticTable.COLUMN_USERPREFERENCE} > 0"
    const val FIRST_SONG = "pn.${PwsPsalmNumbersTable.COLUMN_NUMBER} = 1"
    const val SORTED = "bs.${PwsBookStatisticTable.COLUMN_USERPREFERENCE} desc"

    @JvmField
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()

    @JvmField
    val PROJECTION = arrayOf(
      "b.${PwsBookTable.COLUMN_ID} as ${PwsBookTable.COLUMN_ID}",
      "b.${PwsBookTable.COLUMN_DISPLAYNAME} as $COLUMN_DISPLAY_NAME",
      "b.${PwsBookTable.COLUMN_DISPLAYSHORTNAME} as $COLUMN_DISPLAY_SHORT_NAME",
      "bs.${PwsBookStatisticTable.COLUMN_USERPREFERENCE} as ${PwsBookStatisticTable.COLUMN_USERPREFERENCE}",
      "pn.${PwsPsalmNumbersTable.COLUMN_NUMBER} as ${PwsPsalmNumbersTable.COLUMN_NUMBER}",
      "pn.${PwsPsalmNumbersTable.COLUMN_ID} as ${PSALM_NUMBER_ID}"
    )
  }

  object BookStatistic {
    const val COLUMN_BOOKSTATISTIC_ID = "bookstatistic_id"
    const val COLUMN_BOOKSTATISTIC_PREFERENCE = "bookstatistic_userpref"
    const val COLUMN_BOOKSTATISTIC_READINGS = "bookstatistic_readings"
    const val COLUMN_BOOKSTATISTIC_RAITING = "bookstatistic_raiting"
    const val COLUMN_BOOKID = Books.COLUMN_BOOKID
    const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
    const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
    const val COLUMN_BOOKDISPLAYSHORTNAME = Books.COLUMN_BOOKDISPLAYSHORTNAME
    const val PATH = "bookstatistic"
    const val PATH_ID = PATH + "/#"
    const val PATH_TEXT = PATH + "/*"
    const val URI_MATCH = 60
    const val URI_MATCH_ID = 61
    const val URI_MATCH_TEXT = 62

    @JvmField
    val CONTENT_URI = Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH).build()

    @JvmStatic
    fun getBookStatisticBookEditionUri(bookEdition: String?): Uri {
      return CONTENT_URI.buildUpon().appendPath(bookEdition).build()
    }

    const val TABLES = TABLE_BOOKS_JOIN_BOOKSTATISTIC
    const val SORT_ORDER = COLUMN_BOOKSTATISTIC_PREFERENCE + " DESC"
    val PROJECTION = arrayOf(
      "bs." + PwsBookStatisticTable.COLUMN_ID + " AS " + COLUMN_BOOKSTATISTIC_ID,
      "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " AS " + COLUMN_BOOKSTATISTIC_PREFERENCE,
      "bs." + PwsBookStatisticTable.COLUMN_READINGS + " AS " + COLUMN_BOOKSTATISTIC_READINGS,
      "bs." + PwsBookStatisticTable.COLUMN_RATING + " AS " + COLUMN_BOOKSTATISTIC_RAITING,
      "b." + PwsBookTable.COLUMN_EDITION + " AS " + COLUMN_BOOKEDITION,
      "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + COLUMN_BOOKDISPLAYNAME
    )
    const val SELECTION_PREFERRED_BOOKS_ONLY = COLUMN_BOOKSTATISTIC_PREFERENCE + " > 0"
    val RAW_PROJECTION = arrayOf(
      "b." + PwsBookTable.COLUMN_ID
    )
    const val RAW_TABLES = TABLE_BOOKS_JOIN_BOOKSTATISTIC
    fun getRawSelection(bookEdition: String): String {
      return "b." + PwsBookTable.COLUMN_EDITION + " LIKE '" + bookEdition + "'"
    }
  }

  companion object : PwsDataProviderContract {
    const val SCHEME = "content"
    const val AUTHORITY = BuildConfig.DB_AUTHORITY
    const val QUERY_PARAMETER_LIMIT = "limit"
    const val HISTORY_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val TABLE_BOOKS_JOIN_BOOKSTATISTIC = PwsBookTable.TABLE_BOOKS + " as b " +
      " INNER JOIN " + PwsBookStatisticTable.TABLE_BOOKSTATISTIC + " as bs " +
      "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID
    const val TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS =
      PwsPsalmTable.TABLE_PSALMS + " AS p " +
        "INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
        " INNER JOIN " + PwsBookTable.TABLE_BOOKS + " as b " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID
    const val TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC =
      PwsPsalmTable.TABLE_PSALMS + " AS p " +
        "INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
        " INNER JOIN " + PwsBookTable.TABLE_BOOKS + " as b " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
        " INNER JOIN " + PwsBookStatisticTable.TABLE_BOOKSTATISTIC + " as bs " +
        "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID
    const val TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED =
      PwsBookTable.TABLE_BOOKS + " AS b " +
        " INNER JOIN (SELECT " +
        PwsBookStatisticTable.COLUMN_BOOKID +
        ", " + PwsBookStatisticTable.COLUMN_USERPREFERENCE +
        " FROM " + PwsBookStatisticTable.TABLE_BOOKSTATISTIC +
        " WHERE " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0 " +
        " ORDER BY " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ") AS bs " +
        " ON b." + PwsBookTable.COLUMN_ID + "=bs." + PwsBookStatisticTable.COLUMN_BOOKID +
        " INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
        " INNER JOIN " + PwsPsalmTable.TABLE_PSALMS + " AS p " +
        " ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID
    const val TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED_JOIN_REFERENCEPSALMS =
      TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED +
        " INNER JOIN " + PwsPsalmPsalmReferencesTable.TABLE_PSALMPSALMREFERENCES + " AS ppr " +
        "ON p." + PwsPsalmTable.COLUMN_ID + " = ppr." + PwsPsalmPsalmReferencesTable.COLUMN_REFPSALMID +
        " INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS cpn " +
        "ON cpn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " = ppr." + PwsPsalmPsalmReferencesTable.COLUMN_PSALMID
    const val TABLE_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_USERPREFERRED =
      PwsBookTable.TABLE_BOOKS + " AS b " +
        " INNER JOIN (SELECT " +
        PwsBookStatisticTable.COLUMN_BOOKID +
        " FROM " + PwsBookStatisticTable.TABLE_BOOKSTATISTIC +
        " WHERE " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0 " +
        " ORDER BY " + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ") AS bs " +
        " ON b." + PwsBookTable.COLUMN_ID + "=bs." + PwsBookStatisticTable.COLUMN_BOOKID +
        " INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
        " INNER JOIN " + PwsPsalmFtsTable.TABLE_PSALMS_FTS + " AS p " +
        " ON p.docid=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID
    const val TABLE_PSALMNUMBERS_JOIN_BOOKS =
      PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "INNER JOIN " + PwsBookTable.TABLE_BOOKS + " as b " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID
    const val TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS =
      PwsHistoryTable.TABLE_HISTORY + " AS h " +
        "INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON h." + PwsHistoryTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
        " INNER JOIN " + PwsBookTable.TABLE_BOOKS + " as b " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
        " INNER JOIN " + PwsPsalmTable.TABLE_PSALMS + " as p " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID
    const val TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS =
      PwsFavoritesTable.TABLE_FAVORITES + " AS f " +
        "INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " AS pn " +
        "ON f." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
        " INNER JOIN " + PwsBookTable.TABLE_BOOKS + " as b " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
        " INNER JOIN " + PwsPsalmTable.TABLE_PSALMS + " as p " +
        "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID
  }
}