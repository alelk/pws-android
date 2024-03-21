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
import android.util.SparseArray
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
  private val verseNumberPattern: Pattern = psalmVerseNumberPattern
  private val verseLabelPattern: Pattern = getPsalmVerseLabelPattern(locale)
  private val chorusNumberPattern: Pattern = getPsalmChorusNumberPattern(locale)
  private val chorusLabelPattern: Pattern = getPsalmChorusLabelPattern(locale)
  private var psalmPartType: String? = null
  private var psalmPartNumber = 0
  private var psalmPartText = StringBuilder()
  private val choruses = SparseArray<String>()
  private val verses = SparseArray<String>()

  fun forLocale(locale: Locale?): PwsPsalmHtmlBuilder {
    return if (this.locale != null && this.locale == locale || locale != null && locale == this.locale) this else PwsPsalmHtmlBuilder(
      locale
    )
  }

  fun buildHtml(psalmText: String, isExpanded: Boolean): String {
    return if (isExpanded) buildExpandedHtml(psalmText) else buildHtml(psalmText)
  }

  private fun buildExpandedHtml(psalmText: String): String {
    psalmPartType = null
    psalmPartNumber = 0
    psalmPartText = StringBuilder()
    choruses.clear()
    verses.clear()
    val html = StringBuilder()
    val tokenizer = StringTokenizer(psalmText, "\n")
    while (tokenizer.hasMoreTokens()) {
      val line = tokenizer.nextToken()
      if (line.matches(verseNumberPattern.pattern().toRegex())) {
        endPsalmPart()
        val matcher = verseNumberPattern.matcher(line)
        if (matcher.find()) startPsalmPart("verse", matcher.group(1))
        html.append("<font color='#7aaf83'>").append(line.replace('.', ' '))
          .append("</font><br>")
      } else if (line.matches(chorusNumberPattern.pattern().toRegex())) {
        endPsalmPart()
        val matcher = chorusNumberPattern.matcher(line)
        if (matcher.find()) startPsalmPart("chorus", matcher.group(2))
        html.append("<font color='#7aaf83'>").append(line.replace('.', ' '))
          .append("</font><br>")
      } else if (line.matches(verseLabelPattern.pattern().toRegex())) {
        endPsalmPart()
        val matcher = verseLabelPattern.matcher(line)
        if (matcher.find()) {
          html.append("<font color='#999999'>").append(matcher.group(1))
            .append(
              if (TextUtils.isEmpty(matcher.group(2))) "" else " " + matcher.group(
                2
              )
            ).append("</font><br>")
          html.append(verses[parsePsalmPartNumber(matcher.group(2))])
        }
      } else if (line.matches(chorusLabelPattern.pattern().toRegex())) {
        endPsalmPart()
        val matcher = chorusLabelPattern.matcher(line)
        if (matcher.find()) {
          html.append("<font color='#999999'>").append(matcher.group(1))
            .append(
              if (TextUtils.isEmpty(matcher.group(2))) "" else " " + matcher.group(
                2
              )
            ).append("</font><br>")
          html.append(choruses[parsePsalmPartNumber(matcher.group(2))])
        }
      } else {
        psalmPartText.append(line).append("<br>")
        html.append(line).append("<br>")
      }
    }
    return html.toString()
  }

  private fun startPsalmPart(psalmPartType: String, psalmPartNumber: String) {
    this.psalmPartType = psalmPartType
    this.psalmPartNumber = parsePsalmPartNumber(psalmPartNumber)
    psalmPartText = StringBuilder()
  }

  private fun parsePsalmPartNumber(str: String?): Int =
    if (str == null) 1
    else try {
      str.toInt()
    } catch (exc: NumberFormatException) {
      1
    }

  private fun endPsalmPart() {
    if (psalmPartType == null) return
    if ("verse" == psalmPartType) verses.put(psalmPartNumber, psalmPartText.toString())
    if ("chorus" == psalmPartType) choruses.put(psalmPartNumber, psalmPartText.toString())
    psalmPartType = null
  }

  private fun buildHtml(psalmText: String): String {
    val tokenizer = StringTokenizer(psalmText, "\n")
    val html = StringBuilder()
    while (tokenizer.hasMoreTokens()) {
      val line = tokenizer.nextToken()
      if (line.matches(verseLabelPattern.pattern().toRegex()) || line.matches(
          chorusLabelPattern.pattern().toRegex()
        )
      ) {
        html.append("<font color='#888888'><i>").append(line).append("</i></font><br>")
      } else if (line.matches(verseNumberPattern.pattern().toRegex()) || line.matches(
          chorusNumberPattern.pattern().toRegex()
        )
      ) {
        html.append("<font color='#7aaf83'>").append(line.replace('.', ' '))
          .append("</font><br>")
      } else {
        html.append(line).append("<br>")
      }
    }
    return html.toString()
  }

  companion object {
    private const val PSALM_VERSE_NUMBER_REGEX = "^\\s*+(\\d{1,2})\\.\\s*+$"
    private const val PSALM_VERSE_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})\\]\\s*+$"
    private const val PSALM_CHORUS_NUMBER_FORMAT = "^\\s*+(%s)\\s*+(\\d{1,2})??:\\s*+$"
    private const val PSALM_CHORUS_LABEL_FORMAT = "^\\s*+\\[(%s)\\s*+(\\d{1,2})??\\]\\s*+$"
    private val psalmVerseNumberPattern: Pattern
      get() = Pattern.compile(PSALM_VERSE_NUMBER_REGEX)

    private fun getPsalmVerseLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(
        String.format(
          PSALM_VERSE_LABEL_FORMAT,
          getLocalizedString("lbl_verse", locale)
        )
      )
    }

    private fun getPsalmChorusNumberPattern(locale: Locale?): Pattern {
      return Pattern.compile(
        String.format(
          PSALM_CHORUS_NUMBER_FORMAT,
          getLocalizedString("lbl_chorus", locale)
        )
      )
    }

    private fun getPsalmChorusLabelPattern(locale: Locale?): Pattern {
      return Pattern.compile(
        String.format(
          PSALM_CHORUS_LABEL_FORMAT,
          getLocalizedString("lbl_chorus", locale)
        )
      )
    }

    private fun getLocalizedString(stringKey: String, locale: Locale?): String? {
      return getResource(stringKey, locale!!)
    }
  }
}
