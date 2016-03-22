package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.BookInfo;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.query.PwsDatabaseQuery;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created by Alex Elkin on 02.03.2016.
 */
public class BookInfoBuilder implements PwsBuilder<BookInfo, BookEntity> {

    private BookEntity mBookEntity;

    public BookInfoBuilder() {}

    public BookInfoBuilder(BookEntity bookEntity) {
        mBookEntity = bookEntity;
    }

    @Override
    public PwsBuilder<BookInfo, BookEntity> appendEntity(BookEntity entity) {
        mBookEntity = entity;
        return this;
    }

    @Override
    public BookInfo toObject() {
        BookInfo book = null;
        if (mBookEntity != null) {
            book = new BookInfo();
            book.setName(mBookEntity.getName());
            book.setShortName(mBookEntity.getShortName());
            book.setDisplayName(mBookEntity.getDisplayName());
            book.setDescription(mBookEntity.getDescription());
            book.setVersion(mBookEntity.getVersion());
            book.setEdition(BookEdition.getInstanceBySignature(mBookEntity.getEdition()));
            book.setReleaseDate(mBookEntity.getReleaseDate());
            book.setComment(mBookEntity.getComment());
            book.setPreference(mBookEntity.getPreference());
            book.setLocale(new Locale(mBookEntity.getLocale()));
            if (mBookEntity.getAuthors() != null) {
                book.setAuthors(Arrays.asList(TextUtils.split(mBookEntity.getAuthors(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            }
            if (mBookEntity.getCreators() != null) {
                book.setAuthors(Arrays.asList(TextUtils.split(mBookEntity.getCreators(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            }
            if (mBookEntity.getEditors() != null) {
                book.setAuthors(Arrays.asList(TextUtils.split(mBookEntity.getEditors(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            }
            if (mBookEntity.getReviewers() != null) {
                book.setAuthors(Arrays.asList(TextUtils.split(mBookEntity.getReviewers(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            }

        }
        return book;
    }
}
