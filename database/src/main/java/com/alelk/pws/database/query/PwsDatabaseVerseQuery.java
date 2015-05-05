package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsVerseTable.*;

import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.VerseEntity;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

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
    public VerseEntity insert(PsalmVerse verse) throws PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        if (psalmId == null) {
            // todo throw exception: incorrect psalm id
            return null;
        }
        VerseEntity verseEntity;
        final ContentValues contentValues = new ContentValues();
        fillContentValues(contentValues, verse, psalmId);
        final long id = database.insert(TABLE_VERSES, null, contentValues);
        verseEntity = selectById(id);
        Log.v(LOG_TAG, METHOD_NAME + ": New verse added (psalmId=" + psalmId + "): " + verseEntity);
        return verseEntity;
    }

    @Override
    public VerseEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        VerseEntity verseEntity = null;
        Cursor cursor = database.query(TABLE_VERSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            verseEntity = cursorToVerseEntity(cursor);
        }
        Log.v(LOG_TAG, METHOD_NAME + ": Psalm verse selected (id = '" + id + "'): " + verseEntity);
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
