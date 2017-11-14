package com.alelk.pws.database.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Pws Psalm Util
 *
 * Created by Alex Elkin on 14.03.2016.
 */
public class PwsPsalmUtil {

    private static PwsPsalmHtmlBuilder builder = new PwsPsalmHtmlBuilder(Locale.getDefault());

    public static String psalmTextToHtml(Locale locale, String psalmText) {
        return psalmTextToHtml(locale, psalmText, true);
    }

    public static String psalmTextToHtml(Locale locale, String psalmText, boolean isExpanded) {
        builder = builder.forLocale(locale);
        return builder.buildHtml(psalmText, isExpanded);
    }

    public static String psalmTextToPrettyHtml(
            Locale locale, String psalmText, String bibleRef, String psalmName, Integer psalmNumber,
            String author, String translator, String composer, String footerHtml
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div>");
        if (psalmName != null) sb.append("<h1>")
                .append(psalmNumber != null ? "№ " + psalmNumber + " " : null).append(psalmName)
                .append("</h1>");
        if (bibleRef != null) sb.append("<p>").append(bibleRef).append("</p>");
        sb.append("<p>").append(psalmTextToHtml(locale, psalmText)).append("</p>");
        if (author != null || translator != null || composer != null)
            sb.append("<p>").append(buildPsalmInfoHtml(locale, author, translator, composer)).append("</p>");
        if (footerHtml != null) sb.append("<p>").append(footerHtml).append("</p>");
        sb.append("</div>");
        return sb.toString();
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
