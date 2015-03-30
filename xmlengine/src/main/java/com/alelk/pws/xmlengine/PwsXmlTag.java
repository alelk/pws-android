package com.alelk.pws.xmlengine;

/**
 * Created by alelkin on 25.03.2015.
 */
public interface PwsXmlTag {
    String toString();
    <T extends Enum<T>> T getEnumByText(String tagText);
}
