package com.alelk.pws.pwapp.preference;

/**
 * Psalm Preferences
 *
 * Created by Alex Elkin on 13.11.2017.
 */

public class PsalmPreferences {

    private float textSize;
    private boolean expandPsalmText;

    public PsalmPreferences() {
    }

    public PsalmPreferences(float textSize, boolean expandPsalmText) {
        this.textSize = textSize;
        this.expandPsalmText = expandPsalmText;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public boolean isExpandPsalmText() {
        return expandPsalmText;
    }

    public void setExpandPsalmText(boolean expandPsalmText) {
        this.expandPsalmText = expandPsalmText;
    }
}
