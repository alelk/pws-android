package com.alelk.pws.database.query;

import com.alelk.pws.database.data.PwsObject;
import com.alelk.pws.database.data.entity.PwsDatabaseEntity;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;

/**
 * Created by alelkin on 29.04.2015.
 */
public interface PwsDatabaseQuery <T extends PwsObject, E extends PwsDatabaseEntity> {

    public final static String MULTIVALUE_DELIMITER = "; ";

    /**
     * Insert PwsObject into Pws database.
     * @param pwsObject The PwsObject to insert
     * @return PwsDatabaseEntity inserted into database.
     * @throws PwsDatabaseSourceIdExistsException if PwsObject already exists in database
     */
    E insert(T pwsObject) throws PwsDatabaseSourceIdExistsException;

    /**
     * Returns the PwsDatabaseEntity from Pws database selected by id
     * @param id the id of PwsDatabaseEntity to select
     * @return PwsDatabaseEntity with specified id, null if no PwsDatabaseEntity found
     */
    E selectById(long id);
}
