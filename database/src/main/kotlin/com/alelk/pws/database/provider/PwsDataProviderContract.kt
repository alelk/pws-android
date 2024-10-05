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
import android.net.Uri
import com.alelk.pws.database.BuildConfig
import com.alelk.pws.database.table.PwsBookStatisticTable
import com.alelk.pws.database.table.PwsBookTable
import com.alelk.pws.database.table.PwsPsalmFtsTable
import com.alelk.pws.database.table.PwsPsalmNumbersTable
import com.alelk.pws.database.table.PwsPsalmTable

/**
 * Pws Data Provider Contract
 *
 * Created by Alex Elkin on 21.05.2015.
 */
@Deprecated("use room db")
interface PwsDataProviderContract {
  object Psalms {
    const val COLUMN_ID = "_id"
    const val COLUMN_PSALMID = "psalm_id"
    const val COLUMN_PSALMNAME = "psalm_name"
    const val COLUMN_PSALMNUMBER = "psalm_number"
    const val PATH = "psalms"
    const val URI_MATCH = 20
    const val URI_MATCH_ID = 21

    internal object Suggestions {
      const val COLUMN_ID = "_id"
      const val COLUMN_PSALMNUMBER = Psalms.COLUMN_PSALMNUMBER
      const val COLUMN_PSALMID = Psalms.COLUMN_PSALMID
      const val PATH_NUMBER =
        PwsPsalmTable.TABLE_PSALMS + "/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/#"
      const val PATH_NAME =
        PwsPsalmTable.TABLE_PSALMS + "/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*"
      const val URI_MATCH_NUMBER = 25
      const val URI_MATCH_NAME = 26

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
  }

  object PsalmNumbers {
    const val PATH = "psalmnumbers"
    const val COLUMN_PSALMNUMBER = "psalm_number"
    const val COLUMN_BOOKID = "book_id"
    const val COLUMN_PSALMNUMBER_ID = "psalmnumberid"

    @JvmField
    val PROJECTION = arrayOf(
      "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as _id",
      "pn." + PwsPsalmNumbersTable.COLUMN_ID + " as " + COLUMN_PSALMNUMBER_ID,
      "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " as " + COLUMN_PSALMNUMBER,
      "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " as " + COLUMN_BOOKID
    )

    object Psalm {
      const val COLUMN_ID = "_id"
    }

  }

  object Books {
    const val COLUMN_BOOKEDITION = "book_edition"
    const val COLUMN_BOOKDISPLAYNAME = "book_display_name"
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
    const val COLUMN_BOOKDISPLAYNAME = Books.COLUMN_BOOKDISPLAYNAME
    const val COLUMN_BOOKEDITION = Books.COLUMN_BOOKEDITION
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
    val RAW_PROJECTION = arrayOf(
      "b." + PwsBookTable.COLUMN_ID
    )
    const val RAW_TABLES = TABLE_BOOKS_JOIN_BOOKSTATISTIC
    fun getRawSelection(bookEdition: String): String {
      return "b." + PwsBookTable.COLUMN_EDITION + " LIKE '" + bookEdition + "'"
    }
  }

  @Deprecated("use room")
  companion object : PwsDataProviderContract {
    const val SCHEME = "content"
    const val AUTHORITY = BuildConfig.DB_AUTHORITY
    const val TABLE_BOOKS_JOIN_BOOKSTATISTIC = PwsBookTable.TABLE_BOOKS + " as b " +
      " INNER JOIN " + PwsBookStatisticTable.TABLE_BOOKSTATISTIC + " as bs " +
      "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID
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
  }
}