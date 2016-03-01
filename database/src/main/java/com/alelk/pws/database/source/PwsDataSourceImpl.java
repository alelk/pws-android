package com.alelk.pws.database.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.util.SortedList;
import android.util.Log;

import com.alelk.pws.database.builder.BookBuilder;
import com.alelk.pws.database.builder.FavoriteBuilder;
import com.alelk.pws.database.builder.PsalmBuilder;
import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.FavoritePsalm;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.BookEntity;
import com.alelk.pws.database.data.entity.ChorusEntity;
import com.alelk.pws.database.data.entity.FavoriteEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.PsalmNumberEntity;
import com.alelk.pws.database.data.entity.VerseEntity;
import com.alelk.pws.database.exception.PwsDatabaseException;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.query.PwsDatabaseBookQuery;
import com.alelk.pws.database.query.PwsDatabaseChorusQuery;
import com.alelk.pws.database.query.PwsDatabaseFavoriteQuery;
import com.alelk.pws.database.query.PwsDatabasePsalmNumberQuery;
import com.alelk.pws.database.query.PwsDatabasePsalmQuery;
import com.alelk.pws.database.query.PwsDatabaseQueryHelper;
import com.alelk.pws.database.query.PwsDatabaseVerseQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

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
    public Book getBookInfo(Long id) {
        Book book = null;
        BookEntity bookEntity = new PwsDatabaseBookQuery(database).selectById(id);
        if (bookEntity != null) {
            book = new BookBuilder(bookEntity).toObject();
        }
        return book;
    }

    @Override
    public Book getBookInfo(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        Book book = null;
        BookEntity bookEntity = new PwsDatabaseBookQuery(database).selectByEdition(bookEdition);
        if (bookEntity != null) {
            book = new BookBuilder(bookEntity).toObject();
        }
        return book;
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

    @Override
    public Map<Integer, Psalm> getPsalms(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        Map<Integer, Psalm> psalms = null;
        Set<PsalmEntity> psalmEntities = new PwsDatabasePsalmQuery(database).selectByBookEdition(bookEdition);

        // todo select psalm numbers

        if(psalmEntities != null && !psalmEntities.isEmpty()) {
            psalms = new HashMap<>();
            for (PsalmEntity psalmEntity : psalmEntities) {
                Set<VerseEntity> verseEntities = new PwsDatabaseVerseQuery(database, null).selectByPsalmId(psalmEntity.getId());
                Set<ChorusEntity> chorusEntities = new PwsDatabaseChorusQuery(database, null).selectByPsalmId(psalmEntity.getId());
                Map<BookEdition, Integer> numbers = new PwsDatabaseQueryHelper(database).getPsalmNumbersByPsalmId(psalmEntity.getId());
                Psalm psalm = new PsalmBuilder(psalmEntity, verseEntities, chorusEntities, numbers).toObject();
                if (psalm != null) {
                    psalms.put(psalm.getNumber(bookEdition), psalm);
                }
            }
        }
        return psalms;
    }

    public Map<Integer, Psalm> getPsalms(BookEdition bookEdition, String name) throws PwsDatabaseIncorrectValueException {
        Map<Integer, Psalm> psalms = null;
        Set<PsalmEntity> psalmEntities = new PwsDatabasePsalmQuery(database).selectByNameAndBookEdition(name, bookEdition);
        if(psalmEntities != null && !psalmEntities.isEmpty()) {
            psalms = new HashMap<>();
            for (PsalmEntity psalmEntity : psalmEntities) {
                Set<VerseEntity> verseEntities = new PwsDatabaseVerseQuery(database, null).selectByPsalmId(psalmEntity.getId());
                Set<ChorusEntity> chorusEntities = new PwsDatabaseChorusQuery(database, null).selectByPsalmId(psalmEntity.getId());
                Map<BookEdition, Integer> numbers = new PwsDatabaseQueryHelper(database).getPsalmNumbersByPsalmId(psalmEntity.getId());
                Psalm psalm = new PsalmBuilder(psalmEntity, verseEntities, chorusEntities, numbers).toObject();
                if (psalm != null) {
                    psalms.put(psalm.getNumber(bookEdition), psalm);
                }
            }
        }
        return psalms;
    }

    public Psalm getPsalm(Long id) throws PwsDatabaseIncorrectValueException {
        Psalm psalm = null;
        PsalmEntity psalmEntity = new PwsDatabasePsalmQuery(database).selectById(id);
        if (psalmEntity != null) {
            Set<VerseEntity> verseEntities = new PwsDatabaseVerseQuery(database, null).selectByPsalmId(id);
            Set<ChorusEntity> chorusEntities = new PwsDatabaseChorusQuery(database, null).selectByPsalmId(id);
            Map<BookEdition, Integer> numbers = new PwsDatabaseQueryHelper(database).getPsalmNumbersByPsalmId(id);
            psalm = new PsalmBuilder(psalmEntity, verseEntities, chorusEntities, numbers).toObject();
        }
        return psalm;
    }

    public Psalm getPsalmByPsalmNumberId(Long psalmNumberId) throws PwsDatabaseIncorrectValueException {
        Psalm psalm = null;
        PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(database, null, null).selectById(psalmNumberId);
        long id = psalmNumberEntity.getPsalmId();
        PsalmEntity psalmEntity = new PwsDatabasePsalmQuery(database).selectById(id);
        if (psalmEntity != null) {
            Set<VerseEntity> verseEntities = new PwsDatabaseVerseQuery(database, null).selectByPsalmId(id);
            Set<ChorusEntity> chorusEntities = new PwsDatabaseChorusQuery(database, null).selectByPsalmId(id);
            Map<BookEdition, Integer> numbers = new PwsDatabaseQueryHelper(database).getPsalmNumbersByPsalmId(id);
            psalm = new PsalmBuilder(psalmEntity, verseEntities, chorusEntities, numbers).toObject();
        }
        return psalm;
    }

    public FavoriteEntity addFavorite(FavoritePsalm favoritePsalm) throws PwsDatabaseException {
        final String METHOD_NAME = "addFavorite";
        FavoriteEntity favoriteEntity;
        try {
            favoriteEntity = new PwsDatabaseFavoriteQuery(database).insert(favoritePsalm);
        } catch (PwsDatabaseIncorrectValueException e) {
            Log.w(LOG_TAG, METHOD_NAME + ": Could not add favorite '" + favoritePsalm.getName() +
                    "'. Incorrect favorite body: " + context.getString(e.getPwsDatabaseMessage().getErrorMessageId()));
            throw e;
        }
        return favoriteEntity;
    }

    public SortedList<FavoritePsalm> getFavorites() throws PwsDatabaseIncorrectValueException {
        SortedList favorites = null;
        Set<FavoriteEntity> favoriteEntitySet = new PwsDatabaseFavoriteQuery(database).selectAll();
        for (FavoriteEntity favoriteEntity : favoriteEntitySet) {
            PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(database, null, null).selectById(favoriteEntity.getPsalmNumberId());
            Psalm psalm = getPsalm(psalmNumberEntity.getPsalmId());
        }
        return favorites;
    }

}
