package com.alelk.pws.database.data;

import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by alelkin on 25.03.2015.
 */
public class Psalm implements PwsObject {
    private String name;
    private String version;
    private String author;
    private String translator;
    private String composer;
    private Date year;
    private List<String> tonalities = new ArrayList<>();
    private Map<BookEdition, Integer> numbers;
    private SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();

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

    public Date getYear() {
        return year;
    }

    public void setYear(Date year) {
        this.year = year;
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

    public void addNumber(BookEdition bookEdition, int psalmNumber) throws PwsDatabaseIncorrectValueException {
        if (bookEdition == null || psalmNumber <= 0) {
            throw new PwsDatabaseIncorrectValueException();
        }
        if (numbers == null) {
            numbers = new HashMap<>();
        }
        numbers.put(bookEdition, psalmNumber);
    }

    public SortedMap<Integer, PsalmPart> getPsalmParts() {
        return psalmParts;
    }

    public void setPsalmParts(SortedMap<Integer, PsalmPart> psalmParts) {
        this.psalmParts = new TreeMap<>();
        if (psalmParts != null && psalmParts.size() > 0) {
            this.psalmParts.putAll(psalmParts);
        }
    }

    /**
     * Get psalm number of specified book edition
     * @param bookEdition book edition
     * @return psalm number if book edition is found for this psalm. Returns null otherwise.
     */
    public Integer getNumber(BookEdition bookEdition) {
        if (numbers == null || !numbers.containsKey(bookEdition)) {
            return null;
        }
        return numbers.get(bookEdition);
    }

    public Set<BookEdition> getBookEditions() {
        if (getNumbers() == null || getNumbers().isEmpty()) {
            return null;
        }
        return numbers.keySet();
    }

    public Map<BookEdition, Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(Map<BookEdition, Integer> numbers) {
        this.numbers = numbers;
    }

    public List<String> getTonalities() {
        return tonalities;
    }

    public void setTonalities(List<String> tonalities) {
        this.tonalities = new ArrayList<>();
        if (tonalities != null && tonalities.size() > 0) {
            this.tonalities.addAll(tonalities);
        }
    }

    public String toString() {
        String s = "Psalm v" + this.version + " {" +
                "name='" + this.name +
                "' author='" + this.author +
                "' translator='" + this.translator +
                "' composer='" + this.composer +
                "' year='" + this.year +
                "' number=[ ";
        if (numbers != null && !numbers.isEmpty()) {
            for (BookEdition bookEdition : this.numbers.keySet()) {
                s += this.numbers.get(bookEdition) + "->" + bookEdition + " ";
            }
        }
        s += "] " +
                "psalmPartsTypes=[ ";
        for (int i : psalmParts.keySet()) {
            s += "#" + i + "-" + psalmParts.get(i).getPsalmType() + " ";
        }
        s += "] " +
                "tonalities=[ ";
        for (String tonality : tonalities) {
            s += tonality + " ";
        }
        s += "]}";
        return s;
    }
}
