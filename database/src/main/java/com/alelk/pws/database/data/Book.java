package com.alelk.pws.database.data;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by alelkin on 31.03.2015.
 */
public class Book implements PwsObject {
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

    private SortedMap<Integer, Chapter> chapters = new TreeMap<>();
    private SortedMap<Integer, Psalm> psalms = new TreeMap<>();

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

    public Psalm getPsalm(int number) {
        return psalms.get(number);
    }

    public SortedMap<Integer, Psalm> getPsalms() {
        return psalms;
    }

    public void setPsalms(SortedMap<Integer, Psalm> psalms) {
        this.psalms = new TreeMap<>();
        if (psalms != null && psalms.size() > 0) {
            this.psalms.putAll(psalms);
        }
    }

    public SortedMap<Integer, Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(SortedMap<Integer, Chapter> chapters) {
        this.chapters = new TreeMap<>();
        if (chapters != null && chapters.size() > 0){
            this.chapters.putAll(chapters);
        }
    }

    public String toString() {
        String s = "Book v" + this.version + " {" +
                "name='" + this.name +
                "' shortName='" + this.shortName +
                "' displayName='" + this.displayName +
                "' description='" + this.description +
                "' edition='" + this.edition +
                "' releaseDate='" + this.releaseDate +
                "' comment='" + this.comment +
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
        s += "] chapters=[ ";
        if (this.chapters != null) {
            for (int key : this.chapters.keySet()) {
                s += chapters.get(key).getNumber() + "->'" + chapters.get(key).getName() + "' ";
            }
        }
        s += "] psalms=[ ";
        s += "countOfPsalms=" + this.psalms.size();
        s += " numbers: " + psalms.keySet();
        s += " ]}";
        return s;
    }
}
