package com.alelk.pws.database.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
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

    public PwsDataSourceImpl(Context context, String databaseName, int version) {
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
        }
        return bookEntity;
    }

    @Override
    public PsalmEntity addPsalm(Psalm psalm) {
        PsalmEntity psalmEntity = null;
        // todo handle exception
        try {
            psalmEntity = new PwsDatabasePsalmQuery(database).insert(psalm);
        } catch (PwsDatabaseSourceIdExistsException pwsDatabaseSourceIdExists) {
            pwsDatabaseSourceIdExists.printStackTrace();
        }
        return psalmEntity;
    }

}
