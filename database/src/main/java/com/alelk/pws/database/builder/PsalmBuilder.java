package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmVerse;
import com.alelk.pws.database.data.entity.ChorusEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.VerseEntity;
import com.alelk.pws.database.query.PwsDatabaseQuery;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public class PsalmBuilder implements PwsBuilder<Psalm, PsalmEntity> {

    private PsalmEntity psalmEntity;
    private Set<VerseEntity> verseEntities;
    private Set<ChorusEntity> chorusEntities;

    public PsalmBuilder(PsalmEntity psalmEntity) {
        this.psalmEntity = psalmEntity;
    }

    public PsalmBuilder(PsalmEntity psalmEntity, Set<VerseEntity> verseEntities, Set<ChorusEntity> chorusEntities) {
        this.psalmEntity = psalmEntity;
        this.verseEntities = verseEntities;
        this.chorusEntities = chorusEntities;
    }

    @Override
    public PwsBuilder<Psalm, PsalmEntity> appendEntity(PsalmEntity entity) {
        psalmEntity = entity;
        return this;
    }

    public PwsBuilder<Psalm, PsalmEntity> appendVerses(Set<VerseEntity> verseEntities) {
        this.verseEntities = verseEntities;
        return this;
    }

    public PwsBuilder<Psalm, PsalmEntity> appendChoruses(Set<ChorusEntity> chorusEntities) {
        this.chorusEntities = chorusEntities;
        return this;
    }

    @Override
    public Psalm toObject() {
        Psalm psalm = null;
        if (psalmEntity != null) {
            psalm.setName(psalmEntity.getName());
            psalm.setVersion(psalmEntity.getVersion());
            psalm.setAuthor(psalmEntity.getAuthor());
            psalm.setTranslator(psalmEntity.getTranslator());
            psalm.setComposer(psalmEntity.getComposer());
            psalm.setTonalities(Arrays.asList(TextUtils.split(psalmEntity.getTonalities(), PwsDatabaseQuery.MULTIVALUE_DELIMITER)));
            // todo set year

            SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();
            for (VerseEntity verseEntity : verseEntities) {
                PsalmVerse psalmVerse = new PsalmVerseBuilder().appendEntity(verseEntity).toObject();
                if (psalmVerse != null && psalmVerse.getNumbers() != null){
                    for (int number : psalmVerse.getNumbers()) {
                        psalmParts.put(number, psalmVerse);
                    }
                }
            }
            for (ChorusEntity chorusEntity : chorusEntities) {
                PsalmChorus psalmChorus = new PsalmChorusBuilder().appendEntity(chorusEntity).toObject();
                if (psalmChorus != null && psalmChorus.getNumbers() != null){
                    for (int number : psalmChorus.getNumbers()) {
                        psalmParts.put(number, psalmChorus);
                    }
                }
            }
            psalm.setPsalmParts(psalmParts);
        }
        return psalm;
    }
}
