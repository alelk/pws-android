package com.alelk.pws.database.query;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alex Elkin on 07.05.2015.
 */
public class PwsDatabaseQueryHelper extends PwsDatabaseQueryUtils {

    private final static String LOG_TAG = PwsDatabaseQueryHelper.class.getSimpleName();

    private final static String[] COLUMNS_BOOKEDTITION_PSALMNUMBER = {
            PwsBookTable.TABLE_BOOKS + "." + PwsBookTable.COLUMN_EDITION,
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "." + PwsPsalmNumbersTable.COLUMN_NUMBER};

    // books INNER JOIN psalmnumbers ON books._id=psalmnumbers.bookid
    private final static String TABLE_BOOKS_JOIN_PSALMNUMBERS = PwsBookTable.TABLE_BOOKS +
            " INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS +
            " ON " + PwsBookTable.TABLE_BOOKS + "." + PwsBookTable.COLUMN_ID + "=" +
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "." + PwsPsalmNumbersTable.COLUMN_BOOKID;

    // psalmnumbers.psalmid=?
    private final static String SELECTION_PSALMID =
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=?";

    private SQLiteDatabase mDatabase;
    private Cursor mCursor;

    public PwsDatabaseQueryHelper(SQLiteDatabase database) {
        this.mDatabase = database;
    }

    public Map<BookEdition, Integer> getPsalmNumbersByPsalmId(long psalmId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "getPsalmNumbersByPsalmId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Map<BookEdition, Integer> psalmNumbers = null;
        final String[] SELECTION_ARGS = new String[1];
        Arrays.asList(String.valueOf(psalmId)).toArray(SELECTION_ARGS);
        try {
            mCursor = mDatabase.query(TABLE_BOOKS_JOIN_PSALMNUMBERS,
                    COLUMNS_BOOKEDTITION_PSALMNUMBER,
                    SELECTION_PSALMID,
                    SELECTION_ARGS, null, null, null);
            if (mCursor.moveToFirst()) {
                psalmNumbers = new HashMap<>(mCursor.getCount());
                do {
                    psalmNumbers.put(BookEdition.getInstanceBySignature(mCursor.getString(0)), mCursor.getInt(1));
                } while (mCursor.moveToNext());
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return psalmNumbers;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
