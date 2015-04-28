package com.alelk.pws.database.exception;

/**
 * Created by alelkin on 28.04.2015.
 */
public class PwsDatabaseSourceIdExists extends PwsDatabaseException {

    private long id;

    public PwsDatabaseSourceIdExists(PwsDatabaseMessage pwsDatabaseMessage, long id) {
        super(pwsDatabaseMessage);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
