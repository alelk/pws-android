package com.alelk.pws.database.data;

/**
 * Created by alelkin on 25.03.2015.
 */
public class PsalmVerse extends PsalmPart {
    @Override
    public PsalmPartType getPsalmType() {
        return PsalmPartType.VERSE;
    }
}
