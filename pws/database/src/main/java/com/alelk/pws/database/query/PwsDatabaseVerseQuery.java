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
import java.util.HashSet;
import java.util.Set;
// TODO: 14.03.2016 remove this class 
/**
 * Created by Alex Elkin on 30.04.2015.
 */
@Deprecated
public class PwsDatabaseVerseQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<PsalmVerse, VerseEntity> {

    private static final String LOG_TAG = PwsDatabaseVerseQuery.class.getSimpleName();

    private Cursor mCursor;
    private SQLiteDatabase mDatabase;
    private Long mPsalmId;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_NUMBER,
            COLUMN_PSALMID,
            COLUMN_TEXT };

    public PwsDatabaseVerseQuery(SQLiteDatabase mDatabase, Long mPsalmId) {
        this.mDatabase = mDatabase;
        this.mPsalmId = mPsalmId;
    }

    @Override
    public VerseEntity insert(PsalmVerse verse) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validatePsalmIdNotNull(METHOD_NAME, mPsalmId);
        validatePsalmPartNumbersNotEmpty(METHOD_NAME, verse);
        VerseEntity verseEntity = null;
        Long id = null;
        for (int number : verse.getNumbers()) {
            verseEntity = selectByNumberAndPsalmId(number, mPsalmId);
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
            fillContentValues(contentValues, verse, mPsalmId);
            id = mDatabase.insert(TABLE_VERSES, null, contentValues);
            verseEntity = selectById(id);
            Log.v(LOG_TAG, METHOD_NAME + ": New psalm verse added: " + verseEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": The psalm verse already exists in mDatabase: " + verseEntity);
        }
        return verseEntity;
    }

    @Override
    public VerseEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        VerseEntity verseEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_VERSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                verseEntity = cursorToVerseEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm verse selected (id = '" + id + "'): " + verseEntity);
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm verse found with id=" + id);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return verseEntity;
    }

    public VerseEntity selectByNumberAndPsalmId(long number, long psalmId) {
        final String METHOD_NAME = "selectByNumberAndPsalmId";
        VerseEntity verseEntity = null;
        try {
            final String[] SELECTION_ARGS = new String[2];
            Arrays.asList(String.valueOf(number), String.valueOf(psalmId)).toArray(SELECTION_ARGS);
            mCursor = mDatabase.query(TABLE_VERSES, ALL_COLUMNS,
                    COLUMN_NUMBER + "=? AND " + COLUMN_PSALMID + "=?", SELECTION_ARGS, null, null, "1");
            if (mCursor.moveToFirst()) {
                verseEntity = cursorToVerseEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm verse selected: " + verseEntity);
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm verse found with number='" + number
                        + "' and mPsalmId='" + psalmId + '\'');
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return verseEntity;
    }

    public Set<VerseEntity> selectByPsalmId(long psalmId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByPsalmId";
        Set<VerseEntity> verseEntities = null;
        try {
            validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
            mCursor = mDatabase.query(TABLE_VERSES, ALL_COLUMNS,
                    COLUMN_PSALMID + " = " + psalmId, null, null, null, null);
            if (mCursor.moveToFirst()) {
                verseEntities = new HashSet<>(mCursor.getCount());
                do {
                    verseEntities.add(cursorToVerseEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of psalm verses selected for mPsalmId=" + psalmId + ": " + verseEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm verses selected for mPsalmId=" + psalmId);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return verseEntities;
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
