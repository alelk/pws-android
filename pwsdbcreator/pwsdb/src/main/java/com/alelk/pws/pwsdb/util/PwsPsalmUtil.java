package com.alelk.pws.pwsdb.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alelk.pws.pwsdb.R;
import com.alelk.pws.pwsdb.data.PsalmChorus;
import com.alelk.pws.pwsdb.data.PsalmPart;
import com.alelk.pws.pwsdb.data.PsalmPartType;
import com.alelk.pws.pwsdb.data.PsalmVerse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alex Elkin on 14.03.2016.
 */
public class PwsPsalmUtil {

    public final static String PSALM_VERSE_NUMBER_REGEX = "^\\s*+(\\d{1,2})\\.\\s*+$";
    public final static String PSALM_VERSE_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})\\]\\s*+$";
    public final static String PSALM_CHORUS_NUMBER_FORMAT = "^\\s*+(%s)\\s*+(\\d{1,2})??:\\s*+$";
    public final static String PSALM_CHORUS_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})??\\]\\s*+$";

    private static String getPsalmVerseNumberRegex(Context context, Locale locale) {
        return PSALM_VERSE_NUMBER_REGEX;
    }

    private static String getPsalmVerseLabelRegex(Context context, Locale locale) {
        return String.format(PSALM_VERSE_LABEL_FORMAT, getLocalizedVerseLabel(context, locale));
    }

    private static String getPsalmChorusNumberRegex(Context context, Locale locale) {
        return String.format(PSALM_CHORUS_NUMBER_FORMAT, getLocalizedChorusLabel(context, locale));
    }

    private static String getPsalmChorusLabelRegex(Context context, Locale locale) {
        return String.format(PSALM_CHORUS_LABEL_FORMAT, getLocalizedChorusLabel(context, locale));
    }

    /**
     * Converts psalm part map to plain text
     * @param context app context to access localized  label resources
     * @param locale psalm locale
     * @param psalmParts psalm part map
     * @return text of psalm
     */
    public static String convertPsalmPartsToPlainText(@NonNull Context context,
                                                      @NonNull Locale locale,
                                                      @NonNull final SortedMap<Integer, PsalmPart> psalmParts) {
        List<PsalmVerse> psalmVerses = new ArrayList<>();
        List<PsalmChorus> psalmChoruses = new ArrayList<>();
        int countOfChoruses = countOfChoruses(psalmParts);
        StringBuilder builder = new StringBuilder();
        for (PsalmPart psalmPart : psalmParts.values()) {
            if (psalmPart.getPsalmType() == PsalmPartType.VERSE) {
                if (!psalmVerses.contains(psalmPart)) {
                    psalmVerses.add((PsalmVerse) psalmPart);
                    builder.append(psalmVerses.indexOf(psalmPart) + 1).append(".\n");
                    builder.append(psalmPart.getText().trim()).append("\n \n");
                } else {
                    builder.append("[").append(getLocalizedVerseLabel(context, locale)).append(" ").append(psalmVerses.indexOf((PsalmVerse) psalmPart) + 1).append("]\n \n");
                }
            } else if (psalmPart.getPsalmType() == PsalmPartType.CHORUS) {
                if (!psalmChoruses.contains(psalmPart)) {
                    psalmChoruses.add((PsalmChorus) psalmPart);
                    builder.append(getLocalizedChorusLabel(context, locale));
                    if (countOfChoruses > 1) {
                        builder.append(" " + (psalmChoruses.indexOf(psalmPart) + 1));
                    }
                    builder.append(":\n");
                    builder.append(psalmPart.getText().trim()).append("\n \n");
                } else {
                    builder.append("[").append(getLocalizedChorusLabel(context, locale));
                    if (countOfChoruses > 1) {
                        builder.append(" " + (psalmChoruses.indexOf(psalmPart) + 1));
                    }
                    builder.append("]\n \n");
                }
            }
        }
        return builder.toString().trim();
    }

    public static SortedMap<Integer, PsalmPart> parsePsalmParts(Context context, Locale locale, final String text) {
        final String pVerseNumberRgx = getPsalmVerseNumberRegex(context, locale);
        final String pVerseLabelRgx = getPsalmVerseLabelRegex(context, locale);
        final String pChorusNumberRgx = getPsalmChorusNumberRegex(context, locale);
        final String pChorusLabelRgx = getPsalmChorusLabelRegex(context, locale);
        SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();
        Map<Integer, Integer> chorusesMap = new TreeMap<>();
        Map<Integer, Integer> versesMap = new TreeMap<>();
        StringTokenizer tokenizer = new StringTokenizer(text, "\n");
        PsalmPart psalmPart = null;
        String psalmPartText = null;
        List<Integer> psalmPartNumbers;
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if(line.matches(pVerseNumberRgx)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                psalmPart = new PsalmVerse();
                psalmPart.setNumbers(Arrays.asList(psalmParts.size() + 1));
                versesMap.put(Integer.parseInt(parseValue(pVerseNumberRgx, line, 1)), psalmParts.size() + 1);
                psalmPartText = "";
            } else if (line.matches(pChorusNumberRgx)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                psalmPart = new PsalmChorus();
                psalmPart.setNumbers(Arrays.asList(psalmParts.size() + 1));
                try {
                    chorusesMap.put(Integer.parseInt(parseValue(pChorusNumberRgx, line, 2)), psalmParts.size() + 1);
                } catch (NumberFormatException ex) {
                    chorusesMap.put(1, psalmParts.size() + 1);
                }
                psalmPartText = "";
            } else if (line.matches(pVerseLabelRgx)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                int verseNum = Integer.parseInt(parseValue(pVerseLabelRgx, line, 2));
                psalmPart = psalmParts.get(versesMap.get(verseNum));
                psalmPartNumbers = new ArrayList<>();
                psalmPartNumbers.addAll(psalmPart.getNumbers());
                psalmPartNumbers.add(psalmParts.size() + 1);
                psalmPart.setNumbers(psalmPartNumbers);
                for (int n : psalmPartNumbers) {
                    psalmParts.put(n, psalmPart);
                }
                psalmPart = null;

            } else if (line.matches(pChorusLabelRgx)) {
                if (psalmPart != null) {
                    psalmPart.setText(psalmPartText);
                    psalmParts.put(psalmPart.getNumbers().get(0), psalmPart);
                }
                int chorusNum;
                try {
                    chorusNum = Integer.parseInt(parseValue(pChorusLabelRgx, line, 2));
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

    public static String psalmTextToHtml(Context context, Locale locale, String psalmText) {
        StringTokenizer tokenizer = new StringTokenizer(psalmText, "\n");
        final String pVerseNumberRgx = getPsalmVerseNumberRegex(context, locale);
        final String pVerseLabelRgx = getPsalmVerseLabelRegex(context, locale);
        final String pChorusNumberRgx = getPsalmChorusNumberRegex(context, locale);
        final String pChorusLabelRgx = getPsalmChorusLabelRegex(context, locale);

        String html = "";

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.matches(pVerseLabelRgx) || line.matches(pChorusLabelRgx)) {
                html += "<font color='#888888'><i>" + line + "</i></font>";
            } else if (line.matches(pVerseNumberRgx) || line.matches(pChorusNumberRgx)) {
                html += "<h1><font color='#7aaf83'>" + line.replace('.', ' ') + "</font></h1>";
            } else {
                html += line + "<br>";
            }
        }
        return html;
    }

    public static String getLocalizedChorusLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.chorus);
    }

    public static String getLocalizedVerseLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.verse);
    }

    private static String getLocalizedString(Context context, Locale locale, int resource) {
        Resources baseRes = context.getResources();
        Configuration configuration = new Configuration(baseRes.getConfiguration());
        configuration.locale = locale;
        Resources localRes = new Resources(baseRes.getAssets(), baseRes.getDisplayMetrics(), configuration);
        return localRes.getString(resource);
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
