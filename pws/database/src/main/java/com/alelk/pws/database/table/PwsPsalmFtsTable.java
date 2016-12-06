package com.alelk.pws.database.table;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_TEXT;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_NAME;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_AUTHOR;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_TRANSLATOR;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_COMPOSER;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_ANNOTATION;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_ID;


/**
 * Created by Alex Elkin on 15.03.2016.
 * Implements functionality to providing Full Text Search functionality for psalm.
 */
public class PwsPsalmFtsTable extends PwsTableHelper implements PwsTable {

    public interface UpdateProgressListener {
        void onUpdateProgress(int max, int current);
    }

    private static final String LOG_TAG = PwsPsalmFtsTable.class.getSimpleName();
    public static final String TABLE_PSALMS_FTS = "psalms_fts";
    private static final String TRIGGER_BEFORE_UPDATE = TABLE_PSALMS_FTS + "_bu";
    private static final String TRIGGER_BEFORE_DELETE = TABLE_PSALMS_FTS + "_bd";
    private static final String TRIGGER_AFTER_UPDATE = TABLE_PSALMS_FTS + "_au";
    private static final String TRIGGER_AFTER_INSERT = TABLE_PSALMS_FTS + "_ai";

    private static final String TOKENIZER_API21 = "icu ru_RU";

    private static final boolean IS_API_21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private static final String TABLE_CREATE_SCRIPT = "create virtual table " + TABLE_PSALMS_FTS +
            " using fts4 " +
            "(content=" + TABLE_PSALMS + ", " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT +
            (IS_API_21 ? ", tokenize=" + TOKENIZER_API21 : "") + ");";
    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMS_FTS;

    private static final String TABLE_FILL_VALUES_SCRIPT = "insert into " + TABLE_PSALMS_FTS +
            "(docid, " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT + ") " +
            "select " +
            COLUMN_ID + ", " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT +
            " from " + TABLE_PSALMS + ";";

    private static final String[] TABLE_PSALMS_PROJECTION = {
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_AUTHOR,
            COLUMN_TRANSLATOR,
            COLUMN_COMPOSER,
            COLUMN_ANNOTATION,
            COLUMN_TEXT
    };

    private static final String TRIGGER_BU_SCRIPT = "create trigger " + TRIGGER_BEFORE_UPDATE +
            " before update on " + TABLE_PSALMS +
            " begin delete from " + TABLE_PSALMS_FTS + " where docid=old.rowid; end;";

    private static final String TRIGGER_BD_SCRIPT = "create trigger " + TRIGGER_BEFORE_DELETE +
            " before delete on " + TABLE_PSALMS +
            " begin delete from " + TABLE_PSALMS_FTS + " where docid=old.rowid; end;";

    private static final String TRIGGER_AU_SCRIPT = "create trigger " + TRIGGER_AFTER_UPDATE +
            " after update on " + TABLE_PSALMS +
            " begin insert into " + TABLE_PSALMS_FTS + "(docid, " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT + ") " +
            "values(new.rowid, " +
            "new." + COLUMN_NAME + ", " +
            "new." + COLUMN_AUTHOR + ", " +
            "new." + COLUMN_TRANSLATOR + ", " +
            "new." + COLUMN_COMPOSER + ", " +
            "new." + COLUMN_ANNOTATION + ", " +
            "new." + COLUMN_TEXT + "); end;";

    private static final String TRIGGER_AI_SCRIPT = "create trigger " + TRIGGER_AFTER_INSERT +
            " after insert on " + TABLE_PSALMS +
            " begin insert into " + TABLE_PSALMS_FTS + "(docid, " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT + ") " +
            "values(new.rowid, " +
            "new." + COLUMN_NAME + ", " +
            "new." + COLUMN_AUTHOR + ", " +
            "new." + COLUMN_TRANSLATOR + ", " +
            "new." + COLUMN_COMPOSER + ", " +
            "new." + COLUMN_ANNOTATION + ", " +
            "new." + COLUMN_TEXT + "); end;";

    private static final String TRIGGER_BU_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_BEFORE_UPDATE;
    private static final String TRIGGER_BD_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_BEFORE_DELETE;
    private static final String TRIGGER_AU_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_AFTER_UPDATE;
    private static final String TRIGGER_AI_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_AFTER_INSERT;

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void populateTable(@NonNull SQLiteDatabase db, UpdateProgressListener progressListener) {
        final String METHOD_NAME = "populateTable";
        if (IS_API_21) {
            db.execSQL(TABLE_FILL_VALUES_SCRIPT);
            return;
        }
        Cursor cursor = db.query(TABLE_PSALMS, new String[]{"max(" + COLUMN_ID + ") as maxid"}, null, null, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Log.w(LOG_TAG, METHOD_NAME + ": looks like no rows in '" + TABLE_PSALMS + "' table. Could not populate '" + TABLE_PSALMS_FTS + "' table");
            return;
        }
        long maxId = cursor.getLong(cursor.getColumnIndex("maxid"));
        for (long id = 0; id < maxId; id += 100) {
            progressListener.onUpdateProgress((int) maxId, (int) id);
            Log.v(LOG_TAG, METHOD_NAME + ": Insert to " + TABLE_PSALMS_FTS + " table rows with id between " + id + " and " + (id + 100) + " from " + TABLE_PSALMS + " table.");
            cursor = query(db, id, id + 99);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(LOG_TAG, METHOD_NAME + ": unsuccessful attempt to query data from '" + TABLE_PSALMS + "' table with id between " + id + " and " + (id + 100) + ". No results.");
                continue;
            }
            do {
                db.insert(TABLE_PSALMS_FTS, null, fromPsalmsTableCursor(cursor));
            } while (cursor.moveToNext());
        }
    }

    private static Cursor query(@NonNull SQLiteDatabase db, long fromId, long toId) {
        return db.query(TABLE_PSALMS, TABLE_PSALMS_PROJECTION, "_id >= " + fromId + " and _id <= " + toId, null, null, null, null);
    }

    private static ContentValues fromPsalmsTableCursor(Cursor cursor) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("docid", cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        contentValues.put(COLUMN_NAME, cursor.getString(cursor.getColumnIndex(COLUMN_NAME)).toLowerCase());
        final String translator = cursor.getString(cursor.getColumnIndex(COLUMN_TRANSLATOR));
        if (!TextUtils.isEmpty(translator)) {
            contentValues.put(COLUMN_TRANSLATOR, translator.toLowerCase());
        }
        final String composer = cursor.getString(cursor.getColumnIndex(COLUMN_COMPOSER));
        if (!TextUtils.isEmpty(composer)) {
            contentValues.put(COLUMN_COMPOSER, composer.toLowerCase());
        }
        final String author = cursor.getString(cursor.getColumnIndex(COLUMN_AUTHOR));
        if (!TextUtils.isEmpty(author)) {
            contentValues.put(COLUMN_AUTHOR, author.toLowerCase());
        }
        final String annotation = cursor.getString(cursor.getColumnIndex(COLUMN_ANNOTATION));
        if (!TextUtils.isEmpty(annotation)) {
            contentValues.put(COLUMN_AUTHOR, annotation.toLowerCase());
        }
        contentValues.put(COLUMN_TEXT, cursor.getString(cursor.getColumnIndex(COLUMN_TEXT)).toLowerCase());
        return contentValues;
    }

    public static void dropTable(@NonNull SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }

    public static void setUpAllTriggers(@NonNull SQLiteDatabase db) {
        setBeforeUpdateTrigger(db);
        setBeforeDeleteTrigger(db);
        setAfterUpdateTrigger(db);
        setAfterInsertTrigger(db);
    }

    public static void dropAllTriggers(@NonNull SQLiteDatabase db) {
        dropBeforeUpdateTrigger(db);
        dropBeforeDeleteTrigger(db);
        dropAfterUpdateTrigger(db);
        dropAfterInsertTrigger(db);
    }

    public static boolean isAllTriggersExists(@NonNull SQLiteDatabase db) {
        return isAiTriggerExists(db) && isAuTriggerExists(db) && isBuTriggerExists(db) && isBdTriggerExists(db);
    }

    private static void setBeforeUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_SCRIPT);
    }

    private static void setBeforeDeleteTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_SCRIPT);
    }

    private static void setAfterUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_SCRIPT);
    }

    private static void setAfterInsertTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_SCRIPT);
    }

    private static void dropBeforeUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_DROP_SCRIPT);
    }

    private static void dropBeforeDeleteTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_DROP_SCRIPT);
    }

    private static void dropAfterUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_DROP_SCRIPT);
    }

    private static void dropAfterInsertTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_DROP_SCRIPT);
    }

    private static boolean isBuTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_BEFORE_UPDATE);
    }

    private static boolean isBdTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_BEFORE_DELETE);
    }

    private static boolean isAuTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_AFTER_UPDATE);
    }

    private static boolean isAiTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_AFTER_INSERT);
    }

    public static boolean isTableExists(@NonNull SQLiteDatabase db) {
        return isTableExists(db, TABLE_PSALMS_FTS);
    }
}
