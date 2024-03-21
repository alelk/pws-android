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
package com.alelk.pws.database.util

import android.text.TextUtils
import com.alelk.pws.database.util.LocalizedStringsProvider.getResource
import java.util.Locale

/**
 * Pws Psalm Util
 *
 * Created by Alex Elkin on 14.03.2016.
 */
object PwsPsalmUtil {
  private var builder = PwsPsalmHtmlBuilder(Locale.getDefault())

  @JvmOverloads
  fun psalmTextToHtml(locale: Locale?, psalmText: String?, isExpanded: Boolean = true): String {
    builder = builder.forLocale(locale)
    return builder.buildHtml(psalmText!!, isExpanded)
  }

  fun psalmTextToPrettyHtml(
    locale: Locale,
    psalmText: String?,
    bibleRef: String?,
    psalmName: String?,
    psalmNumber: Int?,
    author: String?,
    translator: String?,
    composer: String?,
    footerHtml: String?
  ): String {
    val sb = StringBuilder()
    sb.append("<div>")
    if (psalmName != null) sb.append("<h1>")
      .append(if (psalmNumber != null) "№ $psalmNumber " else null).append(psalmName)
      .append("</h1>")
    if (bibleRef != null) sb.append("<p>").append(bibleRef).append("</p>")
    sb.append("<p>").append(psalmTextToHtml(locale, psalmText)).append("</p>")
    if (author != null || translator != null || composer != null) sb.append("<p>").append(
      buildPsalmInfoHtml(locale, author, translator, composer)
    ).append("</p>")
    if (footerHtml != null) sb.append("<p>").append(footerHtml).append("</p>")
    sb.append("</div>")
    return sb.toString()
  }

  fun buildPsalmInfoHtml(
    locale: Locale,
    psalmAuthor: String?,
    psalmTranslator: String?,
    music: String?
  ): String? {
    val psalmInfo = ArrayList<String?>()
    if (psalmAuthor != null) psalmInfo.add(
      "<b>" + getLocalizedString(
        "lbl_author",
        locale
      ) + ":</b> " + psalmAuthor
    )
    if (psalmTranslator != null) psalmInfo.add(
      "<b>" + getLocalizedString(
        "lbl_translator",
        locale
      ) + ":</b> " + psalmTranslator
    )
    if (music != null) psalmInfo.add(
      "<b>" + getLocalizedString(
        "lbl_music",
        locale
      ) + ":</b> " + music
    )
    return if (psalmInfo.size == 0) null else TextUtils.join("<br>", psalmInfo)
  }

  private fun getLocalizedString(stringKey: String, locale: Locale): String? {
    return getResource(stringKey, locale)
  }
}
