package com.alelk.pws.xmlengine;

import android.content.res.AssetManager;
import android.util.Log;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.xmlengine.exception.PwsXmlEngineIncorrectValueException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserFileNotFoundException;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Alex Elkin on 25.03.2015.
 */
public class PwsXmlParser extends PwsXmlParserHelper implements Constants {
    private static final String LOG_TAG = "PwsXmlParser";

    private AssetManager assetManager;

    private String mBookPath = "";
    private String mLibraryPath = "";

    public PwsXmlParser(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public List<Book> parseLibrary(String filename) throws PwsXmlParserIncorrectSourceFormatException {
        if (assetManager == null) {
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        if (filename == null) {
            // TODO: 03.02.2016 handle this case 
            return null;
        }
        if (filename.contains("/")) mLibraryPath = filename.substring(0, filename.lastIndexOf("/"));
        else mLibraryPath = "";
        List<Book> books = null;
        try {
            books = super.parseLibrary(filename);
        } catch (PwsXmlEngineIncorrectValueException e) {
            e.printStackTrace();
        } catch (PwsXmlParserFileNotFoundException e) {
            e.printStackTrace();
        }
        return books;
    }

    public String parseLibraryVersion(String filename) throws PwsXmlParserIncorrectSourceFormatException {
        if (assetManager == null) {
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        if (filename == null) {
            // TODO: 03.02.2016 handle this case
            return null;
        }
        String version = null;
        try {
            version = super.parseLibraryVersion(filename);
        } catch (PwsXmlEngineIncorrectValueException e) {
            e.printStackTrace();
        } catch (PwsXmlParserFileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return version;
    }

    public Book parseBook(String filename) throws PwsXmlParserIncorrectSourceFormatException {
        Book book = null;
        if (assetManager == null) {
            throw new PwsXmlParserIncorrectSourceFormatException();
        }
        if (filename == null) {
            // TODO: 02.02.2016 handle this case 
            return null;
        }
        mBookPath = filename.substring(0, filename.lastIndexOf("/"));
        try {
            book = super.parseBook(filename);
        } catch (PwsXmlEngineIncorrectValueException e) {
            // TODO: 31.01.2016 throw exception
            e.printStackTrace();
        } catch (PwsXmlParserFileNotFoundException e) {
            e.printStackTrace();
        }

        return book;
    }

    @Override
    protected InputStreamReader openPwsLibraryFile(String filename) throws PwsXmlParserFileNotFoundException {
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(assetManager.open(filename));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot open file '" + filename + "'");
            throw new PwsXmlParserFileNotFoundException();
        }
        return inputStreamReader;
    }

    @Override
    protected InputStreamReader openPwsBookFile(String filename) throws PwsXmlParserFileNotFoundException{
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(assetManager.open(filename));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot open file '" + filename + "'");
            throw new PwsXmlParserFileNotFoundException();
        }
        return inputStreamReader;
    }

    @Override
    protected InputStreamReader openPwsPsalmFile(String filename) throws PwsXmlParserFileNotFoundException {
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(assetManager.open(mBookPath + "/" + filename));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot open file '" + mBookPath + "/" + filename + "'");
            throw new PwsXmlParserFileNotFoundException();
        }
        return inputStreamReader;
    }
}
