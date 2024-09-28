package com.alelk.pws.database.support.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Year
import java.util.Locale

@Entity(tableName = "books")
data class BookEntityV120(
  @PrimaryKey @ColumnInfo(name = "_id") val id: Long,
  @ColumnInfo(name = "version") val version: String,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "displayshortname") val displayShortName: String,
  @ColumnInfo(name = "displayname") val displayName: String,
  @ColumnInfo(name = "edition") val externalId: String,
  @ColumnInfo(name = "releasedate") val releaseDate: Year? = null,
  @ColumnInfo(name = "authors") val authors: List<String>? = null,
  @ColumnInfo(name = "creators") val creators: List<String>? = null,
  @ColumnInfo(name = "reviewers") val reviewers: List<String>? = null,
  @ColumnInfo(name = "editors") val editors: List<String>? = null,
  @ColumnInfo(name = "description") val description: String? = null,
  @ColumnInfo(name = "preface") val preface: String? = null
)