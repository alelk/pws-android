package com.alelk.pws.database.source;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

import java.util.Map;

/**
 * Created by alelkin on 23.04.2015.
 */
public interface PwsDataSource {
    void open();
    void close();
    BookEntity addBook(Book book);
    PsalmEntity addPsalm(Psalm psalm) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException;
    Map<Integer, Psalm> getPsalms(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException;
    Map<Integer, Psalm> getPsalms(BookEdition bookEdition, String name) throws PwsDatabaseIncorrectValueException;
}
