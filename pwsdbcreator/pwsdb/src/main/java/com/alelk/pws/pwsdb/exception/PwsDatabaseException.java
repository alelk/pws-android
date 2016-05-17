package com.alelk.pws.pwsdb.exception;

/**
 * Created by alelkin on 25.03.2015.
 */
public class PwsDatabaseException extends Exception {
    private PwsDatabaseMessage pwsDatabaseMessage;
    public PwsDatabaseException() {}
    public PwsDatabaseException(PwsDatabaseMessage pwsDatabaseMessage) {
        this.pwsDatabaseMessage = pwsDatabaseMessage;
    }

    public PwsDatabaseMessage getPwsDatabaseMessage() {
        return pwsDatabaseMessage;
    }
}
