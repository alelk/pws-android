package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsChorusTable.*;

import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.entity.ChorusEntity;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

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
    public ChorusEntity insert(PsalmChorus chorus) throws PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        if (psalmId == null) {
            // todo throw exception: incorrect psalm id
            return null;
        }
        ChorusEntity chorusEntity;
        final ContentValues contentValues = new ContentValues();
        fillContentValues(contentValues, chorus, psalmId);
        final long id = database.insert(TABLE_CHORUSES, null, contentValues);
        chorusEntity = selectById(id);
        Log.v(LOG_TAG, METHOD_NAME + ": New chorus added (psalmId=" + psalmId + "): " + chorusEntity);
        return chorusEntity;
    }

    @Override
    public ChorusEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        ChorusEntity chorusEntity = null;
        Cursor cursor = database.query(TABLE_CHORUSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            chorusEntity = cursorToVerseEntity(cursor);
        }
        Log.v(LOG_TAG, METHOD_NAME + ": Psalm chorus selected: " + chorusEntity);
        return chorusEntity;
    }

    private ChorusEntity cursorToVerseEntity(Cursor cursor) {
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
}
