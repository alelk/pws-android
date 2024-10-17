package io.github.alelk.pws.database.common.entity

import androidx.room.*
import java.util.Date

@Entity(
  tableName = "history",
  foreignKeys = [
    ForeignKey(entity = SongNumberEntity::class, parentColumns = ["_id"], childColumns = ["psalmnumberid"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [Index(name = "idx_history_psalmnumberid", value = ["psalmnumberid"])]
)
data class HistoryEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
  @ColumnInfo(name = "psalmnumberid") val songNumberId: Long,
  @ColumnInfo(name = "accesstimestamp") val accessTimestamp: Date
)