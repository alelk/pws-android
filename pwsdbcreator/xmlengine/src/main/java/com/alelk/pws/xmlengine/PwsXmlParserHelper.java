package com.alelk.pws.xmlengine;

import android.util.Log;

import com.alelk.pws.pwsdb.data.Book;
import com.alelk.pws.pwsdb.data.BookEdition;
import com.alelk.pws.pwsdb.data.Chapter;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.data.PsalmChorus;
import com.alelk.pws.pwsdb.data.PsalmPart;
import com.alelk.pws.pwsdb.data.PsalmVerse;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlEngineIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Contains the functionality for Pws Book parsing
 *
 * Created by Alex Elkin on 06.04.2015.
 */
public abstract class PwsXmlParserHelper implements Constants {
    private static final String LOG_TAG = "PwsXmlParserHelper";
    public static final String[] DATE_FORMATS = {"dd MMM yyyy", "MMM yyyy", "yyyy-mm-dd", "yyyy-mm", "yyyy"};

    /**
     * Locale used to date parsing.
     */
    public static final Locale DATE_LOCALE = Locale.US;
    private static final String NUMBERS_SPLIT_REGEX = "[^0-9]";

    /**
     * Returns InputStreamReader for file named filename
     * @param filename The name of PWS Library file. E.q. content.pwslib
     * @return InputStreamReader for PWS Library file
     * @throws com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException if cannot open the file
     */
    protected abstract InputStreamReader openPwsLibraryFile(String filename) throws PwsXmlParserFileNotFoundException;

    /**
     * Returns InputStreamReader for file named filename
     * @param filename The name of PWS Book file. E.q. book.pws
     * @return InputStreamReader for PWS Book file
     * @throws com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException if cannot open the file
     */
    protected abstract InputStreamReader openPwsBookFile(String filename) throws PwsXmlParserFileNotFoundException;

    /**
     * Returns InputStreamReader for file named filename
     * @param filename The name of PWS Psalm file. E.q. psalm.pslm
     * @return InputStreamReader for PWS Psalm file
     * @throws com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException if cannot open the file
     */
    protected abstract InputStreamReader openPwsPsalmFile(String filename) throws PwsXmlParserFileNotFoundException;

    /**
     * Parse the Pws Library file named filename.
     * @param filename the name of PWS Library file.
     * @return list of parsed books
     * @throws PwsXmlEngineIncorrectValueException if incorrect filename
     * @throws PwsXmlParserIncorrectSourceFormatException if exception with parser was occurred.
     * @throws PwsXmlParserFileNotFoundException if cannot open Pws Library file
     */
    protected List<Book> parseLibrary(String filename) throws PwsXmlEngineIncorrectValueException, PwsXmlParserFileNotFoundException, PwsXmlParserIncorrectSourceFormatException {
        Log.i(LOG_TAG, "Start parsing PWS library file: '" + filename + "'");
        List<Book> books = null;
        if (filename == null || filename.isEmpty()) {
            throw new PwsXmlEngineIncorrectValueException();
        }
        InputStreamReader inputStreamReader = openPwsLibraryFile(filename);
        XmlPullParser parser;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStreamReader);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        try {
            boolean isDone = false;
            String tagName;

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (TAG.LIB.TAG.equalsIgnoreCase(tagName)) {
                            if (isDone == true) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Duplicate pwslibrary tag: '" + parser.getName() + "'" +
                                        ". Library will be overwritten.");
                                isDone = false;
                            }
                            try {
                                books = parseLibrary(parser);
                                continue;
                            } catch (PwsDatabaseIncorrectValueException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    case XmlPullParser.TEXT:
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected text: '" + parser.getText() + "'");
                        break;
                    case XmlPullParser.END_TAG:
                        tagName = parser.getName();
                        if (TAG.LIB.TAG.equalsIgnoreCase(tagName)) {
                            isDone = true;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    default:
                }
                parser.next();
            }
            if (!isDone) {
                Log.w(LOG_TAG, "Incorrect file format: " + filename);
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        } catch (IOException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        Log.i(LOG_TAG, "End parsing library file: '" + filename + "'. " + (books != null ? books.size() : "0") + " books have been parsed.");
        return books;
    }

    /**
     * Parse the Version value of Pws Library file named filename.
     * @param filename the name of PWS Library file.
     * @return the Version of Pws Library
     * @throws PwsXmlEngineIncorrectValueException if incorrect filename
     * @throws PwsXmlParserIncorrectSourceFormatException if exception with parser was occurred.
     * @throws PwsXmlParserFileNotFoundException if cannot open Pws Library file
     */
    protected String parseLibraryVersion(String filename) throws PwsXmlEngineIncorrectValueException, PwsXmlParserFileNotFoundException, PwsXmlParserIncorrectSourceFormatException, IOException {
        Log.i(LOG_TAG, "Start parsing the version value from PWS library file: '" + filename + "'");
        if (filename == null || filename.isEmpty()) {
            throw new PwsXmlEngineIncorrectValueException();
        }
        InputStreamReader inputStreamReader = null;
        XmlPullParserFactory factory = null;
        XmlPullParser parser = null;
        String version = null;
        try {
            inputStreamReader = openPwsLibraryFile(filename);
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStreamReader);
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT
                    && !(parser.getEventType() == XmlPullParser.START_TAG && TAG.LIB.TAG.equalsIgnoreCase(parser.getName()))) {
                parser.next();
            }

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attributeName = parser.getAttributeName(i);
                switch (attributeName) {
                    case TAG.LIB.VERSION:
                        version = parser.getAttributeValue(i);
                        break;
                    default:
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unknown Library attribute name: '" + attributeName + "'");
                }
            }
            Log.i(LOG_TAG, "End parsing library file: '" + filename + "'. Version=" + version);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        } finally {
            if (inputStreamReader != null) inputStreamReader.close();
        }

        return version;
    }

    /**
     * Parse the Pws Book file named filename.
     * @param filename the name of Pws Book file
     * @return Book
     * @throws PwsXmlEngineIncorrectValueException if incorrect filename
     * @throws PwsXmlParserIncorrectSourceFormatException if exception with parser was occurred.
     * @throws PwsXmlParserFileNotFoundException if cannot open Pws Book file
     */
    protected Book parseBook(String filename) throws PwsXmlEngineIncorrectValueException, PwsXmlParserIncorrectSourceFormatException, PwsXmlParserFileNotFoundException {
        Log.i(LOG_TAG, "Start parsing book file: '" + filename + "'");
        Book book = null;
        if (filename == null || filename.isEmpty()) {
            throw new PwsXmlEngineIncorrectValueException();
        }
        InputStreamReader inputStreamReader = openPwsBookFile(filename);
        XmlPullParser parser;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStreamReader);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        try {
            boolean isDone = false;
            String tagName;

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (TAG.BK.TAG.equalsIgnoreCase(tagName)) {
                            if (isDone == true) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Duplicate book tag: '" + parser.getName() + "'" +
                                        ". Book will be overwritten.");
                                isDone = false;
                            }
                            try {
                                book = parseBook(parser);
                                continue;
                            } catch (PwsDatabaseIncorrectValueException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    case XmlPullParser.TEXT:
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected text: '" + parser.getText() + "'");
                        break;
                    case XmlPullParser.END_TAG:
                        tagName = parser.getName();
                        if (TAG.BK.TAG.equalsIgnoreCase(tagName)) {
                            isDone = true;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    default:
                }
                parser.next();
            }
            if (!isDone) {
                Log.w(LOG_TAG, "Incorrect file format: " + filename);
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred. File: '" + filename + "' Message: " + e.getMessage());
            throw new PwsXmlParserIncorrectSourceFormatException();
        } catch (IOException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred. File: '" + filename + "' Message: " + e.getMessage());
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        Log.i(LOG_TAG, "End parsing book file: '" + filename + "'");
        return book;
    }

    /**
     * Parse the Pws Psalm file named filename.
     * @param filename the name of Pws Psalm file
     * @return Pws Psalm
     * @throws PwsXmlEngineIncorrectValueException if incorrect filename
     * @throws PwsXmlParserIncorrectSourceFormatException if exception with parser was occurred.
     * @throws PwsXmlParserFileNotFoundException if cannot open Pws Psalm file
     */
    protected Psalm parsePsalm(String filename) throws PwsXmlEngineIncorrectValueException, PwsXmlParserIncorrectSourceFormatException, PwsXmlParserFileNotFoundException {
        Log.i(LOG_TAG, "Start parsing psalm file: '" + filename + "'");
        Psalm psalm = null;
        if (filename == null || filename.isEmpty()) {
            throw new PwsXmlEngineIncorrectValueException();
        }
        InputStreamReader inputStreamReader = openPwsPsalmFile(filename);
        XmlPullParser parser;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStreamReader);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        try {
            boolean isDone = false;
            String tagName;

            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (TAG.PSLM.TAG.equalsIgnoreCase(tagName)) {
                            if (isDone == true) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Duplicate psalm tag: '" + parser.getName() + "'" +
                                        ". Psalm will be overwritten.");
                                isDone = false;
                            }
                            try {
                                psalm = parsePsalm(parser);
                                continue;
                            } catch (PwsDatabaseIncorrectValueException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    case XmlPullParser.TEXT:
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected text: '" + parser.getText() + "'");
                        break;
                    case XmlPullParser.END_TAG:
                        tagName = parser.getName();
                        if (TAG.PSLM.TAG.equalsIgnoreCase(tagName)) {
                            isDone = true;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected tag: '" + tagName + "'");
                        }
                        break;
                    default:
                }
                parser.next();
            }
            if (isDone != true) {
                Log.w(LOG_TAG, "Incorrect file format: " + filename);
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        } catch (IOException e) {
            Log.e(LOG_TAG, "The exception with parser was occurred.");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        Log.i(LOG_TAG, "End parsing psalm file: '" + filename + "'");
        return psalm;
    }

    private List<Book> parseLibrary(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.LIB.TAG)) return null;
        List<Book> books = null;

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case TAG.LIB.VERSION:
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Library attribute name: '" + attributeName + "'");
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                TAG.LIB.VERSION,
                TAG.LIB.BOOKS
        ));
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
                        if (TAG.LIB.BOOKS.equalsIgnoreCase(tagName)) {
                            books = parseBooks(parser);
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Library tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (TAG.LIB.VERSION.equalsIgnoreCase(currentTagName)) {
                            parser.getText();
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: '" + parser.getText() + "'");
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(TAG.LIB.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Library tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            "Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        if (done == true) {
            if (books == null || books.size() == 0)
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": No books parsed");
        }
        return books;
    }

    private List<Book> parseBooks(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(TAG.LIB.BKS.TAG)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": The start Books tag is expected, but there is: '" + parser.getName() + "'");
            return null;
        }

        Book book = null;
        final List<Book> books = new ArrayList<>();
        final List<String> allowedInnerTags = new ArrayList<>(Arrays.asList(
                TAG.LIB.BKS.REF
        ));
        String ref = null;
        Integer preference = null;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        parser.next();
        while (!done) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        currentTagName = tagName;
                        if (TAG.LIB.BKS.REF.equalsIgnoreCase(tagName)) {
                            preference = null;
                            ref = null;
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attributeName = parser.getAttributeName(i);
                                switch (attributeName) {
                                    case TAG.BK.PREFERENCE:
                                        try {
                                            preference = Integer.parseInt(parser.getAttributeValue(i));
                                        } catch (NumberFormatException ex) {
                                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                                    ": Cannot parse book preference value: '" +
                                                    parser.getAttributeValue(i) + "'. Preference should be decimal.");
                                        }
                                        break;
                                    default:
                                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                                ": Unknown Book attribute name: '" + attributeName + "'");
                                }
                            }
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Books tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (TAG.LIB.BKS.REF.equalsIgnoreCase(currentTagName)) {
                            ref = parser.getText();
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (TAG.LIB.BKS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                        if (TAG.LIB.BKS.REF.equalsIgnoreCase(tagName)) {
                            try {
                                book = parseBook(ref);
                            } catch (PwsXmlEngineIncorrectValueException e) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Incorrect pws book filename: '" + ref + "'");
                            } catch (PwsXmlParserFileNotFoundException e) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": File not found: '" + ref + "'");
                            }
                        }
                        if (book != null) {
                            if (preference!=null) {
                                book.setPreference(preference);
                            }
                            Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": New book parsed: " + book.toString());
                            books.add(book);
                            book = null;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Books section has malformed body.");
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
            parser.next();
        }
        if (books.size() == 0) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Cannot parse no one book.");
        }
        Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                ": " + books.size() + " books have been parsed in the current section.");
        return books;
    }

    private Book parseBook(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(Constants.TAG.BK.TAG)) return null;
        final Book book = new Book();
        List<Psalm> psalms = null;
        final SortedMap<Integer, Chapter> chapters = new TreeMap<>();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case TAG.BK.VERSION:
                    book.setVersion(parser.getAttributeValue(i));
                    break;
                case TAG.BK.NAME:
                    book.setName(parser.getAttributeValue(i));
                    break;
                case TAG.BK.EDITION:
                    book.setEdition(parseBookEdition(parser, parser.getAttributeValue(i)));
                    break;
                case TAG.BK.LANGUAGE:
                    String locale = parser.getAttributeValue(i);
                    book.setLocale(new Locale(locale));
                    break;
                case TAG.BK.PREFERENCE:
                    try {
                        book.setPreference(Integer.parseInt(parser.getAttributeValue(i)));
                    } catch (NumberFormatException ex) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Cannot parse book preference value: '" +
                                parser.getAttributeValue(i) + "'. Preference should be decimal.");
                    }
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Book attribute name: '" + attributeName + "'");
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.BK.NAME,
                Constants.TAG.BK.VERSION,
                Constants.TAG.BK.DISPLAY_SHORT_NAME,
                Constants.TAG.BK.DISPLAY_NAME,
                Constants.TAG.BK.DESCRIPTION,
                Constants.TAG.BK.EDITION,
                Constants.TAG.BK.RELEASE_DATE,
                Constants.TAG.BK.COMMENT,
                TAG.BK.PREFERENCE,
                TAG.BK.LANGUAGE,
                Constants.TAG.BK.AUTHORS,
                Constants.TAG.BK.CREATORS,
                Constants.TAG.BK.REVIEWERS,
                Constants.TAG.BK.EDITORS,
                Constants.TAG.BK.PSALMS,
                Constants.TAG.BK.CHAPTERS));
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
                        if (Constants.TAG.BK.AUTHORS.equalsIgnoreCase(tagName)) {
                            book.setAuthors(parsePeople(parser));
                        } else if (Constants.TAG.BK.CREATORS.equalsIgnoreCase(tagName)) {
                            book.setCreators(parsePeople(parser));
                        } else if (Constants.TAG.BK.REVIEWERS.equalsIgnoreCase(tagName)) {
                            book.setReviewers(parsePeople(parser));
                        } else if (Constants.TAG.BK.EDITORS.equalsIgnoreCase(tagName)) {
                            book.setEditors(parsePeople(parser));
                        } else if (Constants.TAG.BK.PSALMS.equalsIgnoreCase(tagName)) {
                            psalms = parsePsalms(parser);
                        } else if (Constants.TAG.BK.CHAPTERS.equalsIgnoreCase(tagName)) {
                            if (book.getEdition() != null) {
                                parseChapters(parser, chapters, book.getEdition());
                            } else {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": No book edition parsed. Cannot parse chapter without" +
                                        " book edition. Please check that <" + Constants.TAG.BK.EDITION +
                                        "> tag is above than <" + Constants.TAG.BK.CHAPTERS + "> tag.");
                            }
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (Constants.TAG.BK.NAME.equalsIgnoreCase(currentTagName)) {
                            book.setName(parser.getText());
                        } else if (Constants.TAG.BK.VERSION.equalsIgnoreCase(currentTagName)) {
                            book.setVersion(parser.getText());
                        } else if (Constants.TAG.BK.DISPLAY_SHORT_NAME.equalsIgnoreCase(currentTagName)) {
                            book.setShortName(parser.getText());
                        } else if (Constants.TAG.BK.DISPLAY_NAME.equalsIgnoreCase(currentTagName)) {
                            book.setDisplayName(parser.getText());
                        } else if (Constants.TAG.BK.DESCRIPTION.equalsIgnoreCase(currentTagName)) {
                            book.setDescription(parser.getText());
                        } else if (Constants.TAG.BK.EDITION.equalsIgnoreCase(currentTagName)) {
                            book.setEdition(parseBookEdition(parser, parser.getText()));
                        } else if (Constants.TAG.BK.RELEASE_DATE.equalsIgnoreCase(currentTagName)) {
                            String date = parser.getText();
                            validateDateFormat(parser, date);
                            book.setReleaseDate(date);
                        } else if (Constants.TAG.BK.COMMENT.equalsIgnoreCase(currentTagName)) {
                            book.setComment(parser.getText());
                        } else if (TAG.BK.LANGUAGE.equalsIgnoreCase(currentTagName)) {
                            book.setLocale(new Locale(parser.getText()));
                        } else if (TAG.BK.PREFERENCE.equalsIgnoreCase(currentTagName)) {
                            try {
                                book.setPreference(Integer.parseInt(parser.getText()));
                            } catch (NumberFormatException ex) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Cannot parse book preference value: '" +
                                        parser.getText() + "'. Preference should be decimal.");
                            }
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: '" + parser.getText() + "'");
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(Constants.TAG.BK.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
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
        if (done == true) {
            if (psalms != null && psalms.size() > 0) {
                BookEdition bookEdition = book.getEdition();
                SortedMap<Integer, Psalm> bookPsalms = new TreeMap<>();
                for(Psalm psalm : psalms) {
                    Integer psalmNumber = psalm.getNumber(bookEdition);
                    if (psalmNumber == null || psalmNumber == 0){
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": The psalm '" + psalm.getName() + "' has no number for the " +
                                "current book edition: '" + bookEdition + "'");
                    } else if (bookPsalms.containsKey(psalmNumber)) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": The book already contains the psalm with number '" + psalmNumber
                                + "'. The psalm will be overwritten.");
                    } else {
                        if (psalm.getLocale() == null) {
                            psalm.setLocale(book.getLocale());
                        }
                        bookPsalms.put(psalm.getNumber(bookEdition), psalm);
                    }
                }
                book.setPsalms(bookPsalms);
            }
            Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": New book parsed: " + book.toString());
        }
        return book;
    }

    private void parseChapters (XmlPullParser parser, SortedMap<Integer, Chapter> chapters, BookEdition bookEdition) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        final String methodName = "parseChapters";
        if (bookEdition == null || chapters == null) {
            Log.e(LOG_TAG, "Error in method " + methodName +
                    ": Incorrect parameters value: chapters=" + chapters +
                    " bookEdition=" + bookEdition);
            return;
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !Constants.TAG.BK.CHPRS.TAG.equalsIgnoreCase(parser.getName())) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    "Start tag '" + Constants.TAG.BK.CHPRS.TAG +
                    "' is expected, but tag is: " + parser.getName());
            return;
        }
        Chapter chapter = null;
        int eventType;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        while (!done) {
            eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (Constants.TAG.BK.CHPRS.CHAPTER.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        chapter = parseChapter(parser, bookEdition);
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book Chapters tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && ! parser.isWhitespace()) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book Chapters text: '" + parser.getText() + "'");
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (Constants.TAG.BK.CHPRS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (Constants.TAG.BK.CHPRS.CHAPTER.equalsIgnoreCase(tagName)) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                        if (chapter != null) {
                            int number = chapter.getNumber();
                            if (chapters.containsKey(chapter.getNumber())) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Book already contains the Chapter with number: "
                                        + number + ". Chapter will be overwritten.");
                            }
                            if (number != 0) {
                                chapters.put(chapter.getNumber(), chapter);
                            } else {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Chapter number equals 0. Chapter has been skipped.");
                            }
                            chapter = null;
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Book Chapter section has malformed body.");
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Book Chapters tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() + ": Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
    }

    private Chapter parseChapter(XmlPullParser parser, BookEdition bookEdition) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (bookEdition == null) return null;
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !Constants.TAG.BK.CHPRS.CHPTR.TAG.equalsIgnoreCase(parser.getName())) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    "Start tag '" + Constants.TAG.BK.CHPRS.CHPTR.TAG +
                    "' is expected, but tag is: " + parser.getName());
            return null;
        }
        final Chapter chapter = new Chapter(bookEdition);
        final Set<Integer> psalmNumbers = new HashSet<>();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case Constants.TAG.BK.CHPRS.CHPTR.VERSION:
                    chapter.setVersion(parser.getAttributeValue(i));
                    break;
                case Constants.TAG.BK.CHPRS.CHPTR.NUMBER:
                    String number = parser.getAttributeValue(i);
                    try {

                        chapter.setNumber(Integer.parseInt(number));
                    } catch (NumberFormatException ex) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Cannot parse Chapter number: '" + number + "'");
                    }
                    break;
                case Constants.TAG.BK.CHPRS.CHPTR.NAME:
                    chapter.setName(parser.getAttributeValue(i));
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Chapter attribute name: '" + attributeName + "'");
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.BK.CHPRS.CHPTR.VERSION,
                Constants.TAG.BK.CHPRS.CHPTR.NUMBER,
                Constants.TAG.BK.CHPRS.CHPTR.NAME,
                Constants.TAG.BK.CHPRS.CHPTR.SHORT_NAME,
                Constants.TAG.BK.CHPRS.CHPTR.DISPLAY_NAME,
                Constants.TAG.BK.CHPRS.CHPTR.VERSION,
                Constants.TAG.BK.CHPRS.CHPTR.RELEASE_DATE,
                Constants.TAG.BK.CHPRS.CHPTR.DESCRIPTION,
                Constants.TAG.BK.CHPRS.CHPTR.PSALM_NUMBERS));
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
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Chapter tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (Constants.TAG.BK.CHPRS.CHPTR.VERSION.equalsIgnoreCase(currentTagName)) {
                            chapter.setVersion(parser.getText());
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.NUMBER.equalsIgnoreCase(currentTagName)) {
                            final String number = parser.getText();
                            try {
                                chapter.setNumber(Integer.parseInt(number));
                            } catch (NumberFormatException ex) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Cannot parse Chapter number: '" + number + "'");
                            }
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.NAME.equalsIgnoreCase(currentTagName)) {
                            chapter.setName(parser.getText());
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.SHORT_NAME.equalsIgnoreCase(currentTagName)) {
                            chapter.setShortName(parser.getText());
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.DISPLAY_NAME.equalsIgnoreCase(currentTagName)) {
                            chapter.setDisplayName(parser.getText());
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.RELEASE_DATE.equalsIgnoreCase(currentTagName)) {
                            final String date = parser.getText();
                            validateDateFormat(parser, date);
                            chapter.setReleaseDate(date);
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.DESCRIPTION.equalsIgnoreCase(currentTagName)) {
                            chapter.setDescription(parser.getText());
                        } else if (Constants.TAG.BK.CHPRS.CHPTR.PSALM_NUMBERS.equalsIgnoreCase(currentTagName)) {
                            psalmNumbers.addAll(parseNumbersFromText(parser.getText()));
                        }else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: '" + parser.getText() + "'");
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(Constants.TAG.BK.CHPRS.CHPTR.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Chapter tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            "Unexpected document ending.");
                    throw new PwsXmlParserIncorrectSourceFormatException();
                default:
            }
        }
        chapter.setPsalmNumbers(psalmNumbers);
        Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                ": New chapter parsed: " + chapter.toString());
        return chapter;
    }

    private List<String> parsePeople(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.BK.CREATORS,
                Constants.TAG.BK.REVIEWERS,
                Constants.TAG.BK.EDITORS,
                Constants.TAG.BK.AUTHORS
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
        if (Constants.TAG.BK.CREATORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(Constants.TAG.BK.CRTRS.CREATOR);
        } else if (Constants.TAG.BK.REVIEWERS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(Constants.TAG.BK.RVRS.REVIEWER);
        } else if (Constants.TAG.BK.EDITORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(Constants.TAG.BK.EDTRS.EDITOR);
        } else if (Constants.TAG.BK.AUTHORS.equalsIgnoreCase(startTag)) {
            allowedInnerTags.add(Constants.TAG.BK.AUTRS.AUTHOR);
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
                        verifyEndTag(parser, tagName, currentTagName);
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

    private List<Psalm> parsePsalms(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(Constants.TAG.BK.PSLMS.TAG)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": The start Psalms tag is expected, but there is: '" + parser.getName() + "'");
            return null;
        }

        Psalm psalm = null;
        final List<Psalm> psalms = new ArrayList<>();
        final List<String> allowedInnerTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.BK.PSLMS.PSALM,
                Constants.TAG.BK.PSLMS.REF
        ));
        String ref = null;
        String tagName;
        String currentTagName = null;
        boolean done = false;
        parser.next();
        while (!done) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        currentTagName = tagName;
                        if (Constants.TAG.BK.PSLMS.PSALM.equalsIgnoreCase(tagName)) {
                            psalm = parsePsalm(parser);
                            continue;
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalms tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (Constants.TAG.BK.PSLMS.REF.equalsIgnoreCase(currentTagName)) {
                            ref = parser.getText();
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (Constants.TAG.BK.PSLMS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (allowedInnerTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                        if (Constants.TAG.BK.PSLMS.REF.equalsIgnoreCase(tagName)) {
                            try {
                                psalm = parsePsalm(ref);
                            } catch (PwsXmlEngineIncorrectValueException e) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": Incorrect pws psalm filename: '" + ref + "'");
                            } catch (PwsXmlParserFileNotFoundException e) {
                                Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                        ": File not found: '" + ref + "'");
                            }
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
            parser.next();
        }
        if (psalms.size() == 0) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Cannot parse no one psalm.");
        }
        Log.v(LOG_TAG, "Line " + parser.getLineNumber() +
                ": " + psalms.size() + " psalms have been parsed in the current section.");
        return psalms;
    }

    private Psalm parsePsalm(XmlPullParser parser) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
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
                TAG.PSLM.ANNOTATION,
                TAG.PSLM.COMPOSER,
                TAG.PSLM.TEXT,
                TAG.PSLM.TONALITIES));
        final List<String> tagsWithoutText = new ArrayList<>(Arrays.asList(
                TAG.PSLM.TONALITIES,
                TAG.PSLM.NUMBERS,
                TAG.PSLM.TEXT));
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
                            parsePsalmNumbers(parser, psalm);
                        } else if (TAG.PSLM.TONALITIES.equalsIgnoreCase(tagName)) {
                            parsePsalmTonalities(parser, psalm);
                        }else if (TAG.PSLM.TEXT.equalsIgnoreCase(tagName)) {
                            parsePsalmText(parser, psalm);
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm tag: " + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName == null) {
                        if (!parser.isWhitespace()) {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                        break;
                    }
                    if (!parser.isWhitespace()) {
                        if (currentTagName.equals(TAG.PSLM.AUTHOR)) {
                            psalm.setAuthor(parser.getText());
                        } else if (currentTagName.equals(TAG.PSLM.TRANSLATOR)) {
                            psalm.setTranslator(parser.getText());
                        } else if (TAG.PSLM.COMPOSER.equalsIgnoreCase(currentTagName)) {
                            psalm.setComposer(parser.getText());
                        } else if (TAG.PSLM.YEAR.equalsIgnoreCase(currentTagName)) {
                            final String date = parser.getText();
                            validateDateFormat(parser, date);
                            psalm.setYear(date);
                        } else if (TAG.PSLM.ANNOTATION.equalsIgnoreCase(currentTagName)) {
                            psalm.setAnnotation(parser.getText());
                        } else if (TAG.PSLM.NAME.equalsIgnoreCase(currentTagName)) {
                            psalm.setName(parser.getText());
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    } else if (!tagsWithoutText.contains(currentTagName)) {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Text required but not found for tag '" + currentTagName + "'");
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(Constants.TAG.PSLM.TAG)) {
                        done = true;
                    } else if (allowedStartTags.contains(tagName.toLowerCase())) {
                        verifyEndTag(parser, tagName, currentTagName);
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

    private void parsePsalmNumbers(XmlPullParser parser, Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(Constants.TAG.PSLM.NUMS.TAG)) return;

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
                    if (Constants.TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        bookEdition = null;
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attributeName = parser.getAttributeName(i);
                            if (Constants.TAG.PSLM.EDITION.equalsIgnoreCase(attributeName)) {
                                bookEdition = parseBookEdition(parser, parser.getAttributeValue(i));
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
                        if (Constants.TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(currentTagName) && bookEdition != null) {
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
                    if (Constants.TAG.PSLM.NUMS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (Constants.TAG.PSLM.NUMS.NUMBER.equalsIgnoreCase(tagName)) {
                        verifyEndTag(parser, tagName, currentTagName);
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

    private BookEdition parseBookEdition(XmlPullParser parser, String s) {
        BookEdition bookEdition = BookEdition.getInstanceBySignature(s.trim());
        if (bookEdition == null) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Unknown Book Edition: '" + s + "'");
        }
        return bookEdition;
    }

    private void parsePsalmTonalities(XmlPullParser parser, Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(Constants.TAG.PSLM.TONS.TAG)) return;

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
                    if (Constants.TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Tonalities tag: '" + tagName + "'");
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && Constants.TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(currentTagName)) {
                        tonality = parser.getText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (Constants.TAG.PSLM.TONS.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (Constants.TAG.PSLM.TONS.TONALITY.equalsIgnoreCase(tagName)) {
                        verifyEndTag(parser, tagName, currentTagName);
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

    private void parsePsalmText(XmlPullParser parser, Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (psalm == null) {
            throw new IllegalArgumentException();
        }
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !Constants.TAG.PSLM.TXT.TAG.equalsIgnoreCase(parser.getName())) return;

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
                    if (Constants.TAG.PSLM.TXT.VERSE.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        psalmVerse = new PsalmVerse();
                        psalmVerse.setNumbers(parseNumbersAttribute(parser, Constants.TAG.PSLM.TXT.VERSE));
                    } else  if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        psalmChorus = new PsalmChorus();
                        psalmChorus.setNumbers(parseNumbersAttribute(parser, Constants.TAG.PSLM.TXT.VERSE));
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Text tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null) {
                        if (Constants.TAG.PSLM.TXT.VERSE.equalsIgnoreCase(currentTagName)) {
                            String line = parser.getText();
                            if (line != null && !line.isEmpty()) {
                                String text = psalmVerse.getText();
                                if (text == null) text = "";
                                text += line.replaceAll("\n\\s*+", "\n").trim();
                                psalmVerse.setText(text);
                            }
                        } else if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(currentTagName)) {
                            String line = parser.getText();
                            if (line != null && !line.isEmpty()) {
                                String text = psalmChorus.getText();
                                if (text == null) text = "";
                                text += line.replaceAll("\n\\s*+", "\n").trim();
                                psalmChorus.setText(text);
                            }
                        } else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected Text: '" + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (Constants.TAG.PSLM.TXT.TAG.equalsIgnoreCase(tagName)) {
                        done = true;
                    } else if (Constants.TAG.PSLM.TXT.VERSE.equalsIgnoreCase(tagName)) {
                        verifyEndTag(parser, tagName, currentTagName);
                        currentTagName = null;
                        if (psalmVerse != null) {
                            psalmVerses.add(psalmVerse);
                            psalmVerse = null;
                        }
                    } else if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
                        verifyEndTag(parser, tagName, currentTagName);
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

    private List<Integer> parseNumbersAttribute(XmlPullParser parser, String tagName) throws XmlPullParserException {
        final List<String> allowedTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.PSLM.TXT.CHORUS,
                Constants.TAG.PSLM.TXT.VERSE ));
        final List<String> allowedAttributeNames = new ArrayList<>(Arrays.asList(
                Constants.TAG.PSLM.TXT.CRS.NUMBER,
                Constants.TAG.PSLM.TXT.VRS.NUMBER ));
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

    private void verifyEndTag(XmlPullParser parser, String endTagName, String currentTagName) throws PwsXmlParserIncorrectSourceFormatException {
        if (endTagName == null || currentTagName == null ||
                !endTagName.equalsIgnoreCase(currentTagName)) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Incorrect end tag: " + endTagName +
                    "(expected tag: '" + currentTagName + "')");
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
    }

    private void validateDateFormat(XmlPullParser parser, String text) {
        Date date = parseDate(text);
        if (date == null) {
            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                    ": Cannot parse date. Incorrect format '" + text + "')");
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

    private List<Integer> parseNumbersFromText(String text) {
        List<Integer> numbers = new ArrayList<>();
        for (String s : text.split(NUMBERS_SPLIT_REGEX)) {
            try {
                numbers.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ex) {
            }
        }
        return numbers;
    }
}
