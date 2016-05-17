package com.alelk.pws.pwsdb.query;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.pwsdb.data.BookEdition;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.data.PsalmPart;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.pwsdb.exception.PwsDatabaseMessage;

import java.util.Map;

/**
 * Created by Alex Elkin on 29.04.2015.
 */
public abstract class PwsDatabaseQueryUtils {

    /**
     * Get log tag.
     * @return log tag of child class
     */
    protected abstract String getLogTag();

    /**
     * Test if Pws Object versions are the same
     * @param oldVersion
     * @param newVersion
     * @return true if the oldVersion matches the newVersion. False otherwise.
     */
    protected boolean isVersionMatches(String oldVersion, String newVersion) {
        if (TextUtils.equals(oldVersion, newVersion)) {
            return true;
        }
        return false;
    }

    /**
     * Validate if psalm is not null
     * @param methodName method name
     * @param psalm psalm object
     * @throws PwsDatabaseIncorrectValueException if psalm is null. Contains the following PwsDatabaseMessage: NULL_PSALM_VALUE
     */
    protected void validatePsalmNotNull(String methodName, Psalm psalm) throws PwsDatabaseIncorrectValueException {
        if (psalm == null) {
            Log.d(getLogTag(), methodName + ": Incorrect value: psalm=null");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NULL_PSALM_VALUE);
        }
    }

    /**
     * Validate if book edition is not null
     * @param methodName method name
     * @param bookEdition book edition object
     * @throws PwsDatabaseIncorrectValueException if book edition is null. Contains the following PwsDatabaseMessage: NULL_BOOK_EDITION_VALUE
     */
    protected void validateBookEditionNotNull(String methodName, BookEdition bookEdition) throws PwsDatabaseIncorrectValueException {
        if (bookEdition == null) {
            Log.d(getLogTag(), methodName + ": Incorrect value: bookEdition=null");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NULL_BOOK_EDITION_VALUE);
        }
    }

    /**
     * Validate if database is not null
     * @param methodName method name
     * @param database SQLiteDatabase object
     * @throws PwsDatabaseIncorrectValueException if database is null. Contains the following PwsDatabaseMessage: NULL_DATABASE_VALUE
     */
    protected void validateSQLiteDatabaseNotNull(String methodName, SQLiteDatabase database) throws PwsDatabaseIncorrectValueException {
        if (database == null) {
            Log.d(getLogTag(), methodName + ": Incorrect value: database=null");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NULL_DATABASE_VALUE);
        }
    }

    /**
     * Validate if psalm id is not null
     * @param methodName method name
     * @param psalmId id of psalm entity from database
     * @throws PwsDatabaseIncorrectValueException if psalmId is null. Contains the following PwsDatabaseMessage: NULL_PSALM_ID_VALUE
     */
    protected void validatePsalmIdNotNull(String methodName, Long psalmId) throws PwsDatabaseIncorrectValueException {
        if (psalmId == null) {
            Log.d(getLogTag(), methodName + ": Incorrect value: psalmId=null");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NULL_PSALM_ID_VALUE);
        }
    }

    /**
     * Validate if psalmNumbers property contains as least one number
     * @param methodName method name
     * @param psalmNumbers psalm numbers of psalm
     * @throws PwsDatabaseIncorrectValueException if psalmNumbers is null or empty. Contains the following PwsDatabaseMessage: NO_PSALM_NUMBERS
     */
    protected void validatePsalmNumbersNotEmpty(String methodName, Map<BookEdition, Integer> psalmNumbers) throws PwsDatabaseIncorrectValueException {
        if (psalmNumbers == null || psalmNumbers.isEmpty()) {
            Log.d(getLogTag(), methodName + ": Incorrect value: psalmNumbers is null or empty");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NO_PSALM_NUMBERS);
        }
    }

    /**
     * Validate if psalm part numbers contains as least one number
     * @param methodName method name
     * @param psalmPart psalm part: psalm verse or psalm chorus
     * @throws PwsDatabaseIncorrectValueException if psalm part numbers is null or empty. Contains the following PwsDatabaseMessage: NO_PSALM_PART_NUMBERS
     */
    protected void validatePsalmPartNumbersNotEmpty(String methodName, PsalmPart psalmPart) throws PwsDatabaseIncorrectValueException {
        if (psalmPart.getNumbers() == null || psalmPart.getNumbers().isEmpty()) {
            Log.d(getLogTag(), methodName + ": Incorrect value: psalm part numbers is null or empty");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NO_PSALM_PART_NUMBERS);
        }
    }

    /**
     * Validate if context non null
     * @param methodName method name
     * @param context context
     * @throws PwsDatabaseIncorrectValueException if context is null or empty. Contains the following PwsDatabaseMessage: NULL_CONTEXT_VALUE
     */
    protected void validateContextNotNull(String methodName, Context context) throws PwsDatabaseIncorrectValueException {
        if (context == null) {
            Log.d(getLogTag(), methodName + ": Incorrect value: context is null");
            throw new PwsDatabaseIncorrectValueException(PwsDatabaseMessage.NULL_CONTEXT_VALUE);
        }
    }
}
