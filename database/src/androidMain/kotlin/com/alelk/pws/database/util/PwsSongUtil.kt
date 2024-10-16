package com.alelk.pws.database.util

import android.text.TextUtils
import com.alelk.pws.database.util.LocalizedStringsProvider.getResource
import java.util.Locale

/**
 * Pws Psalm Util
 *
 * Created by Alex Elkin on 14.03.2016.
 */
object PwsSongUtil {
  private var builder = PwsPsalmHtmlBuilder(Locale.getDefault())

  @JvmOverloads
  fun songTextToHtml(locale: Locale?, psalmText: String?, isExpanded: Boolean = true): String {
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
    sb.append("<p>").append(songTextToHtml(locale, psalmText)).append("</p>")
    if (author != null || translator != null || composer != null) sb.append("<p>").append(
      buildSongInfoHtml(locale, author, translator, composer)
    ).append("</p>")
    if (footerHtml != null) sb.append("<p>").append(footerHtml).append("</p>")
    sb.append("</div>")
    return sb.toString()
  }

  fun buildSongInfoHtml(
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
