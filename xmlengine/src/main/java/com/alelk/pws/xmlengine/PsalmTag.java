package com.alelk.pws.xmlengine;

/**
 * Created by alelkin on 25.03.2015.
 */
public enum PsalmTag implements PwsXmlTag {
    START_TAG ("psalm"),
    END_TAG ("psalm"),
    AUTHOR ("author"),
    NUMBER ("number"),
    COMPOSER ("composer");

    private String tagText;

    PsalmTag(String tagText) {
        this.tagText = tagText;
    }

    public String toString() {
        return tagText;
    }

    public PsalmTag getEnumByText(String tagText) {
        for(PsalmTag psalmTag : PsalmTag.values()) {
            if (psalmTag.toString().equals(tagText)) {
                return psalmTag;
            }
        }
        return null;
    }
}
