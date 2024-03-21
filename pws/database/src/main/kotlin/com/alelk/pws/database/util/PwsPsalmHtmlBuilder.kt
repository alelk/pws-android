/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alelk.pws.database.util;

import android.text.TextUtils;
import android.util.SparseArray;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pws Psalm Html Builder
 *
 * Created by Alex Elkin on 13.11.2017.
 */

public class PwsPsalmHtmlBuilder {

    private static final String LOG_TAG = PwsPsalmHtmlBuilder.class.getSimpleName();
    private final static String PSALM_VERSE_NUMBER_REGEX = "^\\s*+(\\d{1,2})\\.\\s*+$";
    private final static String PSALM_VERSE_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})\\]\\s*+$";
    private final static String PSALM_CHORUS_NUMBER_FORMAT = "^\\s*+(%s)\\s*+(\\d{1,2})??:\\s*+$";
    private final static String PSALM_CHORUS_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})??\\]\\s*+$";

    private Locale locale;
    private Pattern verseNumberPattern;
    private Pattern verseLabelPattern;
    private Pattern chorusNumberPattern;
    private Pattern chorusLabelPattern;
    private String psalmPartType = null;
    private int psalmPartNumber = 0;
    private StringBuilder psalmPartText = new StringBuilder();
    private final SparseArray<String> choruses = new SparseArray<>();
    private final SparseArray<String> verses = new SparseArray<>();

    public PwsPsalmHtmlBuilder(Locale locale) {
        this.locale = locale;
        verseNumberPattern = getPsalmVerseNumberPattern();
        verseLabelPattern = getPsalmVerseLabelPattern(locale);
        chorusNumberPattern = getPsalmChorusNumberPattern(locale);
        chorusLabelPattern = getPsalmChorusLabelPattern(locale);
    }

    public PwsPsalmHtmlBuilder forLocale(Locale locale) {
        if (this.locale != null && this.locale.equals(locale) || locale != null && locale.equals(this.locale)) return this;
        return new PwsPsalmHtmlBuilder(locale);
    }

    public String buildHtml(String psalmText, boolean isExpanded) {
        if (isExpanded) return buildExpandedHtml(psalmText);
        return buildHtml(psalmText);
    }

    private String buildExpandedHtml(String psalmText) {

        psalmPartType = null;
        psalmPartNumber = 0;
        psalmPartText = new StringBuilder();
        choruses.clear();
        verses.clear();

        final StringBuilder html = new StringBuilder();
        final StringTokenizer tokenizer = new StringTokenizer(psalmText, "\n");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.matches(verseNumberPattern.pattern())) {
                endPsalmPart();
                Matcher matcher = verseNumberPattern.matcher(line);
                if (matcher.find())
                    startPsalmPart("verse", matcher.group(1));
                html.append("<font color='#7aaf83'>").append(line.replace('.', ' ')).append("</font><br>");
            } else if (line.matches(chorusNumberPattern.pattern())) {
                endPsalmPart();
                Matcher matcher = chorusNumberPattern.matcher(line);
                if (matcher.find())
                    startPsalmPart("chorus", matcher.group(2));
                html.append("<font color='#7aaf83'>").append(line.replace('.', ' ')).append("</font><br>");
            } else if (line.matches(verseLabelPattern.pattern())) {
                endPsalmPart();
                final Matcher matcher = verseLabelPattern.matcher(line);
                if (matcher.find()) {
                    html.append("<font color='#999999'>").append(matcher.group(1))
                            .append(TextUtils.isEmpty(matcher.group(2)) ? "" : " " + matcher.group(2)).append("</font><br>");
                    html.append(verses.get(parsePsalmPartNumber(matcher.group(2))));
                }
            } else if (line.matches(chorusLabelPattern.pattern())) {
                endPsalmPart();
                final Matcher matcher = chorusLabelPattern.matcher(line);
                if (matcher.find()) {
                    html.append("<font color='#999999'>").append(matcher.group(1))
                            .append(TextUtils.isEmpty(matcher.group(2)) ? "" : " " + matcher.group(2)).append("</font><br>");
                    html.append(choruses.get(parsePsalmPartNumber(matcher.group(2))));
                }
            } else {
                psalmPartText.append(line).append("<br>");
                html.append(line).append("<br>");
            }
        }
        return html.toString();
    }

    private void startPsalmPart(String psalmPartType, String psalmPartNumber) {
        this.psalmPartType = psalmPartType;
        this.psalmPartNumber = parsePsalmPartNumber(psalmPartNumber);
        psalmPartText = new StringBuilder();
    }

    private int parsePsalmPartNumber(String str) {
        if (str == null) return 1;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException exc) {
            return 1;
        }
    }

    private void endPsalmPart() {
        if (psalmPartType == null) return;
        if ("verse".equals(psalmPartType))
            verses.put(psalmPartNumber, psalmPartText.toString());
        if ("chorus".equals(psalmPartType))
            choruses.put(psalmPartNumber, psalmPartText.toString());
        psalmPartType = null;
    }

    private String buildHtml(String psalmText) {
        final StringTokenizer tokenizer = new StringTokenizer(psalmText, "\n");
        final StringBuilder html = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.matches(verseLabelPattern.pattern()) || line.matches(chorusLabelPattern.pattern())) {
                html.append("<font color='#888888'><i>").append(line).append("</i></font><br>");
            } else if (line.matches(verseNumberPattern.pattern()) || line.matches(chorusNumberPattern.pattern())) {
                html.append("<font color='#7aaf83'>").append(line.replace('.', ' ')).append("</font><br>");
            } else {
                html.append(line).append("<br>");
            }
        }
        return html.toString();
    }

    private static Pattern getPsalmVerseNumberPattern() {
        return Pattern.compile(PSALM_VERSE_NUMBER_REGEX);
    }

    private static Pattern getPsalmVerseLabelPattern(Locale locale) {
        return Pattern.compile(String.format(PSALM_VERSE_LABEL_FORMAT, getLocalizedString("lbl_verse", locale)));
    }

    private static Pattern getPsalmChorusNumberPattern(Locale locale) {
        return Pattern.compile(String.format(PSALM_CHORUS_NUMBER_FORMAT, getLocalizedString("lbl_chorus", locale)));
    }

    private static Pattern getPsalmChorusLabelPattern(Locale locale) {
        return Pattern.compile(String.format(PSALM_CHORUS_LABEL_FORMAT, getLocalizedString("lbl_chorus", locale)));
    }

    private static String getLocalizedString(String stringKey, Locale locale) {
        return LocalizedStringsProvider.getResource(stringKey, locale);
    }
}
