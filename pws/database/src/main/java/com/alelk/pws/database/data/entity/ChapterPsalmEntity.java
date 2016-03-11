package com.alelk.pws.database.data.entity;

import android.database.Cursor;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class ChapterPsalmEntity implements PwsDatabaseEntity {

    private long id;
    private long chapterId;
    private long psalmId;

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
        // todo
        return null;
    }

    @Override
    public String toString() {
        return "ChapterPsalmEntity{" +
                "id=" + id +
                ", chapterId=" + chapterId +
                ", psalmId=" + psalmId +
                '}';
    }
}
