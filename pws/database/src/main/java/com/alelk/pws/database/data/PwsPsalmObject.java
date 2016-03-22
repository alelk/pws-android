package com.alelk.pws.database.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;

/**
 * Created by Alex Elkin on 20.02.2016.
 */
public abstract class PwsPsalmObject {
    private String name;
    private String version;
    private String author;
    private String translator;
    private String composer;
    private String year;
    private String annotation;
    private Locale locale;
    private List<String> tonalities = new ArrayList<>();
    private SortedMap<Integer, PsalmPart> psalmParts;

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

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public static Comparator<PwsPsalmObject> getNameComparator() {
        return new Comparator<PwsPsalmObject>() {
            @Override
            public int compare(PwsPsalmObject lhs, PwsPsalmObject rhs) {
                String name1 = lhs.getName();
                String name2 = rhs.getName();
                if (name1 == null && name2 == null) return 0;
                else if (name1 == null) return -1;
                else if (name2 == null) return 1;
                return name1.compareTo(name2);
            }
        };
    }
}
