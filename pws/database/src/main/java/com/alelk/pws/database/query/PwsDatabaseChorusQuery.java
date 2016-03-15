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
import java.util.HashSet;
import java.util.Set;
// TODO: 14.03.2016 remove this class 
/**
 * Created by Alex Elkin on 30.04.2015.
 */
@Deprecated
public class PwsDatabaseChorusQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<PsalmChorus, ChorusEntity> {

    private static final String LOG_TAG = PwsDatabaseChorusQuery.class.getSimpleName();

    private SQLiteDatabase mDatabase;
    private Long mPsalmId;
    private Cursor mCursor;

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_NUMBER,
            COLUMN_PSALMID,
            COLUMN_TEXT };

    public PwsDatabaseChorusQuery(SQLiteDatabase mDatabase, Long psalmId) {
        this.mDatabase = mDatabase;
        this.mPsalmId = psalmId;
    }

    @Override
    public ChorusEntity insert(PsalmChorus chorus) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validatePsalmIdNotNull(METHOD_NAME, mPsalmId);
        validatePsalmPartNumbersNotEmpty(METHOD_NAME, chorus);
        ChorusEntity chorusEntity = null;
        Long id = null;
        for (int number : chorus.getNumbers()) {
            chorusEntity = selectByNumberAndPsalmId(number, mPsalmId);
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
            fillContentValues(contentValues, chorus, mPsalmId);
            id = mDatabase.insert(TABLE_CHORUSES, null, contentValues);
            chorusEntity = selectById(id);
            Log.v(LOG_TAG, METHOD_NAME + ": New psalm chorus added: " + chorusEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": The psalm chorus already exists in mDatabase: " + chorusEntity);
        }
        return chorusEntity;
    }

    @Override
    public ChorusEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        ChorusEntity chorusEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_CHORUSES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                chorusEntity = cursorToChorusEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm chorus selected: " + chorusEntity);
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm chorus found with id=" + id);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return chorusEntity;
    }

    public ChorusEntity selectByNumberAndPsalmId(long number, long psalmId) {
        final String METHOD_NAME = "selectByNumberAndPsalmId";
        ChorusEntity chorusEntity = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(String.valueOf(number), String.valueOf(psalmId)).toArray(SELECTION_ARGS);
        try {
            mCursor = mDatabase.query(TABLE_CHORUSES, ALL_COLUMNS,
                    COLUMN_NUMBER + "=? AND " + COLUMN_PSALMID + "=?", SELECTION_ARGS, null, null, "1");
            if (mCursor.moveToFirst()) {
                chorusEntity = cursorToChorusEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm chorus selected: " + chorusEntity);
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm chorus found with number='" + number
                        + "' and mPsalmId='" + psalmId + '\'');
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return chorusEntity;
    }

    public Set<ChorusEntity> selectByPsalmId(long psalmId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByPsalmId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<ChorusEntity> chorusEntities = null;
        try {
            mCursor = mDatabase.query(TABLE_CHORUSES, ALL_COLUMNS,
                    COLUMN_PSALMID + " = " + psalmId, null, null, null, null);
            if (mCursor.moveToFirst()) {
                chorusEntities = new HashSet<>(mCursor.getCount());
                do {
                    chorusEntities.add(cursorToChorusEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of psalm choruses selected for mPsalmId=" + psalmId + ": " + chorusEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm choruses selected for mPsalmId=" + psalmId);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return chorusEntities;
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
