package com.alelk.pws.database.source;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import static com.alelk.pws.database.helper.PwsDatabaseBookHelper.*;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.exception.PwsDatabaseMessage;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExists;
import com.alelk.pws.database.helper.PwsDatabaseBookHelper;

/**
 * Created by alelkin on 23.04.2015.
 */
public class PwsBookDataSource implements PwsDataSource {
    private static final String LOG_TAG = PwsBookDataSource.class.getSimpleName();
    private SQLiteDatabase database;
    private PwsDatabaseBookHelper bookHelper;

    public final static String MULTIVALUE_DELIMITER = "; ";

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

    public PwsBookDataSource(Context context) {
        bookHelper = new PwsDatabaseBookHelper(context);
    }

    public void open() {
        database = bookHelper.getWritableDatabase();
    }

    public void close() {
        bookHelper.close();
    }

    /**
     * Inserts book into Pws database.
     * @param book The book to insert
     * @return BookEntity inserted into database.
     * @throws PwsDatabaseSourceIdExists if book id already exists and it's version matches the
     * version of the specified book
     */
    public BookEntity insertBook(Book book) throws PwsDatabaseSourceIdExists {
        final String METHOD_NAME = "insertBook";
        BookEntity bookEntity = null;
        bookEntity = selectBookEntityByEdition(book.getEdition());
        if (bookEntity != null) {
            Log.d(LOG_TAG, METHOD_NAME + ": Book already exists: " + bookEntity);
            if (!isVersionMatches(bookEntity, book)) {
                throw new PwsDatabaseSourceIdExists(PwsDatabaseMessage.BOOK_ID_EXISTS, bookEntity.getId());
            }
        } else {
            ContentValues contentValues = new ContentValues();
            fillValues(contentValues, book);
            long id = database.insert(TABLE_BOOKS, null, contentValues);
            bookEntity = selectBookEntityById(id);
            Log.d(LOG_TAG, METHOD_NAME + ": New book added: " + bookEntity);
        }
        return bookEntity;
    }

    /**
     * Returns the BookEntity from Pws database selected by book id
     * @param id the id of book to select
     * @return BookEntity with specified id, null if no book found
     */
    public BookEntity selectBookEntityById(long id) {
        final String METHOD_NAME = "selectBookEntityById";
        BookEntity bookEntity = null;
        Cursor cursor = database.query(TABLE_BOOKS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            bookEntity = cursorToBookEntity(cursor);
        }
        Log.d(LOG_TAG, METHOD_NAME + ": Book selected (id = '" + id + "'): " + bookEntity);
        return bookEntity;
    }

    public BookEntity selectBookEntityByEdition(BookEdition edition) {
        final String METHOD_NAME = "selectBookEntityByEdition";
        BookEntity bookEntity = null;
        Cursor cursor = database.query(TABLE_BOOKS, ALL_COLUMNS, COLUMN_EDITION + " = '" + edition.getSignature() + "'", null, null, null, "1");
        if (cursor.moveToFirst()) {
            bookEntity = cursorToBookEntity(cursor);
        }
        Log.d(LOG_TAG, METHOD_NAME + ": Book selected (edition = '" + edition.getSignature()
                + "'): " + bookEntity);
        return bookEntity;
    }

    private void fillValues(ContentValues values, Book book) {
        if (!book.getVersion().isEmpty()) {
            values.put(COLUMN_VERSION, book.getVersion());
        }
        if (!book.getName().isEmpty()) {
            values.put(COLUMN_NAME, book.getName());
        }
        if (!book.getShortName().isEmpty()) {
            values.put(COLUMN_SHORTNAME, book.getShortName());
        }
        if (!book.getDisplayName().isEmpty()) {
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
        if (!book.getDescription().isEmpty()) {
            values.put(COLUMN_DESCRIPTION, book.getDescription());
        }
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

    private boolean isVersionMatches(BookEntity bookEntity, Book book) {
        if (book.getVersion().equalsIgnoreCase(bookEntity.getVersion())) {
            return true;
        }
        return false;
    }
}
