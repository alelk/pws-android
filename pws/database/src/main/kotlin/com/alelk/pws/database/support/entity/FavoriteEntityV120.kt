package com.alelk.pws.database.support.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntityV120(
    @PrimaryKey @ColumnInfo(name = "_id") val id: Long,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "psalmnumberid") val songNumberId: Long
)