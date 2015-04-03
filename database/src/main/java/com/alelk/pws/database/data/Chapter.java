package com.alelk.pws.database.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by alelkin on 31.03.2015.
 */
public class Chapter implements PwsObject {
    private String name;
    private int number;
    private String shortName;
    private String displayName;
    private String description;
    private String version;
    private Date releaseDate;
    private BookEdition bookEdition;
    private List<Integer> psalmNumbers = new ArrayList<>();
    private String comment;

    public Chapter(BookEdition bookEdition) {
        this.bookEdition = bookEdition;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<Integer> getPsalmNumbers() {
        return psalmNumbers;
    }

    public void setPsalmNumbers(List<Integer> psalmNumbers) {
        this.psalmNumbers = new ArrayList<>();
        if (psalmNumbers != null && psalmNumbers.size() > 0) {
            this.psalmNumbers.addAll(psalmNumbers);
        }
    }

    public void addPsalmNumber(int number) {
        this.psalmNumbers.add(number);
    }

    public void addPsalmNumber(Psalm psalm) {
        if (bookEdition != null) {
            this.psalmNumbers.add(psalm.getNumber(bookEdition));
        }
    }

    public BookEdition getBookEdition() {
        return bookEdition;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
