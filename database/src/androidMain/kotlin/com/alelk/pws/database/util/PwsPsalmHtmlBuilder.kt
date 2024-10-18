package com.alelk.pws.database.util

import android.text.TextUtils
import com.alelk.pws.database.util.LocalizedStringsProvider.getResource
import java.util.Locale
import java.util.StringTokenizer
import java.util.regex.Pattern

/**
 * Pws Psalm Html Builder
 *
 * Created by Alex Elkin on 13.11.2017.
 */
class PwsPsalmHtmlBuilder(private val locale: Locale?) {
  private val verseNumberPattern: Pattern = psalmVerseNumberPattern(locale)
  private val verseLabelPattern: Pattern = getPsalmVerseLabelPattern(locale)
  private val chorusNumberPattern: Pattern = getPsalmChorusNumberPattern(locale)
  private val chorusLabelPattern: Pattern = getPsalmChorusLabelPattern(locale)


  fun forLocale(locale: Locale?): PwsPsalmHtmlBuilder =
    if (this.locale != null && this.locale == locale || locale != null && locale == this.locale) this
    else PwsPsalmHtmlBuilder(locale)

  fun buildHtml(psalmText: String, isExpanded: Boolean): String =
    if (isExpanded) buildExpandedHtml(psalmText)
    else buildHtml(psalmText)

  private fun buildExpandedHtml(psalmText: String): String {

    var psalmPartType: String? = null
    var psalmPartNumber = 0
    val choruses = mutableMapOf<Int, String>()
    val verses = mutableMapOf<Int, String>()
    var psalmPartText = StringBuilder()

    fun startPsalmPart(ppt: String, ppn: String?) {
      psalmPartType = ppt
      psalmPartNumber = parsePsalmPartNumber(ppn)
      psalmPartText = StringBuilder()
    }

    fun endPsalmPart() {
      if (psalmPartType == null) return
      if ("verse" == psalmPartType) verses[psalmPartNumber] = psalmPartText.toString()
      if ("chorus" == psalmPartType) choruses[psalmPartNumber] = psalmPartText.toString()
      psalmPartType = null
    }

    return buildString {
      val tokenizer = StringTokenizer(psalmText, "\n")
      while (tokenizer.hasMoreTokens()) {
        val line = tokenizer.nextToken()
        when {
          line.matches(verseNumberPattern.pattern().toRegex()) -> {
            endPsalmPart()
            val matcher = verseNumberPattern.matcher(line)
            if (matcher.find()) startPsalmPart("verse", matcher.group(1))
            append("<font color='#7aaf83'>")
            append(line.replace('.', ' '))
            append("</font><br>")
          }

          line.matches(chorusNumberPattern.pattern().toRegex()) -> {
            endPsalmPart()
            val matcher = chorusNumberPattern.matcher(line)
            if (matcher.find()) startPsalmPart("chorus", matcher.group(2))
            append("<font color='#7aaf83'>")
            append(line.replace('.', ' '))
            append("</font><br>")
          }

          line.matches(verseLabelPattern.pattern().toRegex()) -> {
            endPsalmPart()
            val matcher = verseLabelPattern.matcher(line)
            if (matcher.find()) {
              append("<font color='#999999'>")
              append(matcher.group(1))
              append(if (TextUtils.isEmpty(matcher.group(2))) "" else " ${matcher.group(2)}").append("</font><br>")
              append(verses[parsePsalmPartNumber(matcher.group(2))])
            }
          }

          line.matches(chorusLabelPattern.pattern().toRegex()) -> {
            endPsalmPart()
            val matcher = chorusLabelPattern.matcher(line)
            if (matcher.find()) {
              append("<font color='#999999'>")
              append(matcher.group(1))
              append(if (matcher.group(2).isNullOrEmpty()) "" else " " + matcher.group(2))
              append("</font><br>")
              append(choruses[parsePsalmPartNumber(matcher.group(2))])
            }
          }

          else -> {
            psalmPartText.append(line).append("<br>")
            append(line)
            append("<br>")
          }
        }
      }
    }
  }


  private fun parsePsalmPartNumber(str: String?): Int =
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
    private const val PSALM_VERSE_NUMBER_FORMAT = """^\s*(%s)?\s*(\d{1,2})\s*\.\s*$"""
    private const val PSALM_VERSE_LABEL_FORMAT = """^\s*\[(%s)\s*(\d{1,2})\]\s*$"""
    private const val PSALM_CHORUS_NUMBER_FORMAT = """^\s*(%s)\s*(\d{1,2})?\s*\.\s*$"""
    private const val PSALM_CHORUS_LABEL_FORMAT = """^\s*\[(%s)\s*(\d{1,2})?\]\s*$"""
    private fun psalmVerseNumberPattern(locale: Locale?): Pattern =
      Pattern.compile(String.format(PSALM_VERSE_NUMBER_FORMAT, getLocalizedString("lbl_verse", locale)))

    private fun getPsalmVerseLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(PSALM_VERSE_LABEL_FORMAT, getLocalizedString("lbl_verse", locale)))
    }

    private fun getPsalmChorusNumberPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(PSALM_CHORUS_NUMBER_FORMAT, getLocalizedString("lbl_chorus", locale)))
    }

    private fun getPsalmChorusLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(String.format(PSALM_CHORUS_LABEL_FORMAT, getLocalizedString("lbl_chorus", locale)))
    }

    private fun getLocalizedString(stringKey: String, locale: Locale?): String? {
      return getResource(stringKey, locale!!)
    }
  }
}
