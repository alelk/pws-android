package com.alelk.pws.database.exception;

import com.alelk.pws.database.R;

/**
 * Created by Alex Elkin on 28.04.2015.
 */
public enum PwsDatabaseMessage {
    BOOK_ID_EXISTS(1, R.string.book_id_exists),
    NULL_DATABASE_VALUE(2, R.string.null_database_value),
    NULL_PSALM_ID_VALUE(3, R.string.null_psalm_id_value),
    NULL_PSALM_VALUE(4, R.string.null_psalm_value),
    NULL_BOOK_EDITION_VALUE(5, R.string.null_book_edition_value),
    NO_BOOK_EDITION_FOUND(6, R.string.no_book_edition_found),
    UNEXPECTED_BOOK_EDITION_VALUE(7, R.string.unexpected_book_edition_value),
    PSALM_NUMBER_EXISTS_FOR_BOOK_ID(8, R.string.psalm_number_exists_for_book_id),
    NO_PSALM_NUMBERS(9, R.string.no_psalm_numbers);

    int errorCode;
    int errorMessageId;
    PwsDatabaseMessage(int errorCode, int errorMessageId){
        this.errorCode = errorCode;
        this.errorMessageId = errorMessageId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getErrorMessageId() {
        return errorMessageId;
    }
}
