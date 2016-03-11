package com.alelk.pws.database.builder;

import com.alelk.pws.database.data.PwsObject;
import com.alelk.pws.database.data.entity.PwsDatabaseEntity;

import java.util.Set;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public interface PwsBuilder<T extends PwsObject, E extends PwsDatabaseEntity> {
    public PwsBuilder<T,E> appendEntity(E entity);
    public T toObject();
}
