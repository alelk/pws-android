package com.alelk.pws.pwsdb.source;

import com.alelk.pws.pwsdb.data.Book;
import com.alelk.pws.pwsdb.data.BookEdition;
import com.alelk.pws.pwsdb.data.BookInfo;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.data.entity.BookEntity;
import com.alelk.pws.pwsdb.data.entity.PsalmEntity;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.pwsdb.exception.PwsDatabaseSourceIdExistsException;

import java.util.Map;

/**
 * Created by alelkin on 23.04.2015.
 */
public interface PwsDataSource {
    void open();
    void close();
    BookEntity addBook(Book book);
    BookInfo getBookInfo(Long id);
    BookInfo getBookInfo(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException;
    BookInfo getBookInfoByPsalmNumberId(Long id);
    PsalmEntity addPsalm(Psalm psalm) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException;
    Map<Integer, Psalm> getPsalms(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException;
    Map<Integer, Psalm> getPsalms(BookEdition bookEdition, String name) throws PwsDatabaseIncorrectValueException;
    Psalm getPsalm(Long id) throws PwsDatabaseIncorrectValueException;
}
