package com.alelk.pws.database.table;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;

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

    public static final String TABLE_PSALMS_FTS = "psalms_fts";
    public static final String TRIGGER_BEFORE_UPDATE = TABLE_PSALMS_FTS + "_bu";
    public static final String TRIGGER_BEFORE_DELETE = TABLE_PSALMS_FTS + "_bd";
    public static final String TRIGGER_AFTER_UPDATE = TABLE_PSALMS_FTS + "_au";
    public static final String TRIGGER_AFTER_INSERT = TABLE_PSALMS_FTS + "_ai";

    private static final String TOKENIZER_API21 = "icu ru_RU";
    private static final String TOKENIZER_API17 = "porter";

    private static final String TABLE_CREATE_SCRIPT = "create virtual table " + TABLE_PSALMS_FTS +
            " using fts4 " +
            "(content=" + TABLE_PSALMS + ", " +
            COLUMN_NAME + ", " +
            COLUMN_AUTHOR + ", " +
            COLUMN_TRANSLATOR + ", " +
            COLUMN_COMPOSER + ", " +
            COLUMN_ANNOTATION + ", " +
            COLUMN_TEXT + ", tokenize=" +
            (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ? TOKENIZER_API21 : TOKENIZER_API17) + ");";
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

    public static void populateTable(@NonNull SQLiteDatabase db) {
        db.execSQL(TABLE_FILL_VALUES_SCRIPT);
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
        if (isAiTriggerExists(db) && isAuTriggerExists(db) && isBuTriggerExists(db) && isBdTriggerExists(db)) {
            return true;
        }
        return false;
    }

    public static void setBeforeUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_SCRIPT);
    }

    public static void setBeforeDeleteTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_SCRIPT);
    }

    public static void setAfterUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_SCRIPT);
    }

    public static void setAfterInsertTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_SCRIPT);
    }

    public static void dropBeforeUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_DROP_SCRIPT);
    }

    public static void dropBeforeDeleteTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_DROP_SCRIPT);
    }

    public static void dropAfterUpdateTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_DROP_SCRIPT);
    }

    public static void dropAfterInsertTrigger(@NonNull SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_DROP_SCRIPT);
    }

    public static boolean isBuTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_BEFORE_UPDATE);
    }

    public static boolean isBdTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_BEFORE_DELETE);
    }

    public static boolean isAuTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_AFTER_UPDATE);
    }

    public static boolean isAiTriggerExists(@NonNull SQLiteDatabase db) {
        return isTriggerExists(db, TRIGGER_AFTER_INSERT);
    }

    public static boolean isTableExists(@NonNull SQLiteDatabase db) {
        return isTableExists(db, TABLE_PSALMS_FTS);
    }
}
