package com.alelk.pws.pwsdb.data;

import java.util.List;
import java.util.Locale;

/**
 * Created by Alex Elkin on 02.03.2016.
 */
public class BookInfo implements PwsObject {
    private String name;
    private String shortName;
    private String displayName;
    private String description;
    private String version;
    private BookEdition edition;
    private String releaseDate;
    private List<String> authors;
    private List<String> creators;
    private List<String> reviewers;
    private List<String> editors;
    private String comment;
    private Integer preference;
    private Locale locale;

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

    public BookEdition getEdition() {
        return edition;
    }

    public void setEdition(BookEdition edition) {
        this.edition = edition;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<String> getCreators() {
        return creators;
    }

    public void setCreators(List<String> creators) {
        this.creators = creators;
    }

    public List<String> getReviewers() {
        return reviewers;
    }

    public void setReviewers(List<String> reviewers) {
        this.reviewers = reviewers;
    }

    public List<String> getEditors() {
        return editors;
    }

    public void setEditors(List<String> editors) {
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

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String toString() {
        String s = "BookInfo v" + this.version + " {" +
                "name='" + this.name +
                "' shortName='" + this.shortName +
                "' displayName='" + this.displayName +
                "' description='" + this.description +
                "' edition='" + this.edition +
                "' releaseDate='" + this.releaseDate +
                "' comment='" + this.comment +
                "' preference='" + this.preference +
                "' locale='" + this.locale +
                "' authors=[ ";
        if (this.authors != null) {
            for (String author : this.authors) {
                s += "'" + author + "' ";
            }
        }
        s += "] creators=[ ";
        if (this.creators != null) {
            for (String creator : this.creators) {
                s += "'" + creator + "' ";
            }
        }
        s += "] editors=[ ";
        if (this.editors != null) {
            for (String editor : this.editors) {
                s += "'" + editor + "' ";
            }
        }
        s += "] reviewers=[ ";
        if (this.reviewers != null) {
            for (String reviewer : this.reviewers) {
                s += "'" + reviewer + "' ";
            }
        }
        s += "]}";
        return s;
    }
}
