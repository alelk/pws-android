package com.alelk.pws.database.support.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntityV120(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
    @ColumnInfo(name = "psalmnumberid", index = true) val songNumberId: Long,
    @ColumnInfo(name = "accesstimestamp") val accessTimestamp: String
)