package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import static com.alelk.pws.database.table.PwsPsalmNumbersTable.*;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.data.entity.PsalmNumberEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseMessage;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.exception.PwsDatabaseSourceNotFoundException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class PwsDatabasePsalmNumberQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<Psalm, PsalmNumberEntity> {

    private static final String LOG_TAG = PwsDatabasePsalmNumberQuery.class.getSimpleName();

    private SQLiteDatabase database;
    private Long psalmId;
    private BookEdition bookEdition;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_PSALMID,
            COLUMN_BOOKID,
            COLUMN_NUMBER };


    public PwsDatabasePsalmNumberQuery(SQLiteDatabase database, Long psalmId, BookEdition bookEdition) {
        this.database = database;
        this.bookEdition = bookEdition;
        this.psalmId = psalmId;
    }

    /**
     * Inserts the Psalm Number to Pws Database.
     * @param psalm the Pws Psalm object. Should contain the psalm number for book edition specified
     *              with constructor.
     * @return PsalmNumberEntity inserted to database.
     * @throws PwsDatabaseIncorrectValueException if incorrect values are specified. Contains the
     * following PwsDatabaseMessage's:
     * NULL_DATABASE_VALUE if the database value is null,
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
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmIdNotNull(METHOD_NAME, psalmId);
        validateBookEditionNotNull(METHOD_NAME, bookEdition);
        validatePsalmNotNull(METHOD_NAME, psalm);

        PsalmNumberEntity psalmNumberEntity;
        BookEntity bookEntity = new PwsDatabaseBookQuery(database).selectByEdition(bookEdition);
        if (bookEntity == null) {
            Log.d(LOG_TAG, METHOD_NAME + ": No book found by book edition: bookEditon=" + bookEdition);
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NO_BOOK_EDITION_FOUND);
        }
        final long bookId = bookEntity.getId();
        if (psalm.getNumber(bookEdition) != null) {
            final long number = psalm.getNumber(bookEdition);
            psalmNumberEntity = selectByNumberAndBookId(number, bookId);
            if (psalmNumberEntity != null) {
                if (psalmNumberEntity.getPsalmId() != psalmId) {
                    Log.d(LOG_TAG, METHOD_NAME + ": Psalm number already exists for specified book, " +
                            "but has different psalmId " +
                            "(bookEdition='" + bookEdition + "' expected psalmId='" + psalmId +
                            "'): " + psalmNumberEntity);
                    throw new PwsDatabaseSourceIdExistsException(PwsDatabaseMessage.PSALM_NUMBER_EXISTS_FOR_BOOK_ID, psalmNumberEntity.getId());
                }
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm number already exists. No need insert to database.");
            } else {
                final ContentValues contentValues = new ContentValues();
                fillContentValues(contentValues, psalmId, bookId, number);
                final long id = database.insert(TABLE_PSALMNUMBERS, null, contentValues);
                psalmNumberEntity = selectById(id);
                Log.v(LOG_TAG, METHOD_NAME + ": New psalm number added: " + psalmNumberEntity);
            }
        } else {
            Log.d(LOG_TAG, METHOD_NAME + ": Incorrect book edition value: '" + bookEdition +
                    "'. No psalm numbers found for this book edition.");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.UNEXPECTED_BOOK_EDITION_VALUE);
        }
        return psalmNumberEntity;
    }

    @Override
    public PsalmNumberEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        PsalmNumberEntity psalmNumberEntity = null;
        Cursor cursor = database.query(TABLE_PSALMNUMBERS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            psalmNumberEntity = cursorToPsalmNumberEntity(cursor);
        }
        Log.v(LOG_TAG, METHOD_NAME + ": Psalm number selected: " + psalmNumberEntity);
        return psalmNumberEntity;
    }

    public PsalmNumberEntity selectByNumberAndBookId(long psalmNumber, long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumberAndBookId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        PsalmNumberEntity psalmNumberEntity = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(String.valueOf(psalmNumber), String.valueOf(bookId)).toArray(SELECTION_ARGS);
        Cursor cursor = database.query(TABLE_PSALMNUMBERS, ALL_COLUMNS,
                COLUMN_NUMBER + "=? AND " + COLUMN_BOOKID + "=?", SELECTION_ARGS, null, null, "1");
        if (cursor.moveToFirst()) {
            psalmNumberEntity = cursorToPsalmNumberEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm number selected: " + psalmNumberEntity);
        }
        return psalmNumberEntity;
    }

    public Set<PsalmNumberEntity> selectByBookId(long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        Set<PsalmNumberEntity> psalmNumberEntities = null;
        Cursor cursor = database.query(TABLE_PSALMNUMBERS, ALL_COLUMNS, COLUMN_BOOKID + " = " + bookId, null, null, null, null);
        if (cursor.moveToFirst()) {
            psalmNumberEntities = new HashSet<>(cursor.getCount());
            do {
                psalmNumberEntities.add(cursorToPsalmNumberEntity(cursor));
            } while (cursor.moveToNext());
            Log.v(LOG_TAG, METHOD_NAME + ": Count of psalm numbers selected for bookId=" + bookId + ": " + psalmNumberEntities.size());
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm numbers selected for bookId=" + bookId);
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
