package com.alelk.pws.database.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.query.PwsDatabaseBookQuery;
import com.alelk.pws.database.query.PwsDatabasePsalmQuery;

/**
 * Created by Alex Elkin on 23.04.2015.
 */
public class PwsDataSourceImpl implements PwsDataSource {
    private static final String LOG_TAG = PwsDataSourceImpl.class.getSimpleName();
    private SQLiteDatabase database;
    private PwsDatabaseHelper databaseHelper;
    private Context context;

    public PwsDataSourceImpl(Context context, String databaseName, int version) {
        this.context = context;
        databaseHelper = new PwsDatabaseHelper(context, databaseName, version);
    }

    @Override
    public void open() {
        database = databaseHelper.getWritableDatabase();
    }

    @Override
    public void close() {
        databaseHelper.close();
    }

    @Override
    public BookEntity addBook(Book book) {
        BookEntity bookEntity = null;
        // todo handle exception
        try {
            bookEntity = new PwsDatabaseBookQuery(database).insert(book);
        } catch (PwsDatabaseSourceIdExistsException pwsDatabaseSourceIdExists) {
            pwsDatabaseSourceIdExists.printStackTrace();
        } catch (PwsDatabaseIncorrectValueException e) {
            e.printStackTrace();
        }
        return bookEntity;
    }

    @Override
    public PsalmEntity addPsalm(Psalm psalm) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "addPsalm";
        PsalmEntity psalmEntity;
        try {
            psalmEntity = new PwsDatabasePsalmQuery(database).insert(psalm);
        } catch (PwsDatabaseSourceIdExistsException e) {
            Log.w(LOG_TAG, METHOD_NAME + ": Could not add psalm '" + psalm.getName() +
                    "'. Duplicate found: " + context.getString(e.getPwsDatabaseMessage().getErrorMessageId()));
            throw e;
        } catch (PwsDatabaseIncorrectValueException e) {
            Log.w(LOG_TAG, METHOD_NAME + ": Could not add psalm '" + psalm.getName() +
                    "'. Incorrect psalm body: " + context.getString(e.getPwsDatabaseMessage().getErrorMessageId()));
            throw e;
        }
        return psalmEntity;
    }

}