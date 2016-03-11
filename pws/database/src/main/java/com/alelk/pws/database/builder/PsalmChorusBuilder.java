package com.alelk.pws.database.builder;

import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.entity.ChorusEntity;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public class PsalmChorusBuilder extends PwsBuilderUtils implements PwsBuilder<PsalmChorus, ChorusEntity> {

    private ChorusEntity chorusEntity;

    @Override
    public PwsBuilder<PsalmChorus, ChorusEntity> appendEntity(ChorusEntity entity) {
        chorusEntity = entity;
        return this;
    }

    @Override
    public PsalmChorus toObject() {
        PsalmChorus psalmChorus = null;
        if (chorusEntity != null) {
            psalmChorus = new PsalmChorus();
            psalmChorus.setNumbers(parseNumbersFromString(chorusEntity.getNumbers()));
            psalmChorus.setText(chorusEntity.getText());
        }
        return psalmChorus;
    }
}
