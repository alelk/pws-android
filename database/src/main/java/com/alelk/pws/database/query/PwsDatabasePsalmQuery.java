package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExists;

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

    SQLiteDatabase database;

    public PwsDatabasePsalmQuery(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public PsalmEntity insert(Psalm psalm) throws PwsDatabaseSourceIdExists {
        final String METHOD_NAME = "insert";
        PsalmEntity psalmEntity;
        final ContentValues contentValues = new ContentValues();
        fillContentValues(contentValues, psalm);
        long id = database.insert(TABLE_PSALMS, null, contentValues);
        psalmEntity = selectById(id);
        Log.d(LOG_TAG, METHOD_NAME + ": New psalm added: " + psalmEntity);
        return psalmEntity;
    }

    @Override
    public PsalmEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        PsalmEntity psalmEntity = null;
        Cursor cursor = database.query(TABLE_PSALMS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            psalmEntity = cursorToPsalmEntity(cursor);
        }
        Log.d(LOG_TAG, METHOD_NAME + ": Psalm selected (id = '" + id + "'): " + psalmEntity);
        return  psalmEntity;
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
}
