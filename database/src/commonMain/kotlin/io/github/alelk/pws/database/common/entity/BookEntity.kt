package io.github.alelk.pws.database.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.alelk.pws.database.common.model.BookExternalId
import io.github.alelk.pws.database.common.model.Person
import io.github.alelk.pws.database.common.model.Version
import io.github.alelk.pws.database.common.model.Year
import java.util.Locale

@Entity(
  tableName = "books",
  indices = [Index(name = "idx_books_edition", value = ["edition"], unique = true)]
)
data class BookEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
  @ColumnInfo(name = "version") val version: Version,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "displayshortname") val displayShortName: String,
  @ColumnInfo(name = "displayname") val displayName: String,
  @ColumnInfo(name = "edition") val externalId: BookExternalId,
  @ColumnInfo(name = "releasedate") val releaseDate: Year? = null,
  @ColumnInfo(name = "authors") val authors: List<Person>? = null,
  @ColumnInfo(name = "creators") val creators: List<Person>? = null,
  @ColumnInfo(name = "reviewers") val reviewers: List<Person>? = null,
  @ColumnInfo(name = "editors") val editors: List<Person>? = null,
  @ColumnInfo(name = "description") val description: String? = null,
  @ColumnInfo(name = "preface") val preface: String? = null
)