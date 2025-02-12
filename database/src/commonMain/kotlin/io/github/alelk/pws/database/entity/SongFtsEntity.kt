package io.github.alelk.pws.database.entity

import androidx.room.*

@Fts4(contentEntity = SongEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61, tokenizerArgs = ["remove_diacritics=1"])
@Entity(tableName = "songs_fts")
data class SongFtsEntity(
  @PrimaryKey @ColumnInfo(name = "rowid") val id: Long,
  @ColumnInfo(name = "name") val name: String?,
  @ColumnInfo(name = "author") val author: String?,
  @ColumnInfo(name = "translator") val translator: String?,
  @ColumnInfo(name = "composer") val composer: String?,
  @ColumnInfo(name = "bibleref") val bibleRef: String?,
  @ColumnInfo(name = "lyric") val lyric: String
)