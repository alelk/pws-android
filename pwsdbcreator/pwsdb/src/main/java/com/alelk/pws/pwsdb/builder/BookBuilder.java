package com.alelk.pws.pwsdb.builder;

import com.alelk.pws.pwsdb.data.Book;
import com.alelk.pws.pwsdb.data.entity.BookEntity;

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
