package com.alelk.pws.xmlengine;

import android.content.res.AssetManager;
import android.util.Log;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Chapter;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlEngineIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
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
 * Created by alelkin on 25.03.2015.
 */
public class PwsXmlParser extends PwsXmlParserHelper implements Constants {
    private static final String LOG_TAG = "PwsXmlParser";

    private AssetManager assetManager;

    public PwsXmlParser(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public Book parseBook(String filename) throws PwsXmlParserIncorrectSourceFormatException {
        Book book = null;
        if (assetManager == null) {
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        try {
            book = super.parseBook(filename);
        } catch (PwsXmlEngineIncorrectValueException e) {
            e.printStackTrace();
        } catch (PwsXmlParserFileNotFoundException e) {
            e.printStackTrace();
        }

        return book;
    }

    @Override
    protected InputStreamReader openPwsBookFile(String filename) throws PwsXmlParserFileNotFoundException{
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(assetManager.open(filename));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot open file '" + filename + "'");
            throw new PwsXmlParserFileNotFoundException();
        }
        return inputStreamReader;
    }
}
