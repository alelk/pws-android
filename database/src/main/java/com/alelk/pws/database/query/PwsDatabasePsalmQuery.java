package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.PsalmNumberEntity;
import com.alelk.pws.database.exception.PwsDatabaseException;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseMessage;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.alelk.pws.database.table.PwsPsalmTable.*;

/**
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabasePsalmQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<Psalm, PsalmEntity> {

    private static final String LOG_TAG = PwsDatabasePsalmQuery.class.getSimpleName();

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_VERSION,
            COLUMN_NAME,
            COLUMN_AUTHOR,
            COLUMN_TRANSLATOR,
            COLUMN_COMPOSER,
            COLUMN_TONALITIES,
            COLUMN_YEAR };

    private SQLiteDatabase database;

    public PwsDatabasePsalmQuery(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public PsalmEntity insert(Psalm psalm) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmNotNull(METHOD_NAME, psalm);

        PsalmEntity psalmEntity;
        database.beginTransaction();
        try {
            psalmEntity = selectByNumbers(psalm);
            if (psalmEntity != null) {
                Log.v(LOG_TAG, METHOD_NAME + ": psalm already exists: " + psalmEntity);
            } else {
                final ContentValues contentValues = new ContentValues();
                fillContentValues(contentValues, psalm);
                long id = database.insert(TABLE_PSALMS, null, contentValues);
                insertPsalmNumbers(psalm, id);

                psalmEntity = selectById(id);

                database.setTransactionSuccessful();
                Log.v(LOG_TAG, METHOD_NAME + ": New psalm added: " + psalmEntity);
            }
        } catch (PwsDatabaseException e) {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm added. The exception was thrown: " + e.getPwsDatabaseMessage());
            throw e;
        } finally {
            database.endTransaction();
        }
        return psalmEntity;
    }

    @Override
    public PsalmEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        PsalmEntity psalmEntity = null;
        Cursor cursor = database.query(TABLE_PSALMS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            psalmEntity = cursorToPsalmEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm selected: " + psalmEntity);
        }
        return  psalmEntity;
    }

    public PsalmEntity selectByNumbers(Psalm psalm) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumbers";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmNotNull(METHOD_NAME, psalm);
        PsalmEntity psalmEntity = null;
        validatePsalmNumbersNotEmpty(METHOD_NAME, psalm.getNumbers());
        Long psalmId = null;
        for (BookEdition bookEdition : psalm.getBookEditions()) {
            long psalmNumber = psalm.getNumber(bookEdition);
            long bookId = new PwsDatabaseBookQuery(database).selectByEdition(bookEdition).getId();
            PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(database, null, null).selectByNumberAndBookId(psalmNumber, bookId);
            if (psalmNumberEntity == null) {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm number entity found with psalmNumber="
                        + psalmNumber + " and bookEdition=" + bookEdition + " (bookId=");
                return null;
            }
            if (psalmId == null) {
                psalmId = psalmNumberEntity.getPsalmId();
            } else if (!psalmId.equals(psalmNumberEntity.getPsalmId())) {
                // todo throw exception
                return null;
            }
        }
        // todo compare all numbers
        if (psalmId != null) {
            psalmEntity = selectById(psalmId);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm selected" + psalmEntity);
        }
        return psalmEntity;
    }

    private void insertPsalmNumbers(Psalm psalm, Long psalmId) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        for (BookEdition bookEdition : psalm.getNumbers().keySet()) {
            new PwsDatabasePsalmNumberQuery(database, psalmId, bookEdition).insert(psalm);
        }
    }

    private PsalmEntity cursorToPsalmEntity(Cursor cursor) {
        PsalmEntity psalmEntity = new PsalmEntity();
        psalmEntity.setId(cursor.getLong(0));
        psalmEntity.setVersion(cursor.getString(1));
        psalmEntity.setName(cursor.getString(2));
        psalmEntity.setAuthor(cursor.getString(3));
        psalmEntity.setTranslator(cursor.getString(4));
        psalmEntity.setComposer(cursor.getString(5));
        psalmEntity.setTonalities(cursor.getString(6));
        psalmEntity.setYear(cursor.getString(7));
        return psalmEntity;
    }

    private void fillContentValues(ContentValues values, Psalm psalm) {
        if (!TextUtils.isEmpty(psalm.getVersion())) {
            values.put(COLUMN_VERSION, psalm.getVersion());
        }
        if (!TextUtils.isEmpty(psalm.getName())) {
            values.put(COLUMN_NAME, psalm.getName());
        }
        if (!TextUtils.isEmpty(psalm.getAuthor())) {
            values.put(COLUMN_AUTHOR, psalm.getAuthor());
        }
        if (!TextUtils.isEmpty(psalm.getTranslator())) {
            values.put(COLUMN_TRANSLATOR, psalm.getTranslator());
        }
        if (!TextUtils.isEmpty(psalm.getComposer())) {
            values.put(COLUMN_COMPOSER, psalm.getComposer());
        }
        if (psalm.getTonalities() != null && !psalm.getTonalities().isEmpty()) {
            values.put(COLUMN_TONALITIES, TextUtils.join(MULTIVALUE_DELIMITER, psalm.getTonalities()));
        }
        if (psalm.getYear() != null) {
            values.put(COLUMN_YEAR, psalm.getYear().toString());
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
