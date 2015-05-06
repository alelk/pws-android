package com.alelk.pws.database.builder;

import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.VerseEntity;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public class PsalmVerseBuilder extends PwsBuilderUtils implements PwsBuilder<PsalmVerse, VerseEntity> {

    private VerseEntity verseEntity;

    @Override
    public PwsBuilder<PsalmVerse, VerseEntity> appendEntity(VerseEntity entity) {
        verseEntity = entity;
        return this;
    }

    @Override
    public PsalmVerse toObject() {
        PsalmVerse psalmVerse = null;
        if (verseEntity != null) {
            psalmVerse = new PsalmVerse();
            psalmVerse.setNumbers(parseNumbersFromString(verseEntity.getNumbers()));
            psalmVerse.setText(verseEntity.getText());
        }
        return psalmVerse;
    }
}
