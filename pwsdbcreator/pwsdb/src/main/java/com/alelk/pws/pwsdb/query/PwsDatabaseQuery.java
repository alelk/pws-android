package com.alelk.pws.pwsdb.query;

import com.alelk.pws.pwsdb.data.PwsObject;
import com.alelk.pws.pwsdb.data.entity.PwsDatabaseEntity;
import com.alelk.pws.pwsdb.exception.PwsDatabaseException;
import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;

/**
 * Created by alelkin on 29.04.2015.
 */
public interface PwsDatabaseQuery <T extends PwsObject, E extends PwsDatabaseEntity> {

    public final static String MULTIVALUE_DELIMITER = "; ";

    /**
     * Insert PwsObject into Pws database.
     * @param pwsObject The PwsObject to insert
     * @return PwsDatabaseEntity inserted into database.
     * @throws PwsDatabaseException if any problems
     */
    E insert(T pwsObject) throws PwsDatabaseException;

    /**
     * Returns the PwsDatabaseEntity from Pws database selected by id
     * @param id the id of PwsDatabaseEntity to select
     * @return PwsDatabaseEntity with specified id, null if no PwsDatabaseEntity found
     */
    E selectById(long id) throws PwsDatabaseIncorrectValueException;

}
