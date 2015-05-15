package com.alelk.pws.database.data;

import android.text.TextUtils;

import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private String year;
    private String annotation;
    private List<String> tonalities = new ArrayList<>();
    private Map<BookEdition, Integer> numbers;
    private SortedMap<Integer, PsalmPart> psalmParts;

    private static class NumberComparator implements Comparator<Psalm> {
        private BookEdition bookEdition;
        public NumberComparator(BookEdition bookEdition) {
            this.bookEdition = bookEdition;
        }
        @Override
        public int compare(Psalm psalm1, Psalm psalm2) {
            Integer psalmNumber1 = psalm1.getNumber(bookEdition);
            Integer psalmNumber2 = psalm2.getNumber(bookEdition);
            if (psalmNumber1 == null && psalmNumber2 == null) return 0;
            else if (psalmNumber1 == null) return -1;
            else if (psalmNumber2 == null) return 1;
            return psalmNumber1.compareTo(psalmNumber2);
        }
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
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

    /**
     * Get set of unique values of psalmParts
     * @return set of unique values of psalmParts
     */
    public Set<PsalmPart> getPsalmPartsValues() {
        Set<PsalmPart> psalmPartSet = null;
        if (psalmParts != null && !psalmParts.isEmpty()) {
            psalmPartSet = new HashSet<>();
            for (PsalmPart psalmPart : psalmParts.values()) {
                if(!psalmPartSet.contains(psalmPart)){
                    psalmPartSet.add(psalmPart);
                }
            }
        }
        return psalmPartSet;
    }

    public void setPsalmParts(SortedMap<Integer, PsalmPart> psalmParts) {
        this.psalmParts = psalmParts;
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

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setTonalities(List<String> tonalities) {
        this.tonalities = new ArrayList<>();
        if (tonalities != null && tonalities.size() > 0) {
            this.tonalities.addAll(tonalities);
        }
    }

    public static Comparator<Psalm> getNumberComparator(BookEdition bookEdition) {
        return new NumberComparator(bookEdition);
    }

    public static Comparator<Psalm> getNameComparator() {
        return new Comparator<Psalm>() {
            @Override
            public int compare(Psalm lhs, Psalm rhs) {
                String name1 = lhs.getName();
                String name2 = rhs.getName();
                if (name1 == null && name2 == null) return 0;
                else if (name1 == null) return -1;
                else if (name2 == null) return 1;
                return name1.compareTo(name2);
            }
        };
    }

    public String toString() {
        String s = "Psalm v" + this.version + " {" +
                "name='" + this.name +
                "' author='" + this.author +
                "' translator='" + this.translator +
                "' composer='" + this.composer +
                "' year='" + this.year +
                "' annotation='" + this.annotation +
                "' number=[ ";
        if (numbers != null && !numbers.isEmpty()) {
            for (BookEdition bookEdition : this.numbers.keySet()) {
                s += this.numbers.get(bookEdition) + "->" + bookEdition + " ";
            }
        }
        s += "] " +
                "psalmPartsTypes=[ ";
        if (psalmParts != null && !psalmParts.isEmpty()) {
            for (int i : psalmParts.keySet()) {
                s += "#" + i + "-" + psalmParts.get(i).getPsalmType() + " ";
            }
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
