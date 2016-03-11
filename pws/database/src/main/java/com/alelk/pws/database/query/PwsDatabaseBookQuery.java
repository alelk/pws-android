package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.table.PwsBookTable.*;
import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseMessage;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

/**
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabaseBookQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<Book, BookEntity> {

    private static final String LOG_TAG = PwsDatabaseBookQuery.class.getSimpleName();

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_VERSION,
            COLUMN_NAME,
            COLUMN_SHORTNAME,
            COLUMN_DISPLAYNAME,
            COLUMN_EDITION,
            COLUMN_RELEASEDATE,
            COLUMN_AUTHORS,
            COLUMN_CREATORS,
            COLUMN_REVIEWERS,
            COLUMN_EDITORS,
            COLUMN_DESCRIPTION };

    private SQLiteDatabase mDatabase;
    private Cursor mCursor;

    public PwsDatabaseBookQuery(SQLiteDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }

    @Override
    public BookEntity insert(Book book) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        BookEntity bookEntity = selectByEdition(book.getEdition());
        if (bookEntity != null) {
            Log.d(LOG_TAG, METHOD_NAME + ": Book already exists: " + bookEntity);
            if (!isVersionMatches(bookEntity.getVersion(), book.getVersion())) {
                throw new PwsDatabaseSourceIdExistsException(PwsDatabaseMessage.BOOK_ID_EXISTS, bookEntity.getId());
            }
            // TODO: 20.02.2016 update when version is up
        } else {
            final ContentValues contentValues = new ContentValues();
            fillContentValues(contentValues, book);
            long id = mDatabase.insert(TABLE_BOOKS, null, contentValues);
            bookEntity = selectById(id);
            Log.d(LOG_TAG, METHOD_NAME + ": New book added: " + bookEntity);
        }
        return bookEntity;
    }

    @Override
    public BookEntity selectById(long id) {
        final String METHOD_NAME = "selectById";
        BookEntity bookEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_BOOKS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                bookEntity = cursorToBookEntity(mCursor);
            }
            Log.d(LOG_TAG, METHOD_NAME + ": Book selected (id = '" + id + "'): " + bookEntity);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return bookEntity;
    }

    /**
     * Select BookEntity from Pws mDatabase by book edition
     * @param edition book edition to select from mDatabase
     * @return BookEntity with specified book edition. Returns null if book edition not found.
     * @throws PwsDatabaseIncorrectValueException if any value is incorrect
     */
    public BookEntity selectByEdition(BookEdition edition) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByEdition";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validateBookEditionNotNull(METHOD_NAME, edition);
        BookEntity bookEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_BOOKS, ALL_COLUMNS, COLUMN_EDITION + " = '" + edition.getSignature() + "'", null, null, null, "1");
            if (mCursor.moveToFirst()) {
                bookEntity = cursorToBookEntity(mCursor);
            }
            Log.d(LOG_TAG, METHOD_NAME + ": Book selected (edition = '" + edition.getSignature()
                    + "'): " + bookEntity);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return bookEntity;
    }

    private BookEntity cursorToBookEntity(Cursor cursor) {
        BookEntity bookEntity = new BookEntity();
        bookEntity.setId(cursor.getLong(0));
        bookEntity.setVersion(cursor.getString(1));
        bookEntity.setName(cursor.getString(2));
        bookEntity.setShortName(cursor.getString(3));
        bookEntity.setDisplayName(cursor.getString(4));
        bookEntity.setEdition(cursor.getString(5));
        bookEntity.setReleaseDate(cursor.getString(6));
        bookEntity.setAuthors(cursor.getString(7));
        bookEntity.setCreators(cursor.getString(8));
        bookEntity.setReviewers(cursor.getString(9));
        bookEntity.setEditors(cursor.getString(10));
        bookEntity.setDescription(cursor.getString(11));
        return bookEntity;
    }

    private void fillContentValues(ContentValues values, Book book) {
        if (!TextUtils.isEmpty(book.getVersion())) {
            values.put(COLUMN_VERSION, book.getVersion());
        }
        if (!TextUtils.isEmpty(book.getName())) {
            values.put(COLUMN_NAME, book.getName());
        }
        if (!TextUtils.isEmpty(book.getShortName())) {
            values.put(COLUMN_SHORTNAME, book.getShortName());
        }
        if (!TextUtils.isEmpty(book.getDisplayName())) {
            values.put(COLUMN_DISPLAYNAME, book.getDisplayName());
        }
        if (book.getEdition() != null) {
            values.put(COLUMN_EDITION, book.getEdition().getSignature());
        }
        if (book.getReleaseDate() != null) {
            values.put(COLUMN_RELEASEDATE, book.getReleaseDate().toString());
        }
        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            values.put(COLUMN_AUTHORS, TextUtils.join(MULTIVALUE_DELIMITER, book.getAuthors()));
        }
        if (book.getCreators() != null && !book.getCreators().isEmpty()) {
            values.put(COLUMN_CREATORS, TextUtils.join(MULTIVALUE_DELIMITER, book.getCreators()));
        }
        if (book.getReviewers() != null && !book.getReviewers().isEmpty()) {
            values.put(COLUMN_REVIEWERS, TextUtils.join(MULTIVALUE_DELIMITER, book.getReviewers()));
        }
        if (book.getEditors() != null && !book.getEditors().isEmpty()) {
            values.put(COLUMN_EDITORS, TextUtils.join(MULTIVALUE_DELIMITER, book.getEditors()));
        }
        if (!TextUtils.isEmpty(book.getDescription())) {
            values.put(COLUMN_DESCRIPTION, book.getDescription());
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
