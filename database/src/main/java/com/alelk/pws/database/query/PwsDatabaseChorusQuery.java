package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsChorusTable.*;

import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.entity.ChorusEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

import java.util.Arrays;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class PwsDatabaseChorusQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<PsalmChorus, ChorusEntity> {

    private static final String LOG_TAG = PwsDatabaseChorusQuery.class.getSimpleName();

    private SQLiteDatabase database;
    private Long psalmId;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_NUMBER,
            COLUMN_PSALMID,
            COLUMN_TEXT };

    public PwsDatabaseChorusQuery(SQLiteDatabase database, Long psalmId) {
        this.database = database;
        this.psalmId = psalmId;
    }

    @Override
    public ChorusEntity insert(PsalmChorus chorus) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmIdNotNull(METHOD_NAME, psalmId);
        validatePsalmPartNumbersNotEmpty(METHOD_NAME, chorus);
        ChorusEntity chorusEntity = null;
        Long id = null;
        for (int number : chorus.getNumbers()) {
            chorusEntity = selectByNumberAndPsalmId(number, psalmId);
            if (chorusEntity == null) break;
            if (id == null){ id = chorusEntity.getId(); } else {
                if (!id.equals(chorusEntity.getId())) {
                    // todo throw exception
                    return null;
                }
            }
        }
        if (chorusEntity == null) {
            final ContentValues contentValues = new ContentValues();
            fillContentValues(contentValues, chorus, psalmId);
            id = database.insert(TABLE_CHORUSES, null, contentValues);
            chorusEntity = selectById(id);
            Log.v(LOG_TAG, METHOD_NAME + ": New psalm chorus added: " + chorusEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": The psalm chorus already exists in database: " + chorusEntity);
        }
        return chorusEntity;
    }

    @Override
    public ChorusEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        ChorusEntity chorusEntity = null;
        Cursor cursor = database.query(TABLE_CHORUSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            chorusEntity = cursorToChorusEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm chorus selected: " + chorusEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm chorus found with id=" + id);
        }
        return chorusEntity;
    }

    public ChorusEntity selectByNumberAndPsalmId(long number, long psalmId) {
        final String METHOD_NAME = "selectByNumberAndPsalmId";
        ChorusEntity chorusEntity = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(String.valueOf(number), String.valueOf(psalmId)).toArray(SELECTION_ARGS);
        Cursor cursor = database.query(TABLE_CHORUSES, ALL_COLUMNS,
                COLUMN_NUMBER + "=? AND " + COLUMN_PSALMID + "=?", SELECTION_ARGS, null, null, "1");
        if (cursor.moveToFirst()) {
            chorusEntity = cursorToChorusEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm chorus selected: " + chorusEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm chorus found with number='" + number
                    + "' and psalmId='" + psalmId + '\'');
        }
        return chorusEntity;
    }

    private ChorusEntity cursorToChorusEntity(Cursor cursor) {
        ChorusEntity chorusEntity = new ChorusEntity();
        chorusEntity.setId(cursor.getLong(0));
        chorusEntity.setNumbers(cursor.getString(1));
        chorusEntity.setPsalmId(cursor.getLong(2));
        chorusEntity.setText(cursor.getString(3));
        return chorusEntity;
    }

    private void fillContentValues(ContentValues values, PsalmChorus chorus, long psalmId) {
        if (chorus.getNumbers() != null && !chorus.getNumbers().isEmpty()) {
            values.put(COLUMN_NUMBER, TextUtils.join(MULTIVALUE_DELIMITER, chorus.getNumbers()));
        }
        if (!TextUtils.isEmpty(chorus.getText())) {
            values.put(COLUMN_TEXT, chorus.getText());
        }
        values.put(COLUMN_PSALMID, psalmId);
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
