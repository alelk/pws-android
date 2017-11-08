package com.alelk.pws.database.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Pws Psalm Util
 *
 * Created by Alex Elkin on 14.03.2016.
 */
public class PwsPsalmUtil {

    private final static String PSALM_VERSE_NUMBER_REGEX = "^\\s*+(\\d{1,2})\\.\\s*+$";
    private final static String PSALM_VERSE_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})\\]\\s*+$";
    private final static String PSALM_CHORUS_NUMBER_FORMAT = "^\\s*+(%s)\\s*+(\\d{1,2})??:\\s*+$";
    private final static String PSALM_CHORUS_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})??\\]\\s*+$";

    private static String getPsalmVerseNumberRegex() {
        return PSALM_VERSE_NUMBER_REGEX;
    }

    private static String getPsalmVerseLabelRegex(Locale locale) {
        return String.format(PSALM_VERSE_LABEL_FORMAT, getLocalizedString("lbl_verse", locale));
    }

    private static String getPsalmChorusNumberRegex(Locale locale) {
        return String.format(PSALM_CHORUS_NUMBER_FORMAT, getLocalizedString("lbl_chorus", locale));
    }

    private static String getPsalmChorusLabelRegex(Locale locale) {
        return String.format(PSALM_CHORUS_LABEL_FORMAT, getLocalizedString("lbl_chorus", locale));
    }

    public static String psalmTextToHtml(Locale locale, String psalmText) {
        StringTokenizer tokenizer = new StringTokenizer(psalmText, "\n");
        final String pVerseNumberRgx = getPsalmVerseNumberRegex();
        final String pVerseLabelRgx = getPsalmVerseLabelRegex(locale);
        final String pChorusNumberRgx = getPsalmChorusNumberRegex(locale);
        final String pChorusLabelRgx = getPsalmChorusLabelRegex(locale);

        StringBuilder html = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.matches(pVerseLabelRgx) || line.matches(pChorusLabelRgx)) {
                html.append("<font color='#888888'><i>").append(line).append("</i></font><br>");
            } else if (line.matches(pVerseNumberRgx) || line.matches(pChorusNumberRgx)) {
                html.append("<font color='#7aaf83'>").append(line.replace('.', ' ')).append("</font><br>");
            } else {
                html.append(line).append("<br>");
            }
        }
        return html.toString();
    }

    @Nullable
    public static String buildPsalmInfoHtml(@NonNull Locale locale,
                                            @Nullable String psalmAuthor,
                                            @Nullable String psalmTranslator,
                                            @Nullable String music) {
        final ArrayList<String> psalmInfo = new ArrayList<>();
        if (psalmAuthor != null)
            psalmInfo.add("<b>" + getLocalizedString("lbl_author", locale) + ":</b> " + psalmAuthor);
        if (psalmTranslator != null)
            psalmInfo.add("<b>" + getLocalizedString("lbl_translator", locale) + ":</b> " + psalmTranslator);
        if (music != null)
            psalmInfo.add ("<b>" + getLocalizedString("lbl_music", locale) + ":</b> " + music);
        if (psalmInfo.size() == 0) return null;
        return TextUtils.join("<br>", psalmInfo);
    }

    private static String getLocalizedString(String stringKey, Locale locale) {
        return LocalizedStringsProvider.getResource(stringKey, locale);
    }
}
