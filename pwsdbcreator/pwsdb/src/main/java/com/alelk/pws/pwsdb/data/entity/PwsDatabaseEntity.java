package com.alelk.pws.pwsdb.data.entity;

import android.database.Cursor;

/**
 * Created by alelkin on 23.04.2015.
 */
public interface PwsDatabaseEntity {

    long getId();
    void setId(long id);
    PwsDatabaseEntity applyDataFomCursor(Cursor cursor);
}
