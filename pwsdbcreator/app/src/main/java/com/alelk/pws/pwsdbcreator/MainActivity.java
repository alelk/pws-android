package com.alelk.pws.pwsdbcreator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.alelk.pws.pwsdb.data.Book;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.pwsdb.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.pwsdb.provider.PwsDataProvider;
import com.alelk.pws.pwsdb.source.PwsDataSource;
import com.alelk.pws.pwsdb.source.PwsDataSourceImpl;
import com.alelk.pws.xmlengine.PwsXmlParser;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String DATABASE_DISPLAY_VERSION = "0.9.1v";

    private final static String mPwsLibFilePath = "content.pwslib";
    private PwsDataSource pwsDataSource;
    private static final String DB_PATH = "/data/data/com.alelk.pws.pwsdbcreator/databases/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager am = this.getAssets();
        PwsXmlParser parser = new PwsXmlParser(am);
        dropDbFile("pws.db");
        pwsDataSource = new PwsDataSourceImpl(getApplicationContext(), "pws.db", PwsDataProvider.DATABASE_VERSION);
        try {
            pwsDataSource.open();
            List<Book> books = parser.parseLibrary(mPwsLibFilePath);
            for (Book book : books) {
                pwsDataSource.addBook(book);
            }
            for (Book book : books) {
                for (Psalm psalm : book.getPsalms().values()) {
                    try {
                        pwsDataSource.addPsalm(psalm);
                    } catch (PwsDatabaseSourceIdExistsException e) {
                    } catch (PwsDatabaseIncorrectValueException e) {
                    }
                }
            }

        } catch (PwsXmlParserIncorrectSourceFormatException e) {
            e.printStackTrace();
        } finally {
            pwsDataSource.close();
        }
        try {
            splitDbFile("pws.db");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void dropDbFile(String databaseName) {
        final String METHOD_NAME = "dropDbFile";
        File file = new File(DB_PATH + databaseName);
        if (!file.exists()) {
            Log.d(LOG_TAG, METHOD_NAME + ": File not found: " + DB_PATH + databaseName);
            return;
        }
        file.delete();
        Log.i(LOG_TAG, METHOD_NAME + ": The file has been deleted: " + DB_PATH + databaseName);
    }

    private void splitDbFile(String databaseName) throws IOException {
        final String METHOD_NAME = "splitDbFile";
        File file = new File(DB_PATH + databaseName);
        if (!file.exists()) {
            Log.w(LOG_TAG, METHOD_NAME + ": File not found: " + DB_PATH + databaseName);
            return;
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        byte[] buffer = new byte[1000000];
        int count;
        try {
            inputStream = new FileInputStream(file.getAbsoluteFile());
            for (int i = 1; (count = inputStream.read(buffer)) != -1; i++) {
                try {
                    outputStream = new FileOutputStream(DB_PATH + databaseName + "." + DATABASE_DISPLAY_VERSION + "." + i);
                    outputStream.write(buffer, 0, count);
                    Log.i(LOG_TAG, METHOD_NAME + ": New file part created: " + DB_PATH + databaseName + "." + i);
                } finally {
                    if (outputStream != null) outputStream.close();
                }
            }
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }
}
