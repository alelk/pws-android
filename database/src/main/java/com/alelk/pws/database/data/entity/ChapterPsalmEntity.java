package com.alelk.pws.database.data.entity;

/**
 * Created by alelkin on 30.04.2015.
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
    public String toString() {
        return "ChapterPsalmEntity{" +
                "id=" + id +
                ", chapterId=" + chapterId +
                ", psalmId=" + psalmId +
                '}';
    }
}
