package io.github.alelk.pws.domain.song.lyric

import io.github.alelk.pws.domain.core.Locale
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string

// Mapping of supported locales to codepoints used when generating lyric text.
private val lyricCodepointByLocale: Map<Locale, Arb<Codepoint>> = mapOf(
  Locale.RU to Codepoint.cyrillic(),
  Locale.UK to Codepoint.cyrillic(),
  Locale.EN to Codepoint.az(),
)

fun Arb.Companion.lyricLocale(): Arb<Locale> = Arb.of(lyricCodepointByLocale.keys)

/** Generates a single lyric line (one part line) consisting of several words. */
fun Arb.Companion.lyricPartLine(
  countWords: IntRange = 4..5,
  locale: Locale = lyricCodepointByLocale.keys.first()
): Arb<String> {
  val codepoint = lyricCodepointByLocale[locale]
    ?: error("Locale $locale not supported. Expected one of ${lyricCodepointByLocale.keys.joinToString()}")
  return Arb.list(Arb.string(2..10, codepoint), countWords).map { words ->
    words.joinToString(separator = " ")
      .lowercase()
      .replaceFirstChar { c -> c.titlecase() }
  }
}

/** Generates text for a lyric part (multi-line). */
fun Arb.Companion.lyricPartText(
  countLines: IntRange = 4..4,
  locale: Locale = Locale.RU,
  lyricPartLine: Arb<String> = Arb.lyricPartLine(locale = locale)
): Arb<String> = Arb.list(lyricPartLine, countLines).map { it.joinToString(separator = "\n") }

enum class LyricPartType { VERSE, CHORUS, BRIDGE }

/** Public arbitrary: generates a Lyric together with its Locale (Locale chosen from provided Arb). */
fun Arb.Companion.lyric(
  countUniqueParts: IntRange = 1..4,
  totalPartsCount: IntRange = 1..10,
  locale: Arb<Locale> = Arb.lyricLocale()
): Arb<Pair<Lyric, Locale>> = arbitrary {
  Arb.lyric(countUniqueParts, totalPartsCount, locale = locale.bind()).bind()
}

/** Internal builder: generates a Lyric for a specific Locale. */
fun Arb.Companion.lyric(
  countUniqueParts: IntRange = 1..4,
  totalPartsCount: IntRange = 1..10,
  locale: Locale = Locale.RU,
  lyricPartText: Arb<String> = Arb.lyricPartText(locale = locale),
  lyricPartTypes: Arb<LyricPartType> = Arb.enum<LyricPartType>()
): Arb<Pair<Lyric, Locale>> = arbitrary {
  // Arbitrary generating a Pair(text, type) for a lyric part definition.
  val lyricPartArb = arbitrary { lyricPartText.bind() to lyricPartTypes.bind() }
  // Unique parts ensures we have distinct (text, type) combinations to sample from.
  val uniqueLyricParts = Arb.list(lyricPartArb, countUniqueParts).bind()
  val lyricPartsArb = Arb.list(Arb.of(uniqueLyricParts), totalPartsCount).map { sampled ->
    sampled
      .mapIndexed { index, (text, type) -> Triple(index + 1, text, type) }
      .groupBy { triple -> triple.second to triple.third }
      .mapValues { (key, value) ->
        val (text, type) = key
        val partNumbers = value.map { v -> v.first }.toSet()
        when (type) {
          LyricPartType.VERSE -> Verse(partNumbers, text)
          LyricPartType.CHORUS -> Chorus(partNumbers, text)
          LyricPartType.BRIDGE -> Bridge(partNumbers, text)
        }
      }
      .values
      .toList()
      .sortedBy { p -> p.numbers.min() }
  }
  val lyric = lyricPartsArb.bind()
  Lyric(lyric) to locale
}

// Fallback extension for Cyrillic codepoints if kotest doesn't provide Codepoint.cyrillic().
// Uses Russian/ Ukrainian basic Cyrillic ranges plus Yo/YO.
private fun Codepoint.Companion.cyrillic(): Arb<Codepoint> =
  Arb.of((('А'..'Я').map { Codepoint(it.code) } + ('а'..'я').map { Codepoint(it.code) } + listOf('ё', 'Ё').map { Codepoint(it.code) }))

