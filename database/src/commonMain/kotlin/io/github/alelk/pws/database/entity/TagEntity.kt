package io.github.alelk.pws.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.ids.TagId

@Entity(
  tableName = "tags",
  indices = [
    Index(name = "idx_tags_priority", value = ["priority"]),
    Index(value = ["name"], unique = true)
  ]
)
data class TagEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: TagId,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int,
    @ColumnInfo(name = "color") val color: Color,
    @ColumnInfo(name = "predefined", defaultValue = "false") val predefined: Boolean = true
) {
  init {
    require(name.isNotBlank()) { "tag name must not be blank" }
  }
}