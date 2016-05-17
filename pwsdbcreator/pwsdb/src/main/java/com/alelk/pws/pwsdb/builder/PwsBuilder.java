package com.alelk.pws.pwsdb.builder;

import com.alelk.pws.pwsdb.data.PwsObject;
import com.alelk.pws.pwsdb.data.entity.PwsDatabaseEntity;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public interface PwsBuilder<T extends PwsObject, E extends PwsDatabaseEntity> {
    public PwsBuilder<T,E> appendEntity(E entity);
    public T toObject();
}
