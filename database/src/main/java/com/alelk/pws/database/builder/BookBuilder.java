package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.query.PwsDatabaseQuery;

import java.util.Arrays;

/**
 * Created by Alex Elkin on 30.01.2016.
 */
public class BookBuilder implements PwsBuilder<Book, BookEntity> {

    private BookEntity mBookEntity;

    public BookBuilder() {}

    public BookBuilder(BookEntity bookEntity) {
        mBookEntity = bookEntity;
    }

    @Override
    public PwsBuilder<Book, BookEntity> appendEntity(BookEntity entity) {
        mBookEntity = entity;
        return this;
    }

    @Override
    public Book toObject() {
        Book book = null;
        book = (Book) new BookInfoBuilder(mBookEntity).toObject();
        return book;
    }
}
