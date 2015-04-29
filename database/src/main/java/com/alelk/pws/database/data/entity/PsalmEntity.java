package com.alelk.pws.database.data.entity;

/**
 * Created by alelkin on 28.04.2015.
 */
public class PsalmEntity implements PwsDatabaseEntity{
    private long id;
    private String name;
    private String version;
    private String author;
    private String translator;
    private String composer;
    private String year;
    private String tonalities;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "PsalmEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", translator='" + translator + '\'' +
                ", composer='" + composer + '\'' +
                ", year='" + year + '\'' +
                ", tonalities='" + tonalities + '\'' +
                '}';
    }
}
