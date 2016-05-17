package com.alelk.pws.pwsdb.exception;

/**
 * Created by alelkin on 25.03.2015.
 */
public class PwsDatabaseIncorrectValueException extends PwsDatabaseException {
    public PwsDatabaseIncorrectValueException() {}

    public PwsDatabaseIncorrectValueException(PwsDatabaseMessage pwsDatabaseMessage) {
        super(pwsDatabaseMessage);
    }

    @Override
    public String toString() {
        return "PwsDatabaseIncorrectValueException{" +
                "pwsDatabaseMessage=" + super.getPwsDatabaseMessage() +
                '}';
    }
}
