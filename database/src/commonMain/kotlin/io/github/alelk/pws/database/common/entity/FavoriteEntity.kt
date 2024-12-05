package io.github.alelk.pws.database.common.entity

import androidx.room.*

@Entity(
  tableName = "favorites",
  foreignKeys = [
    ForeignKey(entity = SongNumberEntity::class, parentColumns = ["_id"], childColumns = ["psalmnumberid"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [
    Index(name = "idx_favorites_psalmnumberid", value = ["psalmnumberid"]),
    Index(name = "idx_favorites_position", value = ["position"])
  ],
)
data class FavoriteEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
  @ColumnInfo(name = "position") val position: Int,
  @ColumnInfo(name = "psalmnumberid", index = true) val songNumberId: Long
)