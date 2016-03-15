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
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.database.util.PwsPsalmUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.alelk.pws.database.table.PwsPsalmTable.*;

/**
 * Created by Alex Elkin on 29.04.2015.
 * Edited by Alex Elkin on 14.03.2016: code refactoring - psalm text field has been added.
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
            TABLE_PSALMS + "." + COLUMN_YEAR,
            TABLE_PSALMS + "." + COLUMN_ANNOTATION,
            TABLE_PSALMS + "." + COLUMN_TEXT};

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

    private static final String SELECTION_BY_NAME_AND_BOOK_EDITION =
            PwsPsalmTable.TABLE_PSALMS + "." + PwsPsalmTable.COLUMN_NAME + " LIKE ? AND " +
            PwsBookTable.TABLE_BOOKS + "." + PwsBookTable.COLUMN_EDITION + "=?";

    private SQLiteDatabase mDatabase;
    private Cursor mCursor;

    public PwsDatabasePsalmQuery(SQLiteDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }

    /**
     * Insert PWS Psalm object to mDatabase
     * @param psalm PWS Psalm object
     * @return PsalmEntity inserted into mDatabase
     * @throws PwsDatabaseIncorrectValueException if any value incorrect. Contains one of the following PwsDatabaseMessage:
     * NULL_DATABASE_VALUE if mDatabase is null,
     * NULL_PSALM_VALUE if psalm object is null,
     * NO_PSALM_NUMBERS if the psalm does not contains no one number,
     * NO_BOOK_EDITION_FOUND if book edition is specified, but no book found with this book edition,
     * UNEXPECTED_BOOK_EDITION_VALUE if if book edition is specified, but the psalm param has no psalm number for this book edition,
     * NO_PSALM_PART_NUMBERS if psalm chorus or psalm verse does not contain no one number,
     * @throws PwsDatabaseSourceIdExistsException if source already exists, but it's version is different from current. Contains one of the following PwsDatabaseMessage:
     * PSALM_ID_EXISTS if psalm object already exists in mDatabase with another version
     */
    @Override
    public PsalmEntity insert(Psalm psalm) throws PwsDatabaseIncorrectValueException, PwsDatabaseSourceIdExistsException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validatePsalmNotNull(METHOD_NAME, psalm);
        PsalmEntity psalmEntity;
        mDatabase.beginTransaction();
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
                long id = mDatabase.insert(TABLE_PSALMS, null, contentValues);
                insertPsalmNumbers(psalm, id);

                psalmEntity = selectById(id);

                mDatabase.setTransactionSuccessful();
                Log.v(LOG_TAG, METHOD_NAME + ": New psalm added: " + psalmEntity);
            }
        } catch (PwsDatabaseException e) {
            Log.v(LOG_TAG, METHOD_NAME + ": No psalm added. The exception was thrown: " + e.getPwsDatabaseMessage());
            throw e;
        } finally {
            mDatabase.endTransaction();
        }
        return psalmEntity;
    }

    @Override
    public PsalmEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        PsalmEntity psalmEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_PSALMS, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                psalmEntity = cursorToPsalmEntity(mCursor);
                Log.v(LOG_TAG, METHOD_NAME + ": Psalm selected: " + psalmEntity);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return  psalmEntity;
    }

    public PsalmEntity selectByPsalmNumberId(long psalmNumberId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByPsalmNumberId";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        PsalmEntity psalmEntity = null;
        try {
            PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(mDatabase, null, null).selectById(psalmNumberId);
            if (psalmNumberEntity != null) {
                psalmEntity = selectById(psalmNumberEntity.getPsalmId());
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        if (psalmEntity != null) {
            Log.v(LOG_TAG, METHOD_NAME + ": New PsalmEntity selected for psalmNumberId='" + psalmNumberId + "': " + psalmEntity);
        } else {
            Log.v(LOG_TAG, METHOD_NAME + ": No PsalmEntity found for psalmNumberId='" + psalmNumberId + "'");
        }
        return  psalmEntity;
    }

    public Set<PsalmEntity> selectByBookEdition(BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookEdition";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<PsalmEntity> psalmEntities = null;
        final String[] SELECTION_ARGS = new String[1];
        Arrays.asList(bookEdition.getSignature()).toArray(SELECTION_ARGS);
        try {
            mCursor = mDatabase.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS, ALL_COLUMNS, SELECTION_BY_BOOK_EDITION, SELECTION_ARGS, null, null, null);
            if (mCursor.moveToFirst()) {
                psalmEntities = new HashSet<>(mCursor.getCount());
                do {
                    psalmEntities.add(cursorToPsalmEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of psalms selected for bookEdition=" + bookEdition + ": " + psalmEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalms selected for bookEdition=" + bookEdition);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return psalmEntities;
    }

    public Set<PsalmEntity> selectByNameAndBookEdition(String name, BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNameAndBookEdition";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<PsalmEntity> psalmEntities = null;
        final String[] SELECTION_ARGS = new String[2];
        Arrays.asList(name, bookEdition.getSignature()).toArray(SELECTION_ARGS);
        try {
            mCursor = mDatabase.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS, ALL_COLUMNS, SELECTION_BY_NAME_AND_BOOK_EDITION, SELECTION_ARGS, null, null, null);
            if (mCursor.moveToFirst()) {
                psalmEntities = new HashSet<>(mCursor.getCount());
                do {
                    psalmEntities.add(cursorToPsalmEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of psalms selected for name='" + name + "' and bookEdition=" + bookEdition + ": " + psalmEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No psalms selected for name='" + name + "' and bookEdition=" + bookEdition);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return psalmEntities;
    }

    // todo refactor this function
    public Set<PsalmEntity> selectByBookId(long bookId) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByBookId";

        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<PsalmNumberEntity> psalmNumberEntities =
                new PwsDatabasePsalmNumberQuery(mDatabase, null, null).selectByBookId(bookId);
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
            Log.v(LOG_TAG, METHOD_NAME + ": No psalms selected for bookId=" + bookId);
        }
        return  psalmEntities;
    }

    public PsalmEntity selectByNumbers(Psalm psalm) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectByNumbers";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        validatePsalmNotNull(METHOD_NAME, psalm);
        PsalmEntity psalmEntity = null;
        validatePsalmNumbersNotEmpty(METHOD_NAME, psalm.getNumbers());
        Long psalmId = null;
        for (BookEdition bookEdition : psalm.getBookEditions()) {
            long psalmNumber = psalm.getNumber(bookEdition);
            // todo: check if book edition selected is not null
            long bookId = new PwsDatabaseBookQuery(mDatabase).selectByEdition(bookEdition).getId();
            PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(mDatabase, null, null).selectByNumberAndBookId(psalmNumber, bookId);
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
            new PwsDatabasePsalmNumberQuery(mDatabase, psalmId, bookEdition).insert(psalm);
        }
    }

    // TODO: 14.03.2016 remove this method
    @Deprecated
    private void insertPsalmParts(Psalm psalm, Long psalmId) throws PwsDatabaseSourceIdExistsException, PwsDatabaseIncorrectValueException {
        for (PsalmPart psalmPart : psalm.getPsalmPartsValues()) {
            if (psalmPart.getPsalmType() == PsalmPartType.CHORUS) {
                new PwsDatabaseChorusQuery(mDatabase, psalmId).insert((PsalmChorus) psalmPart);
            } else if (psalmPart.getPsalmType() == PsalmPartType.VERSE) {
                new PwsDatabaseVerseQuery(mDatabase, psalmId).insert((PsalmVerse) psalmPart);
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
        psalmEntity.setAnnotation(cursor.getString(8));
        psalmEntity.setText(cursor.getString(9));
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
        if (!TextUtils.isEmpty(psalm.getYear())) {
            values.put(COLUMN_YEAR, psalm.getYear().toString());
        }
        if (!TextUtils.isEmpty(psalm.getAnnotation())) {
            values.put(COLUMN_ANNOTATION, psalm.getAnnotation().toString());
        }
        String text = PwsPsalmUtil.convertPsalmPartsToPlainText(psalm.getPsalmParts());
        if (!TextUtils.isEmpty(text)) {
            values.put(COLUMN_TEXT, text);
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }
}
