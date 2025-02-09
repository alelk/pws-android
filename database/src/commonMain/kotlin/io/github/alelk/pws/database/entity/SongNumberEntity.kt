package io.github.alelk.pws.database.entity

import androidx.room.*

/** Number of song in book.
 *
 * Unique constraints:
 * - Pair (songId, bookId) - book cannot contain the same song twice.
 * - Pair (number, bookId) - book contain numbered list of songs and song number is unique in book
 */
@Entity(
  tableName = "psalmnumbers",
  foreignKeys = [
    ForeignKey(entity = SongEntity::class, parentColumns = ["_id"], childColumns = ["psalmid"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = BookEntity::class, parentColumns = ["_id"], childColumns = ["bookid"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [
    Index(name = "idx_psalmnumbers_psalmid", value = ["psalmid"]),
    Index(name = "idx_psalmnumbers_bookid", value = ["bookid"]),
    Index(name = "idx_psalmnumbers_psalmid_bookid", value = ["psalmid", "bookid"], unique = true),
    Index(name = "idx_psalmnumbers_number_bookid", value = ["number", "bookid"], unique = true),
  ]
)
data class SongNumberEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
  @ColumnInfo(name = "number") val number: Int,
  @ColumnInfo(name = "psalmid") val songId: Long,
  @ColumnInfo(name = "bookid") val bookId: Long,
  @ColumnInfo(name = "priority") val priority: Int,
)