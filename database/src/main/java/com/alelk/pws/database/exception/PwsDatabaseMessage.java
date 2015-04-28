package com.alelk.pws.database.exception;

import com.alelk.pws.database.R;

/**
 * Created by alelkin on 28.04.2015.
 */
public enum PwsDatabaseMessage {
    BOOK_ID_EXISTS(1, R.string.book_id_exists);
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
