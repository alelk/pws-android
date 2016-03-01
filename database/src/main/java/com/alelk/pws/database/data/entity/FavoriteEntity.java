package com.alelk.pws.database.data.entity;

import android.database.Cursor;

/**
 * Created by Alex Elkin on 19.02.2016.
 */
public class FavoriteEntity implements PwsDatabaseEntity {

    private long id;
    private long psalmNumberId;
    private long position;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public long getPsalmNumberId() {
        return psalmNumberId;
    }

    public void setPsalmNumberId(long psalmNumberId) {
        this.psalmNumberId = psalmNumberId;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public PwsDatabaseEntity applyDataFomCursor(Cursor cursor) {
        // // TODO: 20.02.2016 remove this method as builder may be used
        return null;
    }

    @Override
    public String toString() {
        return "FavoriteEntity{" +
                "id=" + id +
                ", psalmNumberId=" + psalmNumberId +
                ", position=" + position +
                '}';
    }
}
