package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsVerseTable.*;

import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.VerseEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

import java.util.Arrays;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class PwsDatabaseVerseQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<PsalmVerse, VerseEntity> {

    private static final String LOG_TAG = PwsDatabaseVerseQuery.class.getSimpleName();

    private SQLiteDatabase database;
    private Long psalmId;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_NUMBER,
            COLUMN_PSALMID,
            COLUMN_TEXT };

    public PwsDatabaseVerseQuery(SQLiteDatabase database, Long psalmId) {
        this.database = database;
        this.psalmId = psalmId;
    }

    @Override
    public VerseEntity insert(PsalmVerse verse) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmIdNotNull(METHOD_NAME, psalmId);
        VerseEntity verseEntity = null;
        Long id = null;
        for (int number : verse.getNumbers()) {
            verseEntity = selectByNumberAndPsalmId(number, psalmId);
            if (verseEntity == null) break;
            if (id == null){ id = verseEntity.getId(); } else {
                if (!id.equals(verseEntity.getId())) {
                    // todo throw exception
                    return null;
                }
            }
        }
        if (verseEntity == null) {
            final ContentValues contentValues = new ContentValues();
            fillContentValues(contentValues, verse, psalmId);
            id = database.insert(TABLE_VERSES, null, contentValues);
            verseEntity = selectById(id);
            Log.v(LOG_TAG, METHOD_NAME + ": New psalm verse added: " + verseEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": The psalm verse already exists in database: " + verseEntity);
        }
        return verseEntity;
    }

    @Override
    public VerseEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        VerseEntity verseEntity = null;
        Cursor cursor = database.query(TABLE_VERSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            verseEntity = cursorToVerseEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm verse selected (id = '" + id + "'): " + verseEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm verse found with id=" + id);
        }
        return verseEntity;
    }

    public VerseEntity selectByNumberAndPsalmId(long number, long psalmId) {
        final String METHOD_NAME = "selectByNumberAndPsalmId";
        VerseEntity verseEntity = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(String.valueOf(number), String.valueOf(psalmId)).toArray(SELECTION_ARGS);
        Cursor cursor = database.query(TABLE_VERSES, ALL_COLUMNS,
                COLUMN_NUMBER + "=? AND " + COLUMN_PSALMID + "=?", SELECTION_ARGS, null, null, "1");
        if (cursor.moveToFirst()) {
            verseEntity = cursorToVerseEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm verse selected: " + verseEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm verse found with number='" + number
                    + "' and psalmId='" + psalmId + '\'');
        }
        return verseEntity;
    }

    private VerseEntity cursorToVerseEntity(Cursor cursor) {
        VerseEntity verseEntity = new VerseEntity();
        verseEntity.setId(cursor.getLong(0));
        verseEntity.setNumbers(cursor.getString(1));
        verseEntity.setPsalmId(cursor.getLong(2));
        verseEntity.setText(cursor.getString(3));
        return verseEntity;
    }

    private void fillContentValues(ContentValues values, PsalmVerse verse, long psalmId) {
        if (verse.getNumbers() != null && !verse.getNumbers().isEmpty()) {
            values.put(COLUMN_NUMBER, TextUtils.join(MULTIVALUE_DELIMITER, verse.getNumbers()));
        }
        if (!TextUtils.isEmpty(verse.getText())) {
            values.put(COLUMN_TEXT, verse.getText());
        }
        values.put(COLUMN_PSALMID, psalmId);
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
