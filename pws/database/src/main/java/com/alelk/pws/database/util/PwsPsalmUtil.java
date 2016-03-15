package com.alelk.pws.database.util;

import android.text.TextUtils;

import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmChorus;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmPartType;
import com.alelk.pws.database.data.PsalmVerse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alex Elkin on 14.03.2016.
 */
public class PwsPsalmUtil {

    private static final String VERSE_LBL = "Verse";
    private static final String CHORUS_LBL = "Chorus";

    public final static String PSALM_VERSE_NUMBER_REGEX = "^\\s*+(\\d{1,2})\\.\\s*+$";
    public final static String PSALM_VERSE_LABEL_REGEX = "^\\s*+(" + VERSE_LBL + ")\\s*+(\\d{1,2})\\.\\s*+$";
    public final static String PSALM_CHORUS_NUMBER_REGEX = "^\\s*+(" + CHORUS_LBL + ")\\s*+(\\d{1,2})??:\\s*+$";
    public final static String PSALM_CHORUS_LABEL_REGEX = "^\\s*+(" + CHORUS_LBL + ")\\s*+(\\d{1,2})??\\.\\s*+$";

    public static String convertPsalmPartsToPlainText(final SortedMap<Integer, PsalmPart> psalmParts) {
        List<PsalmVerse> psalmVerses = new ArrayList<>();
        List<PsalmChorus> psalmChoruses = new ArrayList<>();
        int countOfChoruses = countOfChoruses(psalmParts);
        StringBuilder builder = new StringBuilder();
        for (PsalmPart psalmPart : psalmParts.values()) {
            if (psalmPart.getPsalmType() == PsalmPartType.VERSE) {
                if (!psalmVerses.contains((PsalmVerse) psalmPart)) {
                    psalmVerses.add((PsalmVerse) psalmPart);
                    builder.append(psalmVerses.indexOf((PsalmVerse) psalmPart) + 1).append(".\n");
                    builder.append(psalmPart.getText().trim()).append("\n \n");
                } else {
                    builder.append(VERSE_LBL + " ").append(psalmVerses.indexOf((PsalmVerse) psalmPart) + 1).append(".\n \n");
                }
            } else if (psalmPart.getPsalmType() == PsalmPartType.CHORUS) {
                if (!psalmChoruses.contains((PsalmChorus) psalmPart)) {
                    psalmChoruses.add((PsalmChorus) psalmPart);
                    builder.append(CHORUS_LBL);
                    if (countOfChoruses > 1) {
                        builder.append(" " + (psalmChoruses.indexOf((PsalmChorus) psalmPart) + 1));
                    }
                    builder.append(":\n");
                    builder.append(psalmPart.getText().trim()).append("\n \n");
                } else {
                    builder.append(CHORUS_LBL);
                    if (countOfChoruses > 1) {
                        builder.append(" " + (psalmChoruses.indexOf((PsalmChorus) psalmPart) + 1));
                    }
                    builder.append(".\n \n");
                }
            }
        }
        return builder.toString().trim();
    }

    public static SortedMap<Integer, PsalmPart> parsePsalmParts(final String text) {
        SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();
        Map<Integer, Integer> chorusesMap = new TreeMap<>();
        Map<Integer, Integer> versesMap = new TreeMap<>();
        StringTokenizer tokenizer = new StringTokenizer(text, "\n");
        PsalmPart psalmPart = null;
        String psalmPartText = null;
        List<Integer> psalmPartNumbers;
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if(line.matches(PSALM_VERSE_NUMBER_REGEX)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                psalmPart = new PsalmVerse();
                psalmPart.setNumbers(Arrays.asList(psalmParts.size() + 1));
                versesMap.put(Integer.parseInt(parseValue(PSALM_VERSE_NUMBER_REGEX, line, 1)), psalmParts.size() + 1);
                psalmPartText = "";
            } else if (line.matches(PSALM_CHORUS_NUMBER_REGEX)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                psalmPart = new PsalmChorus();
                psalmPart.setNumbers(Arrays.asList(psalmParts.size() + 1));
                try {
                    chorusesMap.put(Integer.parseInt(parseValue(PSALM_CHORUS_NUMBER_REGEX, line, 2)), psalmParts.size() + 1);
                } catch (NumberFormatException ex) {
                    chorusesMap.put(1, psalmParts.size() + 1);
                }
                psalmPartText = "";
            } else if (line.matches(PSALM_VERSE_LABEL_REGEX)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                int verseNum = Integer.parseInt(parseValue(PSALM_VERSE_LABEL_REGEX, line, 2));
                psalmPart = psalmParts.get(versesMap.get(verseNum));
                psalmPartNumbers = new ArrayList<>();
                psalmPartNumbers.addAll(psalmPart.getNumbers());
                psalmPartNumbers.add(psalmParts.size() + 1);
                psalmPart.setNumbers(psalmPartNumbers);
                for (int n : psalmPartNumbers) {
                    psalmParts.put(n, psalmPart);
                }
                psalmPart = null;

            } else if (line.matches(PSALM_CHORUS_LABEL_REGEX)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                int chorusNum;
                try {
                    chorusNum = Integer.parseInt(parseValue(PSALM_CHORUS_LABEL_REGEX, line, 2));
                } catch (NumberFormatException ex) {
                    chorusNum = 1;
                }
                psalmPart = psalmParts.get(chorusesMap.get(chorusNum));
                psalmPartNumbers = new ArrayList<>();
                psalmPartNumbers.addAll(psalmPart.getNumbers());
                psalmPartNumbers.add(psalmParts.size() + 1);
                psalmPart.setNumbers(psalmPartNumbers);
                for (int n : psalmPartNumbers) {
                    psalmParts.put(n, psalmPart);
                }
                psalmPart = null;

            } else {
                if (!TextUtils.isEmpty(line.trim())) {
                    psalmPartText += line.trim() + "\n";
                }
                if (!tokenizer.hasMoreTokens()) {
                    if (psalmPart == null) continue;
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
            }
        }
        return psalmParts.size() == 0 ? null : psalmParts;
    }

    private static int countOfChoruses(final SortedMap<Integer, PsalmPart> psalmParts) {
        ArrayList<PsalmChorus> choruses = new ArrayList<>();
        for (PsalmPart psalmPart : psalmParts.values()) {
            if (psalmPart.getPsalmType() == PsalmPartType.CHORUS && !choruses.contains((PsalmChorus) psalmPart)) {
                choruses.add((PsalmChorus)psalmPart);
            }
        }
        return choruses.size();
    }

    private static String parseValue(String regex, String fileLine, int group) {
        Matcher matcher = Pattern.compile(regex).matcher(fileLine);
        if (matcher.find()) return matcher.group(group);
        return null;
    }
}
