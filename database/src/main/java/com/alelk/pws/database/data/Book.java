package com.alelk.pws.database.data;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alex Elkin on 31.03.2015.
 */
public class Book extends BookInfo implements PwsObject {

    private SortedMap<Integer, Chapter> chapters = new TreeMap<>();
    private SortedMap<Integer, Psalm> psalms = new TreeMap<>();

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
        String s = "Book v" + super.getVersion() + " {" +
                "name='" + super.getName() +
                "' shortName='" + super.getShortName() +
                "' displayName='" + super.getDisplayName() +
                "' description='" + super.getDescription() +
                "' edition='" + super.getEdition() +
                "' releaseDate='" + super.getReleaseDate() +
                "' comment='" + super.getComment() +
                "' authors=[ ";
        if (super.getAuthors() != null) {
            for (String author : super.getAuthors()) {
                s += "'" + author + "' ";
            }
        }
        s += "] creators=[ ";
        if (super.getCreators() != null) {
            for (String creator : super.getCreators()) {
                s += "'" + creator + "' ";
            }
        }
        s += "] editors=[ ";
        if (super.getEditors() != null) {
            for (String editor : super.getEditors()) {
                s += "'" + editor + "' ";
            }
        }
        s += "] reviewers=[ ";
        if (super.getReviewers() != null) {
            for (String reviewer : super.getReviewers()) {
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
