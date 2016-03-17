package com.alelk.pws.database.data.entity;

import android.database.Cursor;

/**
 * Created by Alex Elkin on 23.04.2015.
 */
public class BookEntity implements PwsDatabaseEntity {
    private long id;
    private String name;
    private String shortName;
    private String displayName;
    private String description;
    private String version;
    private String edition;
    private String releaseDate;
    private String authors;
    private String creators;
    private String reviewers;
    private String editors;
    private String comment;
    private Integer preference;

    public BookEntity() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCreators() {
        return creators;
    }

    public void setCreators(String creators) {
        this.creators = creators;
    }

    public String getReviewers() {
        return reviewers;
    }

    public void setReviewers(String reviewers) {
        this.reviewers = reviewers;
    }

    public String getEditors() {
        return editors;
    }

    public void setEditors(String editors) {
        this.editors = editors;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getPreference() {
        return preference;
    }

    public void setPreference(Integer preference) {
        this.preference = preference;
    }

    @Override
    public String toString() {
        return "BookEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", version='" + version + '\'' +
                ", edition='" + edition + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", authors='" + authors + '\'' +
                ", creators='" + creators + '\'' +
                ", reviewers='" + reviewers + '\'' +
                ", editors='" + editors + '\'' +
                ", comment='" + comment + '\'' +
                ", preference='" + preference + '\'' +
                '}';
    }
}
