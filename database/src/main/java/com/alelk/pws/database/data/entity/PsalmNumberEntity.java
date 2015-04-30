package com.alelk.pws.database.data.entity;

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
