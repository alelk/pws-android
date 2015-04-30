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
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

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
    }

    @Override
    public PsalmNumberEntity insert(Psalm psalm) throws PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        if (database == null || psalmId == null || bookEdition == null) {
            // todo throw exception
            return null;
        }
        PsalmNumberEntity psalmNumberEntity = null;
        BookEntity bookEntity = new PwsDatabaseBookQuery(database).selectByEdition(bookEdition);
        if (bookEntity == null) {
            // todo throw exception: no book found
            return null;
        }
        final long bookId = bookEntity.getId();
        if (psalm.getNumber(bookEdition) != null) {
            final long number = psalm.getNumber(bookEdition);
            final ContentValues contentValues = new ContentValues();
            fillContentValues(contentValues, psalmId, bookId, number);
            final long id = database.insert(TABLE_PSALMNUMBERS, null, contentValues);
            psalmNumberEntity = selectById(id);
        } else {
            // todo throw exception: incorrect book edition
            return null;
        }
        Log.v(LOG_TAG, METHOD_NAME + ": New psalm number added: " + psalmNumberEntity);
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
}
