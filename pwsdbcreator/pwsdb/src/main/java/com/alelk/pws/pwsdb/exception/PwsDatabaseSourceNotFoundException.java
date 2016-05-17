package com.alelk.pws.pwsdb.exception;

/**
 * Created by Alex Elkin on 05.05.2015.
 */
public class PwsDatabaseSourceNotFoundException extends PwsDatabaseException {
    public PwsDatabaseSourceNotFoundException(PwsDatabaseMessage pwsDatabaseMessage) {
        super(pwsDatabaseMessage);
    }
}
