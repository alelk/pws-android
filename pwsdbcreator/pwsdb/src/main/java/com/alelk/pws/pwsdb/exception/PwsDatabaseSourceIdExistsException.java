package com.alelk.pws.pwsdb.exception;

/**
 * Created by alelkin on 28.04.2015.
 */
public class PwsDatabaseSourceIdExistsException extends PwsDatabaseException {

    private long id;

    public PwsDatabaseSourceIdExistsException(PwsDatabaseMessage pwsDatabaseMessage, long id) {
        super(pwsDatabaseMessage);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
