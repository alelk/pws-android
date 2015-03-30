package com.alelk.pws.xmlengine;

import android.util.Log;

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
public class PwsXmlParser {
    private static final String LOG_TAG = "PwsXmlParser";
    public static final String[] DATE_FORMATS = {"dd MMM yyyy", "MMM yyyy", "yyyy-mm-dd", "yyyy-mm", "yyyy"};
    public static final Locale DATE_LOCALE = Locale.US;

    public void parse() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        parserInitialize();

        //
        Psalm psalm;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_DOCUMENT) {
                Log.d("parser", "Start document");
            } else if (eventType == XmlPullParser.START_TAG) {

                String tagName = parser.getName();
                Log.d("parser", "StartTag:" + tagName);
                if (tagName.equalsIgnoreCase(Constants.TAG.PSLM.TAG)) {
                    psalm = parsePsalm();
                    if (psalm != null) {
                        Log.i("ParsePsalm: ", psalm.toString());
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

    private Psalm parsePsalm() throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
        if (parser.getEventType() != XmlPullParser.START_TAG ||
                !parser.getName().equalsIgnoreCase(Constants.TAG.PSLM.TAG)) return null;
        Psalm psalm = new Psalm();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            switch (attributeName) {
                case Constants.TAG.PSLM.VERSION:
                    psalm.setVersion(parser.getAttributeValue(i));
                    break;
                case Constants.TAG.PSLM.NAME:
                    psalm.setName(parser.getAttributeValue(i));
                    break;
                case Constants.TAG.PSLM.AUTHOR:
                    psalm.setAuthor(parser.getAttributeValue(i));
                    break;
                default:
                    Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                            ": Unknown Psalm attribute name: " + attributeName);
            }
        }

        int eventType;
        final List<String> allowedStartTags = new ArrayList<>(Arrays.asList(
                Constants.TAG.PSLM.AUTHOR,
                Constants.TAG.PSLM.NAME,
                Constants.TAG.PSLM.TRANSLATOR,
                Constants.TAG.PSLM.NUMBERS,
                Constants.TAG.PSLM.YEAR,
                Constants.TAG.PSLM.COMPOSER,
                Constants.TAG.PSLM.TEXT));
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
                        if (Constants.TAG.PSLM.NUMBERS.equalsIgnoreCase(tagName)) {
                            parsePsalmNumbers(psalm);
                        } else if (Constants.TAG.PSLM.TEXT.equalsIgnoreCase(tagName)) {
                            parsePsalmText(psalm);
                        }
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm tag: " + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null && !parser.isWhitespace()) {
                        if (currentTagName.equals(Constants.TAG.PSLM.AUTHOR)) {
                            psalm.setAuthor(parser.getText());
                        } else if (currentTagName.equals(Constants.TAG.PSLM.TRANSLATOR)) {
                            psalm.setTranslator(parser.getText());
                        } else if (Constants.TAG.PSLM.COMPOSER.equalsIgnoreCase(currentTagName)) {
                            psalm.setComposer(parser.getText());
                        } else if (Constants.TAG.PSLM.YEAR.equalsIgnoreCase(currentTagName)) {
                            psalm.setYear(parseDate(parser.getText()));
                        } else if (Constants.TAG.PSLM.NAME.equalsIgnoreCase(currentTagName)) {
                            psalm.setName(parser.getText());
                        }else {
                            Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                    ": Unexpected text: " + parser.getText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase(Constants.TAG.PSLM.TAG)) {
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
                            if (Constants.TAG.BK.EDITION.equalsIgnoreCase(attributeName)) {
                                String attributeValue = parser.getAttributeValue(i);
                                switch (attributeValue.toLowerCase()){
                                    case Constants.ATTR_VAL.BK.EDITION_GUSLI:
                                        bookEdition = BookEdition.GUSLI;
                                        break;
                                    case Constants.ATTR_VAL.BK.EDITION_PV2000:
                                        bookEdition = BookEdition.PV2000;
                                        break;
                                    default:
                                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                                ": Unknown Book Edition: '" + attributeValue);
                                }
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
                ": Cannot to parse no one psalm numbers.");
    }

    private void parsePsalmText(Psalm psalm) throws XmlPullParserException, IOException, PwsXmlParserIncorrectSourceFormatException, PwsDatabaseIncorrectValueException {
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
                        psalmVerse.setNumbers(parseNumbersAttribute(Constants.TAG.PSLM.TXT.VERSE));
                    } else  if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
                        currentTagName = tagName;
                        psalmChorus = new PsalmChorus();
                        psalmChorus.setNumbers(parseNumbersAttribute(Constants.TAG.PSLM.TXT.VERSE));
                    } else {
                        Log.w(LOG_TAG, "Line " + parser.getLineNumber() +
                                ": Unexpected Psalm Text tag: '" + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTagName != null) {
                        if (Constants.TAG.PSLM.TXT.VERSE.equalsIgnoreCase(currentTagName)) {
                            psalmVerse.setText(psalmVerse.getText() + parser.getText());
                        } else if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(currentTagName)) {
                            psalmChorus.setText(psalmChorus.getText() + parser.getText());
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
                        verifyEndTag(tagName, currentTagName);
                        currentTagName = null;
                        if (psalmVerse != null) {
                            psalmVerses.add(psalmVerse);
                            psalmVerse = null;
                        }
                    } else if (Constants.TAG.PSLM.TXT.CHORUS.equalsIgnoreCase(tagName)) {
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
                psalmParts.put(i, pv);
            }
        }
        for (PsalmChorus pc : psalmChoruses) {
            if (pc == null || pc.getNumbers() == null) continue;
            for (int i : pc.getNumbers()) {
                psalmParts.put(i, pc);
            }
        }
        psalm.setPsalmParts(psalmParts);
    }

    private List<Integer> parseNumbersAttribute(String tagName) throws XmlPullParserException {
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
