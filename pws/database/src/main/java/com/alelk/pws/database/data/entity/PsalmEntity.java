package com.alelk.pws.database.data.entity;

import android.database.Cursor;

import static com.alelk.pws.database.table.PwsPsalmTable.*;

/**
 * Created by Alex Elkin on 28.04.2015.
 */
public class PsalmEntity implements PwsDatabaseEntity{
    private long id;
    private String name;
    private String locale;
    private String version;
    private String author;
    private String translator;
    private String composer;
    private String year;
    private String tonalities;
    private String annotation;
    private String text;

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
        int idCol = cursor.getColumnIndex(COLUMN_ID);
        if (idCol >= 0) {
            setId(cursor.getLong(idCol));
        }
        int versionCol = cursor.getColumnIndex(COLUMN_VERSION);
        if (versionCol >= 0) {
            setVersion(cursor.getString(versionCol));
        }
        int nameCol = cursor.getColumnIndex(COLUMN_NAME);
        if (nameCol >= 0) {
            setName(cursor.getString(nameCol));
        }
        int authorCol = cursor.getColumnIndex(COLUMN_AUTHOR);
        if (authorCol >= 0) {
            setAuthor(cursor.getString(authorCol));
        }
        int translatorCol = cursor.getColumnIndex(COLUMN_TRANSLATOR);
        if (translatorCol >= 0) {
            setTranslator(cursor.getString(translatorCol));
        }
        int composerCol = cursor.getColumnIndex(COLUMN_COMPOSER);
        if (composerCol >= 0) {
            setComposer(cursor.getString(composerCol));
        }
        int tonalitiesCol = cursor.getColumnIndex(COLUMN_TONALITIES);
        if (tonalitiesCol >= 0) {
            setTonalities(cursor.getString(tonalitiesCol));
        }
        int yearCol = cursor.getColumnIndex(COLUMN_YEAR);
        if (yearCol >= 0) {
            setYear(cursor.getString(yearCol));
        }
        int annotationCol = cursor.getColumnIndex(COLUMN_ANNOTATION);
        if (annotationCol >= 0) {
            setAnnotation(cursor.getString(annotationCol));
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTranslator() {
        return translator;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getTonalities() {
        return tonalities;
    }

    public void setTonalities(String tonalities) {
        this.tonalities = tonalities;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return "PsalmEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", locale='" + locale + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", translator='" + translator + '\'' +
                ", composer='" + composer + '\'' +
                ", year='" + year + '\'' +
                ", tonalities='" + tonalities + '\'' +
                ", annotation='" + annotation + '\'' +
                ", text=[" + text.substring(0, 25).replaceAll("\n", " ") + "... (" + text.length() + " symbols)]" +
                '}';
    }
}
