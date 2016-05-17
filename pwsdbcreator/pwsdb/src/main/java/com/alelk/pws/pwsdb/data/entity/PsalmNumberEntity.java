package com.alelk.pws.pwsdb.data.entity;

import android.database.Cursor;

import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.*;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_BOOKID;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_ID;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_NUMBER;
import static com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable.COLUMN_PSALMID;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class PsalmNumberEntity implements PwsDatabaseEntity {

    private long id;
    private long bookId;
    private long psalmId;
    private long number;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public PwsDatabaseEntity applyDataFomCursor(Cursor cursor) {
        final int idCol = cursor.getColumnIndex(COLUMN_ID);
        if (idCol >= 0) {
            setId(cursor.getLong(idCol));
        }
        final int bookidCol = cursor.getColumnIndex(COLUMN_BOOKID);
        if (bookidCol >= 0) {
            setBookId(cursor.getLong(bookidCol));
        }
        final int psalmidCol = cursor.getColumnIndex(COLUMN_PSALMID);
        if (psalmidCol >= 0) {
            setPsalmId(cursor.getLong(psalmidCol));
        }
        final int numberCol = cursor.getColumnIndex(COLUMN_NUMBER);
        if (numberCol >= 0) {
            setNumber(cursor.getLong(numberCol));
        }
        return null;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public long getPsalmId() {
        return psalmId;
    }

    public void setPsalmId(long psalmId) {
        this.psalmId = psalmId;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "PsalmNumberEntity{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", psalmId=" + psalmId +
                ", number=" + number +
                '}';
    }
}
