package com.alelk.pws.pwsdb.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alelk.pws.pwsdb.data.BookEdition;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.data.entity.BookEntity;
import com.alelk.pws.pwsdb.data.entity.PsalmNumberEntity;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.pwsdb.exception.PwsDatabaseMessage;
import com.alelk.pws.pwsdb.exception.PwsDatabaseSourceIdExistsException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.*;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_BOOKID;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_ID;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_NUMBER;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_PSALMID;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class PwsDatabasePsalmNumberQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<Psalm, PsalmNumberEntity> {

    private static final String LOG_TAG = PwsDatabasePsalmNumberQuery.class.getSimpleName();

    private SQLiteDatabase mDatabase;
    private Long mPsalmId;
    private BookEdition mBookEdition;
    private Cursor mCursor;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_PSALMID,
            COLUMN_BOOKID,
            COLUMN_NUMBER };


    public PwsDatabasePsalmNumberQuery(SQLiteDatabase database, Long psalmId, BookEdition bookEdition) {
        this.mDatabase = database;
        this.mBookEdition = bookEdition;
        this.mPsalmId = psalmId;
    }

    /**
     * Inserts the Psalm Number to Pws Database.
     * @param psalm the Pws Psalm object. Should contain the psalm number for book edition specified
     *              with constructor.
     * @return PsalmNumberEntity inserted to mDatabase.
     * @throws PwsDatabaseIncorrectValueException if incorrect values are specified. Contains the
     * following PwsDatabaseMessage's:
     * NULL_DATABASE_VALUE if the mDatabase value is null,
     * NULL_PSALM_ID_VALUE if the psalm id value is null,
     * NULL_BOOK_EDITION_VALUE if book edition value is null,
     * NULL_PSALM_VALUE if the psalm value is null,
     * NO_BOOK_EDITION_FOUND if book edition is specified, but no book found with this book edition,
     * UNEXPECTED_BOOK_EDITION_VALUE if if book edition is specified, but the psalm param has no psalm number for this book edition.
     * @throws PwsDatabaseSourceIdExistsException if psalm number already exists for specified book edition
     */
    @Override
    public PsalmNumberEntity insert(Psalm psalm) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validatePsalmIdNotNull(METHOD_NAME, mPsalmId);
        validateBookEditionNotNull(METHOD_NAME, mBookEdition);
        validatePsalmNotNull(METHOD_NAME, psalm);

        PsalmNumberEntity psalmNumberEntity;
        BookEntity bookEntity = new PwsDatabaseBookQuery(mDatabase).selectByEdition(mBookEdition);
        if (bookEntity == null) {
            Log.d(LOG_TAG, METHOD_NAME + ": No book found by book edition: bookEditon=" + mBookEdition);
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NO_BOOK_EDITION_FOUND);
        }
        final long bookId = bookEntity.getId();
        if (psalm.getNumber(mBookEdition) != null) {
            final long number = psalm.getNumber(mBookEdition);
            psalmNumberEntity = selectByNumberAndBookId(number, bookId);
            if (psalmNumberEntity != null) {
                if (psalmNumberEntity.getPsalmId() != mPsalmId) {
                    Log.d(LOG_TAG, METHOD_NAME + ": Psalm number already exists for specified book, " +
                            "but has different mPsalmId " +
                            "(mBookEdition='" + mBookEdition + "' expected mPsalmId='" + mPsalmId +
                            "'): " + psalmNumberEntity);
                    throw new PwsDatabaseSourceIdExistsException(PwsDatabaseMessage.PSALM_NUMBER_EXISTS_FOR_BOOK_ID, psalmNumberEntity.getId());
                }
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm number already exists. No need insert to mDatabase.");
            } else {
                final ContentValues contentValues = new ContentValues();
                fillContentValues(contentValues, mPsalmId, bookId, number);
                final long id = mDatabase.insert(TABLE_PSALMNUMBERS, null, contentValues);
                psalmNumberEntity = selectById(id);
                Log.v(LOG_TAG, METHOD_NAME + ": New psalm number added: " + psalmNumberEntity);
            }
        } else {
            Log.d(LOG_TAG, METHOD_NAME + ": Incorrect book edition value: '" + mBookEdition +
                    "'. No psalm numbers found for this book edition.");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.UNEXPECTED_BOOK_EDITION_VALUE);
        }
        return psalmNumberEntity;
    }

    @Override
    public PsalmNumberEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        PsalmNumberEntity psalmNumberEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_PSALMNUMBERS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                psalmNumberEntity = cursorToPsalmNumberEntity(mCursor);
            }
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm number selected: " + psalmNumberEntity);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return psalmNumberEntity;
    }

    public PsalmNumberEntity selectByNumber(long psalmNumber) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumber";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validateBookEditionNotNull(METHOD_NAME, mBookEdition);
        BookEntity bookEntity = new PwsDatabaseBookQuery(mDatabase).selectByEdition(mBookEdition);
        if (bookEntity == null) {
            Log.d(LOG_TAG, METHOD_NAME + ": No book entity selected for book edition '" + mBookEdition + "'");
            // TODO: 20.02.2016 throw exception
            return null;
        }
        PsalmNumberEntity psalmNumberEntity = selectByNumberAndBookId(psalmNumber, bookEntity.getId());
        return psalmNumberEntity;
    }

    public PsalmNumberEntity selectByNumberAndBookId(long psalmNumber, long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumberAndBookId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        PsalmNumberEntity psalmNumberEntity = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(String.valueOf(psalmNumber), String.valueOf(bookId)).toArray(SELECTION_ARGS);
        try {
            mCursor = mDatabase.query(TABLE_PSALMNUMBERS, ALL_COLUMNS,
                    COLUMN_NUMBER + "=? AND " + COLUMN_BOOKID + "=?", SELECTION_ARGS, null, null, "1");
            if (mCursor.moveToFirst()) {
                psalmNumberEntity = cursorToPsalmNumberEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm number selected: " + psalmNumberEntity);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return psalmNumberEntity;
    }

    public Set<PsalmNumberEntity> selectByBookId(long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<PsalmNumberEntity> psalmNumberEntities = null;
        try {
            mCursor = mDatabase.query(TABLE_PSALMNUMBERS, ALL_COLUMNS, COLUMN_BOOKID + " = " + bookId, null, null, null, null);
            if (mCursor.moveToFirst()) {
                psalmNumberEntities = new HashSet<>(mCursor.getCount());
                do {
                    psalmNumberEntities.add(cursorToPsalmNumberEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of psalm numbers selected for bookId=" + bookId + ": " + psalmNumberEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm numbers selected for bookId=" + bookId);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return  psalmNumberEntities;
    }

    private PsalmNumberEntity cursorToPsalmNumberEntity(Cursor cursor) {
        PsalmNumberEntity psalmNumberEntity = new PsalmNumberEntity();
        psalmNumberEntity.setId(cursor.getLong(0));
        psalmNumberEntity.setPsalmId(cursor.getLong(1));
        psalmNumberEntity.setBookId(cursor.getLong(2));
        psalmNumberEntity.setNumber(cursor.getLong(3));
        return psalmNumberEntity;
    }

    private void fillContentValues(ContentValues values, long psalmId, long bookId, long number) {
        values.put(COLUMN_PSALMID, psalmId);
        values.put(COLUMN_BOOKID, bookId);
        values.put(COLUMN_NUMBER, number);
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
