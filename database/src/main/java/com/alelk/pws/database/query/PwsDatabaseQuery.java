package com.alelk.pws.database.query;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.alelk.pws.database.data.PwsObject;
import com.alelk.pws.database.data.entity.PwsDatabaseEntity;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExists;

/**
 * Created by alelkin on 29.04.2015.
 */
public interface PwsDatabaseQuery <T extends PwsObject, E extends PwsDatabaseEntity> {

    public final static String MULTIVALUE_DELIMITER = "; ";

    /**
     * Insert PwsObject into Pws database.
     * @param pwsObject The PwsObject to insert
     * @return PwsDatabaseEntity inserted into database.
     * @throws PwsDatabaseSourceIdExists if PwsObject already exists in database
     */
    E insert(T pwsObject) throws PwsDatabaseSourceIdExists;

    /**
     * Returns the PwsDatabaseEntity from Pws database selected by id
     * @param id the id of PwsDatabaseEntity to select
     * @return PwsDatabaseEntity with specified id, null if no PwsDatabaseEntity found
     */
    E selectById(long id);
}
