package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmPartType;
import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.PsalmNumberEntity;
import com.alelk.pws.database.exception.PwsDatabaseException;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseMessage;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alelk.pws.database.table.PwsPsalmTable.*;

/**
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabasePsalmQuery extends PwsDatabaseQueryUtils implements PwsDatabaseQuery<Psalm, PsalmEntity> {

    private static final String LOG_TAG = PwsDatabasePsalmQuery.class.getSimpleName();

    private final static String[] ALL_COLUMNS = {
            TABLE_PSALMS + "." + COLUMN_ID,
            TABLE_PSALMS + "." + COLUMN_VERSION,
            TABLE_PSALMS + "." + COLUMN_NAME,
            TABLE_PSALMS + "." + COLUMN_AUTHOR,
            TABLE_PSALMS + "." + COLUMN_TRANSLATOR,
            TABLE_PSALMS + "." + COLUMN_COMPOSER,
            TABLE_PSALMS + "." + COLUMN_TONALITIES,
            TABLE_PSALMS + "." + COLUMN_YEAR };

    // psalms INNER JOIN psalmnumbers ON psalms._id=psalmnumbers.psalmid
    // INNER JOIN books ON books._id=psalmnumbers.bookid
    private static final String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMS +
            " INNER JOIN " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS +
            " ON " + TABLE_PSALMS + "." + COLUMN_ID + "=" +
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "." + PwsPsalmNumbersTable.COLUMN_PSALMID +
            " INNER JOIN " + PwsBookTable.TABLE_BOOKS +
            " ON " + PwsBookTable.TABLE_BOOKS + "." + PwsBookTable.COLUMN_ID + "=" +
            PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + "." + PwsPsalmNumbersTable.COLUMN_BOOKID;

    private static final String SELECTION_BY_BOOK_EDITION =
            PwsBookTable.TABLE_BOOKS + "." + PwsBookTable.COLUMN_EDITION + "=?";

    private SQLiteDatabase database;

    public PwsDatabasePsalmQuery(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert PWS Psalm object to database
     * @param psalm PWS Psalm object
     * @return PsalmEntity inserted into database
     * @throws PwsDatabaseIncorrectValueException if any value incorrect. Contains one of the following PwsDatabaseMessage:
     * NULL_DATABASE_VALUE if database is null,
     * NULL_PSALM_VALUE if psalm object is null,
     * NO_PSALM_NUMBERS if the psalm does not contains no one number,
     * NO_BOOK_EDITION_FOUND if book edition is specified, but no book found with this book edition,
     * UNEXPECTED_BOOK_EDITION_VALUE if if book edition is specified, but the psalm param has no psalm number for this book edition,
     * NO_PSALM_PART_NUMBERS if psalm chorus or psalm verse does not contain no one number,
     * @throws PwsDatabaseSourceIdExistsException if source already exists, but it's version is different from current. Contains one of the following PwsDatabaseMessage:
     * PSALM_ID_EXISTS if psalm object already exists in database with another version
     */
    @Override
    public PsalmEntity insert(Psalm psalm) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmNotNull(METHOD_NAME, psalm);
        PsalmEntity psalmEntity;
        database.beginTransaction();
        try {
            psalmEntity = selectByNumbers(psalm);
            if (psalmEntity != null) {
                if (!isVersionMatches(psalm.getVersion(), psalmEntity.getVersion())) {
                    Log.d(LOG_TAG, METHOD_NAME + ": Psalm already exists, but it's version does not match the current version: " + psalmEntity
                            + " (current version: " + psalm.getVersion() + ")");
                    throw new PwsDatabaseSourceIdExistsException(PwsDatabaseMessage.PSALM_ID_EXISTS, psalmEntity.getId());
                }
                Log.v(LOG_TAG, METHOD_NAME + ": psalm already exists: " + psalmEntity);
            } else {
                final ContentValues contentValues = new ContentValues();
                fillContentValues(contentValues, psalm);
                long id = database.insert(TABLE_PSALMS, null, contentValues);
                insertPsalmNumbers(psalm, id);
                insertPsalmParts(psalm, id);

                psalmEntity = selectById(id);

                database.setTransactionSuccessful();
                Log.v(LOG_TAG, METHOD_NAME + ": New psalm added: " + psalmEntity);
            }
        } catch (PwsDatabaseException e) {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm added. The exception was thrown: " + e.getPwsDatabaseMessage());
            throw e;
        } finally {
            database.endTransaction();
        }
        return psalmEntity;
    }

    @Override
    public PsalmEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        PsalmEntity psalmEntity = null;
        Cursor cursor = database.query(TABLE_PSALMS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
        if (cursor.moveToFirst()) {
            psalmEntity = cursorToPsalmEntity(cursor);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm selected: " + psalmEntity);
        }
        return  psalmEntity;
    }

    public Set<PsalmEntity> selectByBookEdition(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookEdition";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        Set<PsalmEntity> psalmEntities = null;
        final String[] SELECTION_ARGS = new String[1];
        Arrays.asList(bookEdition.getSignature()).toArray(SELECTION_ARGS);
        Cursor cursor = database.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS, ALL_COLUMNS, SELECTION_BY_BOOK_EDITION, SELECTION_ARGS, null, null, null);

        return psalmEntities;
    }

    public Set<PsalmEntity> selectByBookId(long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookId";

        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        Set<PsalmNumberEntity> psalmNumberEntities =
                new PwsDatabasePsalmNumberQuery(database, null, null).selectByBookId(bookId);
        Set<PsalmEntity> psalmEntities = null;
        if (psalmNumberEntities != null && !psalmNumberEntities.isEmpty()) {
            psalmEntities = new HashSet<>(psalmNumberEntities.size());
            for (PsalmNumberEntity psalmNumberEntity : psalmNumberEntities) {
                long psalmId = psalmNumberEntity.getPsalmId();
                PsalmEntity psalmEntity = selectById(psalmId);
                psalmEntities.add(psalmEntity);
            }
            Log.v(LOG_TAG, METHOD_NAME + ": Count of psalms selected for bookId=" + bookId + ": " + psalmEntities.size());
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalms selected for bookId=" + bookId + ": ");
        }
        return  psalmEntities;
    }

    public PsalmEntity selectByNumbers(Psalm psalm) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumbers";
        validateSQLiteDatabaseNotNull(METHOD_NAME, database);
        validatePsalmNotNull(METHOD_NAME, psalm);
        PsalmEntity psalmEntity = null;
        validatePsalmNumbersNotEmpty(METHOD_NAME, psalm.getNumbers());
        Long psalmId = null;
        for (BookEdition bookEdition : psalm.getBookEditions()) {
            long psalmNumber = psalm.getNumber(bookEdition);
            long bookId = new PwsDatabaseBookQuery(database).selectByEdition(bookEdition).getId();
            PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(database, null, null).selectByNumberAndBookId(psalmNumber, bookId);
            if (psalmNumberEntity == null) {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalm number entity found with psalmNumber="
                        + psalmNumber + " and bookEdition=" + bookEdition + " (bookId=");
                return null;
            }
            if (psalmId == null) {
                psalmId = psalmNumberEntity.getPsalmId();
            } else if (!psalmId.equals(psalmNumberEntity.getPsalmId())) {
                // todo throw exception
                return null;
            }
        }
        // todo compare all numbers
        if (psalmId != null) {
            psalmEntity = selectById(psalmId);
            Log.v(LOG_TAG, METHOD_NAME + ": Psalm selected" + psalmEntity);
        }
        return psalmEntity;
    }

    private void insertPsalmNumbers(Psalm psalm, Long psalmId) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        for (BookEdition bookEdition : psalm.getNumbers().keySet()) {
            new PwsDatabasePsalmNumberQuery(database, psalmId, bookEdition).insert(psalm);
        }
    }

    private void insertPsalmParts(Psalm psalm, Long psalmId) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        for (PsalmPart psalmPart : psalm.getPsalmPartsValues()) {
            if (psalmPart.getPsalmType() == PsalmPartType.CHORUS) {
                new PwsDatabaseChorusQuery(database, psalmId).insert((PsalmChorus) psalmPart);
            } else if (psalmPart.getPsalmType() == PsalmPartType.VERSE) {
                new PwsDatabaseVerseQuery(database, psalmId).insert((PsalmVerse) psalmPart);
            }
        }
    }

    private PsalmEntity cursorToPsalmEntity(Cursor cursor) {
        PsalmEntity psalmEntity = new PsalmEntity();
        psalmEntity.setId(cursor.getLong(0));
        psalmEntity.setVersion(cursor.getString(1));
        psalmEntity.setName(cursor.getString(2));
        psalmEntity.setAuthor(cursor.getString(3));
        psalmEntity.setTranslator(cursor.getString(4));
        psalmEntity.setComposer(cursor.getString(5));
        psalmEntity.setTonalities(cursor.getString(6));
        psalmEntity.setYear(cursor.getString(7));
        return psalmEntity;
    }

    private void fillContentValues(ContentValues values, Psalm psalm) {
        if (!TextUtils.isEmpty(psalm.getVersion())) {
            values.put(COLUMN_VERSION, psalm.getVersion());
        }
        if (!TextUtils.isEmpty(psalm.getName())) {
            values.put(COLUMN_NAME, psalm.getName());
        }
        if (!TextUtils.isEmpty(psalm.getAuthor())) {
            values.put(COLUMN_AUTHOR, psalm.getAuthor());
        }
        if (!TextUtils.isEmpty(psalm.getTranslator())) {
            values.put(COLUMN_TRANSLATOR, psalm.getTranslator());
        }
        if (!TextUtils.isEmpty(psalm.getComposer())) {
            values.put(COLUMN_COMPOSER, psalm.getComposer());
        }
        if (psalm.getTonalities() != null && !psalm.getTonalities().isEmpty()) {
            values.put(COLUMN_TONALITIES, TextUtils.join(MULTIVALUE_DELIMITER, psalm.getTonalities()));
        }
        if (psalm.getYear() != null) {
            values.put(COLUMN_YEAR, psalm.getYear().toString());
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
