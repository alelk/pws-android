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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
