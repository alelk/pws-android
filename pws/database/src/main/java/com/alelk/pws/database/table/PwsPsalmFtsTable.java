package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_TEXT;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmTable.COLUMN_ID;


/**
 * Created by Alex Elkin on 15.03.2016.
 */
public class PwsPsalmFtsTable implements PwsTable {

    public static final String TABLE_PSALMS_FTS = "psalms_fts";
    public static final String TRIGGER_BEFORE_UPDATE = TABLE_PSALMS_FTS + "_bu";
    public static final String TRIGGER_BEFORE_DELETE = TABLE_PSALMS_FTS + "_bd";
    public static final String TRIGGER_AFTER_UPDATE = TABLE_PSALMS_FTS + "_au";
    public static final String TRIGGER_AFTER_INSERT = TABLE_PSALMS_FTS + "_ai";

    private static final String TABLE_CREATE_SCRIPT = "create virtual table " + TABLE_PSALMS_FTS +
            " using fts4 " +
            "(content=" + TABLE_PSALMS + ", " +
            COLUMN_TEXT + ", tokenize=icu ru_RU);";
    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMS_FTS;

    private static final String TABLE_FILL_VALUES_SCRIPT = "insert into " + TABLE_PSALMS_FTS +
            "(docid, " + COLUMN_TEXT + ") select " + COLUMN_ID + ", " + COLUMN_TEXT +
            " from " + TABLE_PSALMS + ";";

    private static final String TRIGGER_BU_SCRIPT = "create trigger " + TRIGGER_BEFORE_UPDATE +
            " before update on " + TABLE_PSALMS +
            " begin delete from " + TABLE_PSALMS_FTS + " where docid=old.rowid; end;";

    private static final String TRIGGER_BD_SCRIPT = "create trigger " + TRIGGER_BEFORE_DELETE +
            " before delete on " + TABLE_PSALMS +
            " begin delete from " + TABLE_PSALMS_FTS + " where docid=old.rowid; end;";

    private static final String TRIGGER_AU_SCRIPT = "create trigger " + TRIGGER_AFTER_UPDATE +
            " after update on " + TABLE_PSALMS +
            " begin insert into " + TABLE_PSALMS_FTS + "(docid, " + COLUMN_TEXT + ") " +
            "values(new.rowid, new." + COLUMN_TEXT + "); end;";

    private static final String TRIGGER_AI_SCRIPT = "create trigger " + TRIGGER_AFTER_INSERT +
            " after insert on " + TABLE_PSALMS +
            " begin insert into " + TABLE_PSALMS_FTS + "(docid, " + COLUMN_TEXT + ") " +
            "values(new.rowid, new." + COLUMN_TEXT + "); end;";

    private static final String TRIGGER_BU_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_BEFORE_UPDATE;
    private static final String TRIGGER_BD_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_BEFORE_DELETE;
    private static final String TRIGGER_AU_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_AFTER_UPDATE;
    private static final String TRIGGER_AI_DROP_SCRIPT = "drop trigger if exists " + TRIGGER_AFTER_INSERT;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
        db.execSQL(TABLE_FILL_VALUES_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }

    public static void setUpAllTriggers(SQLiteDatabase db) {
        setBeforeUpdateTrigger(db);
        setBeforeDeleteTrigger(db);
        setAfterUpdateTrigger(db);
        setAfterInsertTrigger(db);
    }

    public static void dropAllTriggers(SQLiteDatabase db) {
        dropBeforeUpdateTrigger(db);
        dropBeforeDeleteTrigger(db);
        dropAfterUpdateTrigger(db);
        dropAfterInsertTrigger(db);
    }

    public static void setBeforeUpdateTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_SCRIPT);
    }
    public static void setBeforeDeleteTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_SCRIPT);
    }
    public static void setAfterUpdateTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_SCRIPT);
    }
    public static void setAfterInsertTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_SCRIPT);
    }
    public static void dropBeforeUpdateTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_BU_DROP_SCRIPT);
    }
    public static void dropBeforeDeleteTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_BD_DROP_SCRIPT);
    }
    public static void dropAfterUpdateTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_AU_DROP_SCRIPT);
    }
    public static void dropAfterInsertTrigger(SQLiteDatabase db) {
        db.execSQL(TRIGGER_AI_DROP_SCRIPT);
    }
}
