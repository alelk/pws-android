package io.github.alelk.pws.database.common.entity.converter

import androidx.room.TypeConverter
import io.github.alelk.pws.database.common.entity.SongRefReason
import io.github.alelk.pws.domain.model.BookExternalId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year
import io.github.alelk.pws.domain.model.toTagId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val ENTITY_STRING_ARRAY_DELIMITER = ';'
const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

class DbTypeConverters {

  @TypeConverter
  fun versionToString(version: Version): String = version.toString()

  @TypeConverter
  fun parseVersion(version: String): Version = Version.fromString(version)

  @TypeConverter
  fun localeToString(locale: Locale): String = locale.toString()

  @TypeConverter
  fun parseLocale(locale: String): Locale = Locale.forLanguageTag(locale)

  @TypeConverter
  fun bookExternalIdToString(id: BookExternalId): String = id.toString()

  @TypeConverter
  fun parseBookExternalId(id: String): BookExternalId = BookExternalId.parse(id)

  @TypeConverter
  fun personListToString(list: List<Person>): String = list.joinToString(ENTITY_STRING_ARRAY_DELIMITER.toString()) { it.name }

  @TypeConverter
  fun personListFromString(string: String): List<Person> =
    kotlin
      .runCatching {
        if (string.isBlank()) emptyList()
        else string.split(ENTITY_STRING_ARRAY_DELIMITER).map(::Person)
      }
      .recover { e ->
        throw IllegalArgumentException(
          "unable to parse person list from string '$string': expected delimiter '$ENTITY_STRING_ARRAY_DELIMITER': ${e.message}",
          e
        )
      }
      .getOrThrow()

  @TypeConverter
  fun personToString(person: Person): String = person.name

  @TypeConverter
  fun stringToPerson(string: String): Person = Person(string)

  @TypeConverter
  fun tonalityListToString(list: List<Tonality>): String = list.joinToString(ENTITY_STRING_ARRAY_DELIMITER.toString()) { it.identifier }

  @TypeConverter
  fun tonalityListFromString(string: String): List<Tonality> =
    if (string.isBlank()) emptyList()
    else string.split(ENTITY_STRING_ARRAY_DELIMITER).map(Tonality::fromIdentifier)

  @TypeConverter
  fun tonalityToString(tonality: Tonality): String = tonality.identifier

  @TypeConverter
  fun tonalityFromString(tonality: String): Tonality = Tonality.fromIdentifier(tonality)

  @TypeConverter
  fun songRefReasonToString(reason: SongRefReason): String = reason.identifier

  @TypeConverter
  fun stringToSongRefReason(reason: String): SongRefReason = SongRefReason.fromIdentifier(reason)

  @TypeConverter
  fun tagIdToString(id: TagId): String = id.toString()

  @TypeConverter
  fun parseTagId(id: String): TagId = id.toTagId()

  @TypeConverter
  fun colorToString(color: Color): String = color.toString()

  @TypeConverter
  fun parseColor(hexColor: String): Color = Color.parse(hexColor)

  @TypeConverter
  fun yearToString(year: Year): String = year.toString()

  @TypeConverter
  fun parseYear(year: String): Year = Year.parse(year)

  val df = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US)

  @TypeConverter
  fun dateToString(date: Date): String = df.format(date)

  @TypeConverter
  fun parseDate(date: String): Date = checkNotNull(df.parse(date))
}