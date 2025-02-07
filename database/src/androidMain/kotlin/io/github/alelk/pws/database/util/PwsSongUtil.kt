package io.github.alelk.pws.database.util

import android.text.TextUtils
import io.github.alelk.pws.database.util.LocalizedStringsProvider.getResource
import java.util.Locale

/**
 * Pws Psalm Util
 *
 * Created by Alex Elkin on 14.03.2016.
 */
object PwsSongUtil {
  private var builder = PwsSongHtmlBuilder(Locale.getDefault())

  @JvmOverloads
  fun songTextToHtml(locale: Locale?, songText: String?, isExpanded: Boolean = true): String {
    builder = builder.forLocale(locale)
    return builder.buildHtml(songText!!, isExpanded)
  }

  fun songTextToPrettyHtml(
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
    songAuthor: String?,
    songTranslator: String?,
    music: String?
  ): String? {
    val songInfo = ArrayList<String?>()
    if (songAuthor != null) songInfo.add(
      "<b>" + getLocalizedString(
        "lbl_author",
        locale
      ) + ":</b> " + songAuthor
    )
    if (songTranslator != null) songInfo.add(
      "<b>" + getLocalizedString(
        "lbl_translator",
        locale
      ) + ":</b> " + songTranslator
    )
    if (music != null) songInfo.add(
      "<b>" + getLocalizedString(
        "lbl_music",
        locale
      ) + ":</b> " + music
    )
    return if (songInfo.size == 0) null else TextUtils.join("<br>", songInfo)
  }

  private fun getLocalizedString(stringKey: String, locale: Locale): String? {
    return getResource(stringKey, locale)
  }
}
