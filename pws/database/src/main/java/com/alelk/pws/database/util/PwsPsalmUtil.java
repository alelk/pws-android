package com.alelk.pws.database.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.alelk.pws.database.R;

import java.util.Locale;
import java.util.StringTokenizer;

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
}
