package io.github.alelk.pws.android.app.util

import android.text.TextUtils
import io.github.alelk.pws.android.app.util.LocalizedStringsProvider.getResource
import java.util.Locale
import java.util.StringTokenizer
import java.util.regex.Pattern

/**
 * Pws Psalm Html Builder
 *
 * Created by Alex Elkin on 13.11.2017.
 */
class PwsSongHtmlBuilder(private val locale: Locale?) {
  private val verseNumberPattern: Pattern = songVerseNumberPattern(locale)
  private val verseLabelPattern: Pattern = getSongVerseLabelPattern(locale)
  private val chorusNumberPattern: Pattern = getSongChorusNumberPattern(locale)
  private val chorusLabelPattern: Pattern = getSongChorusLabelPattern(locale)


  fun forLocale(locale: Locale?): PwsSongHtmlBuilder =
    if (this.locale != null && this.locale == locale || locale != null && locale == this.locale) this
    else PwsSongHtmlBuilder(locale)

  fun buildHtml(psalmText: String, isExpanded: Boolean): String =
    if (isExpanded) buildExpandedHtml(psalmText)
    else buildHtml(psalmText)

  private fun buildExpandedHtml(psalmText: String): String {

    var songPartType: String? = null
    var songPartNumber = 0
    val choruses = mutableMapOf<Int, String>()
    val verses = mutableMapOf<Int, String>()
    var songPartText = StringBuilder()

    fun startSongPart(ppt: String, ppn: String?) {
      songPartType = ppt
      songPartNumber = parseSongPartNumber(ppn)
      songPartText = StringBuilder()
    }

    fun endSongPart() {
      if (songPartType == null) return
      if ("verse" == songPartType) verses[songPartNumber] = songPartText.toString()
      if ("chorus" == songPartType) choruses[songPartNumber] = songPartText.toString()
      songPartType = null
    }

    return buildString {
      val tokenizer = StringTokenizer(psalmText, "\n")
      while (tokenizer.hasMoreTokens()) {
        val line = tokenizer.nextToken()
        when {
          line.matches(verseNumberPattern.pattern().toRegex()) -> {
            endSongPart()
            val matcher = verseNumberPattern.matcher(line)
            if (matcher.find()) startSongPart("verse", matcher.group(1))
            append("<font color='#7aaf83'>")
            append(line.replace('.', ' '))
            append("</font><br>")
          }

          line.matches(chorusNumberPattern.pattern().toRegex()) -> {
            endSongPart()
            val matcher = chorusNumberPattern.matcher(line)
            if (matcher.find()) startSongPart("chorus", matcher.group(2))
            append("<font color='#7aaf83'>")
            append(line.replace('.', ' '))
            append("</font><br>")
          }

          line.matches(verseLabelPattern.pattern().toRegex()) -> {
            endSongPart()
            val matcher = verseLabelPattern.matcher(line)
            if (matcher.find()) {
              append("<font color='#999999'>")
              append(matcher.group(1))
              append(if (TextUtils.isEmpty(matcher.group(2))) "" else " ${matcher.group(2)}").append("</font><br>")
              append(verses[parseSongPartNumber(matcher.group(2))])
            }
          }

          line.matches(chorusLabelPattern.pattern().toRegex()) -> {
            endSongPart()
            val matcher = chorusLabelPattern.matcher(line)
            if (matcher.find()) {
              append("<font color='#999999'>")
              append(matcher.group(1))
              append(if (matcher.group(2).isNullOrEmpty()) "" else " " + matcher.group(2))
              append("</font><br>")
              append(choruses[parseSongPartNumber(matcher.group(2))])
            }
          }

          else -> {
            songPartText.append(line).append("<br>")
            append(line)
            append("<br>")
          }
        }
      }
    }
  }


  private fun parseSongPartNumber(str: String?): Int =
    if (str == null) 1
    else try {
      str.toInt()
    } catch (exc: NumberFormatException) {
      1
    }

  private fun buildHtml(psalmText: String): String =
    buildString {
      val tokenizer = StringTokenizer(psalmText, "\n")
      while (tokenizer.hasMoreTokens()) {
        val line = tokenizer.nextToken()
        when {
          line.matches(verseLabelPattern.pattern().toRegex()) || line.matches(chorusLabelPattern.pattern().toRegex()) -> {
            append("<font color='#888888'><i>")
            append(line)
            append("</i></font><br>")
          }

          line.matches(verseNumberPattern.pattern().toRegex()) || line.matches(chorusNumberPattern.pattern().toRegex()) -> {
            append("<font color='#7aaf83'>")
            append(line.replace('.', ' '))
            append("</font><br>")
          }

          else -> {
            append(line)
            append("<br>")
          }
        }
      }
    }

  companion object {
    private const val SONG_VERSE_NUMBER_FORMAT = """^\s*(%s)?\s*(\d{1,2})\s*\.\s*$"""
    private const val SONG_VERSE_LABEL_FORMAT = """^\s*\[(%s)\s*(\d{1,2})\]\s*$"""
    private const val SONG_CHORUS_NUMBER_FORMAT = """^\s*(%s)\s*(\d{1,2})?\s*\.\s*$"""
    private const val SONG_CHORUS_LABEL_FORMAT = """^\s*\[(%s)\s*(\d{1,2})?\]\s*$"""
    private fun songVerseNumberPattern(locale: Locale?): Pattern =
      Pattern.compile(String.format(SONG_VERSE_NUMBER_FORMAT, getLocalizedString("lbl_verse", locale)))

    private fun getSongVerseLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(SONG_VERSE_LABEL_FORMAT, getLocalizedString("lbl_verse", locale)))
    }

    private fun getSongChorusNumberPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(SONG_CHORUS_NUMBER_FORMAT, getLocalizedString("lbl_chorus", locale)))
    }

    private fun getSongChorusLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(SONG_CHORUS_LABEL_FORMAT, getLocalizedString("lbl_chorus", locale)))
    }

    private fun getLocalizedString(stringKey: String, locale: Locale?): String? {
      return getResource(stringKey, locale!!)
    }
  }
}
