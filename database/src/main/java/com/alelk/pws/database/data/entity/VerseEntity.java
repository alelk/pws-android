package com.alelk.pws.database.data.entity;

import android.text.TextUtils;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class VerseEntity implements PwsDatabaseEntity {
    private long id;
    private long psalmId;
    private String numbers;
    private String text;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public long getPsalmId() {
        return psalmId;
    }

    public void setPsalmId(long psalmId) {
        this.psalmId = psalmId;
    }

    public String getNumbers() {
        return numbers;
    }

    public void setNumbers(String numbers) {
        this.numbers = numbers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "VerseEntity{" +
                "id=" + id +
                ", psalmId=" + psalmId +
                ", numbers='" + numbers + '\'' +
                ", text='" + (text == null ? "" : text.substring(0, 20).replaceAll("\n", "|")) +
                "... (" + text.length() + " symbols)'" +
                '}';
    }
}
