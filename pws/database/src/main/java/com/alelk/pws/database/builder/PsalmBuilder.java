package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.ChorusEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.VerseEntity;
import com.alelk.pws.database.query.PwsDatabaseQuery;
import com.alelk.pws.database.util.PwsPsalmUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alex Elkin on 06.05.2015.
 * Edited by Alex Elkin on 14.06.2015: code refactoring - psalm text field has been added.
 */
public class PsalmBuilder implements PwsBuilder<Psalm, PsalmEntity> {

    private PsalmEntity psalmEntity;
    private String text;
    private Map<BookEdition, Integer> numbers;

    public PsalmBuilder(PsalmEntity psalmEntity) {
        this.psalmEntity = psalmEntity;
    }

    public PsalmBuilder(PsalmEntity psalmEntity, Map<BookEdition, Integer> numbers) {
        this.psalmEntity = psalmEntity;
        this.numbers = numbers;
    }

    @Override
    public PwsBuilder<Psalm, PsalmEntity> appendEntity(PsalmEntity entity) {
        psalmEntity = entity;
        return this;
    }

    public PwsBuilder<Psalm, PsalmEntity> appendNumbers(Map<BookEdition, Integer> numbers) {
        this.numbers = numbers;
        return this;
    }

    @Override
    public Psalm toObject() {
        Psalm psalm = null;
        if (psalmEntity != null) {
            psalm = new Psalm();
            psalm.setName(psalmEntity.getName());
            psalm.setVersion(psalmEntity.getVersion());
            psalm.setAuthor(psalmEntity.getAuthor());
            psalm.setTranslator(psalmEntity.getTranslator());
            psalm.setComposer(psalmEntity.getComposer());
            if (psalmEntity.getTonalities() != null) {
                psalm.setTonalities(Arrays.asList(TextUtils.split(psalmEntity.getTonalities(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            }
            psalm.setYear(psalmEntity.getYear());
            psalm.setAnnotation(psalmEntity.getAnnotation());
            psalm.setPsalmParts(PwsPsalmUtil.parsePsalmParts(psalmEntity.getText()));
            psalm.setNumbers(numbers);
        }
        return psalm;
    }
}
