package com.alelk.pws.database.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.alelk.pws.database.R;

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

    @Nullable
    public static String buildPsalmInfoHtml(@NonNull Context context,
                                            @NonNull Locale locale,
                                            @Nullable String psalmAuthor,
                                            @Nullable String psalmTranslator,
                                            @Nullable String music) {
        final ArrayList<String> psalmInfo = new ArrayList<>();
        if (psalmAuthor != null)
            psalmInfo.add("<b>" + getLocalizedAuthorLabel(context, locale) + ":</b> " + psalmAuthor);
        if (psalmTranslator != null)
            psalmInfo.add("<b>" + getLocalizedTranslatorLabel(context, locale) + ":</b> " + psalmTranslator);
        if (music != null)
            psalmInfo.add ("<b>" + getLocalizedMusicLabel(context, locale) + ":</b> " + music);
        if (psalmInfo.size() == 0) return null;
        return TextUtils.join("<br>", psalmInfo);
    }

    @NonNull
    private static String getLocalizedAuthorLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.lbl_author);
    }

    private static String getLocalizedTranslatorLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.lbl_translator);
    }

    @NonNull
    private static String getLocalizedMusicLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.lbl_music);
    }

    @NonNull
    private static String getLocalizedChorusLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.lbl_chorus);
    }

    @NonNull
    private static String getLocalizedVerseLabel(Context context, Locale locale) {
        return getLocalizedString(context, locale, R.string.lbl_verse);
    }

    @NonNull
    private static String getLocalizedString(Context context, Locale locale, int resource) {
        Resources baseRes = context.getResources();
        Configuration configuration = new Configuration(baseRes.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
        Resources localRes = new Resources(baseRes.getAssets(), baseRes.getDisplayMetrics(), configuration);
        return localRes.getString(resource);
    }
}
