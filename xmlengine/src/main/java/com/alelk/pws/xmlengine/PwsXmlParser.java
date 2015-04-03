package com.alelk.pws.xmlengine;

import android.util.Log;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by alelkin on 25.03.2015.
 */
public class PwsXmlParser implements Constants {
    private static final String LOG_TAG = "PwsXmlParser";
    public static final String[] DATE_FORMATS = {"dd MMM yyyy", "MMM yyyy", "yyyy-mm-dd", "yyyy-mm", "yyyy"};
    public static final Locale DATE_LOCALE = Locale.US;

    public void parse() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        parserInitialize();

        //
        Psalm psalm;
        Book book;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_DOCUMENT) {
                Log.d("parser", "Start document");
            } else if (eventType == XmlPullParser.START_TAG) {

                String tagName = parser.getName();
                Log.d("parser", "StartTag:" + tagName);
                if (tagName.equalsIgnoreCase(TAG.PSLM.TAG)) {
                    psalm = parsePsalm();
                    if (psalm != null) {
                        Log.i("ParsePsalm: ", psalm.toString());
                    }
                }
                if (tagName.equalsIgnoreCase(TAG.BK.TAG)) {
                    book = parseBook();
                    if (book != null) {
                        Log.i("ParsePsalm: ", book.toString());
                    }
                }
            } else if (eventType == XmlPullParser.TEXT) {
                Log.d("parser", "Text:" + parser.getText());
            } else if (eventType == XmlPullParser.END_TAG) {
                Log.d("parser", "EndTag:" + parser.getName());
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                Log.d("parser", "EndDocument");
            }
            try {
                eventType = parser.next();
            } catch (Exception ex) {
                Log.w(LOG_TAG, "Parsing error: Position: line " + parser.getLineNumber() + " Exception: " + ex);
                throw ex;
            }
        }
    }

    private Book parseBook() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.BK.TAG)) return null;
        final Book book = new Book();
        List<Psalm> psalms = null;

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case TAG.BK.VERSION:
                    book.setVersion(parser.getAttributeValue(i));
                    break;
                case TAG.BK.NAME:
                    book.setName(parser.getAttributeValue(i));
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Book attribute name: '" + attributeName + "'");
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                TAG.BK.NAME,
                TAG.BK.VERSION,
                TAG.BK.SHORT_NAME,
                TAG.BK.DISPLAY_NAME,
                TAG.BK.DESCRIPTION,
                TAG.BK.EDITION,
                TAG.BK.RELEASE_DATE,
                TAG.BK.COMMENT,
                TAG.BK.AUTHORS,
                TAG.BK.CREATORS,
                TAG.BK.REVIEWERS,
                TAG.BK.EDITORS,
                TAG.BK.PSALMS));
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedStartTags.contains(tagName.toLowerCase())) {
                        currentTagName = tagName;
                        if (TAG.BK.AUTHORS.equalsIgnoreCase(tagName)) {
                            book.setAuthors(parsePeople());
                        } else if (TAG.BK.CREATORS.equalsIgnoreCase(tagName)) {
                            book.setCreators(parsePeople());
                        } else if (TAG.BK.REVIEWERS.equalsIgnoreCase(tagName)) {
                            book.setReviewers(parsePeople());
                        } else if (TAG.BK.EDITORS.equalsIgnoreCase(tagName)) {
                            book.setEditors(parsePeople());
                        } else if (TAG.BK.PSALMS.equalsIgnoreCase(tagName)) {
                            psalms = parsePsalms();
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (TAG.BK.NAME.equalsIgnoreCase(currentTagName)) {
                            book.setName(parser.getText());
                        } else if (TAG.BK.VERSION.equalsIgnoreCase(currentTagName)) {
                            book.setVersion(parser.getText());
                        } else if (TAG.BK.SHORT_NAME.equalsIgnoreCase(currentTagName)) {
                            book.setShortName(parser.getText());
                        } else if (TAG.BK.DISPLAY_NAME.equalsIgnoreCase(currentTagName)) {
                            book.setDisplayName(parser.getText());
                        } else if (TAG.BK.DESCRIPTION.equalsIgnoreCase(currentTagName)) {
                            book.setDescription(parser.getText());
                        } else if (TAG.BK.EDITION.equalsIgnoreCase(currentTagName)) {
                            book.setEdition(parseBookEdition(parser.getText()));
                        } else if (TAG.BK.RELEASE_DATE.equalsIgnoreCase(currentTagName)) {
                            book.setReleaseDate(parseDate(parser.getText()));
                        } else if (TAG.BK.COMMENT.equalsIgnoreCase(currentTagName)) {
                            book.setComment(parser.getText());
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: '" + parser.getText() + "'");
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(TAG.BK.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            "Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        return book;
    }

    private List<String> parsePeople() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                TAG.BK.CREATORS,
                TAG.BK.REVIEWERS,
                TAG.BK.EDITORS,
                TAG.BK.AUTHORS
        ));
        if (parser.getEventType() != XmlPullParser.START_TAG ) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    "Start tag is expected, but item is: " + parser.getEventType());
            return null;
        }
        String startTag = parser.getName();
        if (!allowedStartTags.contains(startTag)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    "Incorrect start tag: '" + parser.getEventType() + "'. " +
                    "Supported tags: " + allowedStartTags.toString());
            return null;
        }
        final List<String> allowedInnerTags = new ArrayList<>();
        if (TAG.BK.CREATORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(TAG.BK.CRTRS.CREATOR);
        } else if (TAG.BK.REVIEWERS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(TAG.BK.RVRS.REVIEWER);
        } else if (TAG.BK.EDITORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(TAG.BK.EDTRS.EDITOR);
        } else if (TAG.BK.AUTHORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(TAG.BK.AUTRS.AUTHOR);
        }
        int eventType;
        String person = null;
        List<String> people = new ArrayList<>();
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedInnerTags.contains(tagName)) {
                        currentTagName = tagName;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && allowedInnerTags.contains(currentTagName)) {
                        person = parser.getText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (allowedStartTags.contains(tagName)) {
                        done = true;
                    } else if (allowedInnerTags.contains(tagName)) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (person != null) {
                            people.add(person);
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Person has malformed body.");
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        if (people.size() == 0) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Cannot parse no one person.");
        }
        return people;
    }

    private List<Psalm> parsePsalms() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.BK.PSLMS.TAG)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": The start Psalms tag is expected, but there is: '" + parser.getName() + "'");
            return null;
        }

        Psalm psalm = null;
        final List<Psalm> psalms = new ArrayList<>();
        final List<String> allowedInnerTags = new ArrayList<>(Arrays.asList(
                TAG.BK.PSLMS.PSALM,
                TAG.BK.PSLMS.REF
        ));
        String ref;
        int eventType;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        currentTagName = tagName;
                        if (TAG.BK.PSLMS.PSALM.equalsIgnoreCase(tagName)) {
                            psalm = parsePsalm();
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalms tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (TAG.BK.PSLMS.REF.equalsIgnoreCase(currentTagName)) {
                            ref = parser.getText();
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (TAG.BK.PSLMS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (TAG.BK.PSLMS.REF.equalsIgnoreCase(tagName)) {
                            // todo open file and parse psalm
                        }
                        if (psalm != null) {
                            Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": New psalm parsed: " + psalm.toString());
                            psalms.add(psalm);
                            psalm = null;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Psalms section has malformed body.");
                        }
                    }  else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        if (psalms.size() == 0) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Cannot parse no one psalm.");
        }
        Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                ": " + psalms.size() + " psalms have been parsed in the current section.");
        return psalms;
    }

    private Psalm parsePsalm() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.PSLM.TAG)) return null;
        Psalm psalm = new Psalm();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case TAG.PSLM.VERSION:
                    psalm.setVersion(parser.getAttributeValue(i));
                    break;
                case TAG.PSLM.NAME:
                    psalm.setName(parser.getAttributeValue(i));
                    break;
                case TAG.PSLM.AUTHOR:
                    psalm.setAuthor(parser.getAttributeValue(i));
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Psalm attribute name: " + attributeName);
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                TAG.PSLM.AUTHOR,
                TAG.PSLM.NAME,
                TAG.PSLM.TRANSLATOR,
                TAG.PSLM.NUMBERS,
                TAG.PSLM.YEAR,
                TAG.PSLM.COMPOSER,
                TAG.PSLM.TEXT,
                TAG.PSLM.TONALITIES));
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedStartTags.contains(tagName.toLowerCase())) {
                        currentTagName = tagName;
                        if (TAG.PSLM.NUMBERS.equalsIgnoreCase(tagName)) {
                            parsePsalmNumbers(psalm);
                        } else if (TAG.PSLM.TONALITIES.equalsIgnoreCase(tagName)) {
                            parsePsalmTonalities(psalm);
                        }else if (TAG.PSLM.TEXT.equalsIgnoreCase(tagName)) {
                            parsePsalmText(psalm);
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm tag: " + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (currentTagName.equals(TAG.PSLM.AUTHOR)) {
                            psalm.setAuthor(parser.getText());
                        } else if (currentTagName.equals(TAG.PSLM.TRANSLATOR)) {
                            psalm.setTranslator(parser.getText());
                        } else if (TAG.PSLM.COMPOSER.equalsIgnoreCase(currentTagName)) {
                            psalm.setComposer(parser.getText());
                        } else if (TAG.PSLM.YEAR.equalsIgnoreCase(currentTagName)) {
                            psalm.setYear(parseDate(parser.getText()));
                        } else if (TAG.PSLM.NAME.equalsIgnoreCase(currentTagName)) {
                            psalm.setName(parser.getText());
                        }else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(TAG.PSLM.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm tag: " + tagName);
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            "Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        return psalm;
    }

    private void parsePsalmNumbers(Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.PSLM.NUMS.TAG)) return;

        int eventType;
        Map<BookEdition, Integer> psalmNumbers = new HashMap<>();
        BookEdition bookEdition = null;
        Integer psalmNumber = null;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        bookEdition = null;
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attributeName = parser.getAttributeName(i);
                            if (TAG.PSLM.EDITION.equalsIgnoreCase(attributeName)) {
                                bookEdition = parseBookEdition(parser.getAttributeValue(i));
                            } else {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Unknown Psalm Number attribute name: '" + attributeName);
                            }
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Numbers tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null) {
                        if (TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(currentTagName) && bookEdition != null) {
                            psalmNumber = null;
                            try {
                                psalmNumber = Integer.parseInt(parser.getText());
                            } catch (NumberFormatException ex) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Cannot parse Psalm Number: '" + parser.getText() + "'");
                            }
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.NUMS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(tagName)) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (bookEdition != null && psalmNumber != null) {
                            psalmNumbers.put(bookEdition, psalmNumber);
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Psalm number has malformed body. Edition='" + bookEdition +
                                    "' Number='" + psalmNumber);
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Numbers tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        if (psalmNumbers.size() > 0) {
            psalm.setNumbers(psalmNumbers);
        } else Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                ": Cannot parse no one psalm numbers.");
    }

    private BookEdition parseBookEdition(String s) {
        BookEdition bookEdition = null;
        switch (s.trim().toLowerCase()){
            case ATTR_VAL.BK.EDITION_GUSLI:
                bookEdition = BookEdition.GUSLI;
                break;
            case ATTR_VAL.BK.EDITION_PV2000:
                bookEdition = BookEdition.PV2000;
                break;
            default:
                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                        ": Unknown Book Edition: '" + s + "'");
        }
        return bookEdition;
    }

    private void parsePsalmTonalities(Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.PSLM.TONS.TAG)) return;

        int eventType;
        String tonality = null;
        List<String> tonalities = new ArrayList<>();
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Tonalities tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(currentTagName)) {
                        tonality = parser.getText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.TONS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(tagName)) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (tonality != null) {
                            tonalities.add(tonality);
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Psalm tonality has malformed body.");
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Tonalities tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        if (tonalities.size() > 0) {
            psalm.setTonalities(tonalities);
        } else Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                ": Cannot parse no one psalm tonality.");
    }

    private void parsePsalmText(Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !TAG.PSLM.TXT.TAG.equalsIgnoreCase(parser.getName())) return;

        int eventType;
        List<PsalmVerse> psalmVerses = new ArrayList<>();
        List<PsalmChorus> psalmChoruses = new ArrayList<>();
        SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();
        PsalmVerse psalmVerse = null;
        PsalmChorus psalmChorus = null;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.TXT.VERSE.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        psalmVerse = new PsalmVerse();
                        psalmVerse.setNumbers(parseNumbersAttribute(TAG.PSLM.TXT.VERSE));
                    } else  if (TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        psalmChorus = new PsalmChorus();
                        psalmChorus.setNumbers(parseNumbersAttribute(TAG.PSLM.TXT.VERSE));
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Text tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null) {
                        if (TAG.PSLM.TXT.VERSE.equalsIgnoreCase(currentTagName)) {
                            psalmVerse.setText(psalmVerse.getText() + parser.getText());
                        } else if (TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(currentTagName)) {
                            psalmChorus.setText(psalmChorus.getText() + parser.getText());
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected Text: '" + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (TAG.PSLM.TXT.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (TAG.PSLM.TXT.VERSE.equalsIgnoreCase(tagName)) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (psalmVerse != null) {
                            psalmVerses.add(psalmVerse);
                            psalmVerse = null;
                        }
                    } else if (TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (psalmChorus != null) {
                            psalmChoruses.add(psalmChorus);
                            psalmChorus = null;
                        }
                    }else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Text tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        for (PsalmVerse pv : psalmVerses) {
            if (pv == null || pv.getNumbers() == null) continue;
            for (int i : pv.getNumbers()) {
                if (psalmParts.containsKey(i)) {
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": The Psalm already contains the Psalm Part with number " + i +
                            ". The Psalm Part will be overwritten.");
                }
                psalmParts.put(i, pv);
            }
        }
        for (PsalmChorus pc : psalmChoruses) {
            if (pc == null || pc.getNumbers() == null) continue;
            for (int i : pc.getNumbers()) {
                if (psalmParts.containsKey(i)) {
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": The Psalm already contains the Psalm Part with number " + i +
                            ". The Psalm Part will be overwritten.");
                }
                psalmParts.put(i, pc);
            }
        }
        psalm.setPsalmParts(psalmParts);
    }

    private List<Integer> parseNumbersAttribute(String tagName) throws XmlPullParserException {
        final List<String> allowedTags = new ArrayList<>(Arrays.asList(
                TAG.PSLM.TXT.CHORUS,
                TAG.PSLM.TXT.VERSE ));
        final List<String> allowedAttributeNames = new ArrayList<>(Arrays.asList(
                TAG.PSLM.TXT.CRS.NUMBER,
                TAG.PSLM.TXT.VRS.NUMBER ));
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !allowedTags.contains(parser.getName().toLowerCase()) ||
                tagName == null) {
            return null;
        }
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = parser.getAttributeName(i);
            if (allowedAttributeNames.contains(attrName.toLowerCase())) {
                String[] items = parser.getAttributeValue(i).replaceAll(" ", "").split(",");
                for (String item : items) {
                    try {
                        int number = Integer.parseInt(item.trim());
                        if (number > 0) {
                            numbers.add(number);
                        }
                    } catch (NumberFormatException ex) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Cannot parse number: '" + item);
                    }
                }
            } else {
                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                        ": Unexpected attribute name: '" + attrName +
                        "' (expected: '" + tagName + "')");
            }
        }
        if (numbers.size() == 0) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": No parsed numbers");
        }
        return numbers;
    }

    private void verifyEndTag(String endTagName, String currentTagName) throws PwsXmlParserIncorrectSourceFormatException {
        if (endTagName == null || currentTagName == null ||
                !endTagName.equalsIgnoreCase(currentTagName)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Incorrect end tag: " + endTagName +
                    "(expected tag: '" + currentTagName + "')");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
    }

    private Date parseDate(String text) {
        Date date = null;
        for (String format : DATE_FORMATS) {
            try {
                DateFormat dateFormat = new SimpleDateFormat(format, DATE_LOCALE);
                date = dateFormat.parse(text);
                return date;
            } catch (ParseException ex) {
            }
        }
        return date;
    }

    private void parserInitialize() throws XmlPullParserException, PwsXmlParserIncorrectSourceFormatException {
        if (parserInputStreamReader == null){
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
        parser.setInput(parserInputStreamReader);
    }

    public InputStreamReader getParserInputStreamReader() {
        return parserInputStreamReader;
    }

    public void setParserInputStreamReader(InputStreamReader parserInputStreamReader) {
        this.parserInputStreamReader = parserInputStreamReader;
    }

    private InputStreamReader parserInputStreamReader;
    private XmlPullParser parser;
}
