package com.alelk.pws.database.data.entity;

/**
 * Created by Alex Elkin on 30.04.2015.
 */
public class ChapterEntity implements PwsDatabaseEntity {
    private long id;
    private long bookId;
    private long number;
    private String name;
    private String shortName;
    private String displayName;
    private String version;
    private String releaseDate;
    private String description;
    private String comment;

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

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "ChapterEntity{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", number=" + number +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", version='" + version + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", description='" + description + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
