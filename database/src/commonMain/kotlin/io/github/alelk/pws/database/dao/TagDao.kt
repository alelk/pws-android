package io.github.alelk.pws.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alelk.pws.database.entity.SongNumberWithSongWithBookWithFavorite
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.domain.model.TagId
import kotlinx.coroutines.flow.Flow

private const val customTagPrefix = "custom-"

fun TagId.customTagNumber(): Int? = this.toString().takeIf { it.startsWith(customTagPrefix) }?.removePrefix(customTagPrefix)?.dropWhile { it == '0' }?.toInt()
fun TagId.Companion.createCustomTag(number: Int) = parse("$customTagPrefix${number.toString().padStart(5, '0')}")
fun TagEntity.isCustomTag(): Boolean = this.id.customTagNumber() != null

@Dao
interface TagDao : Pageable1<TagEntity> {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tag: TagEntity)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(tags: List<TagEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tag: TagEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tags: List<TagEntity>)

  @Query("SELECT * FROM tags WHERE id = :id")
  suspend fun getById(id: TagId): TagEntity?

  @Query("SELECT * FROM tags WHERE id in (:ids)")
  suspend fun getByIds(ids: List<TagId>): List<TagEntity>

  @Query("SELECT * FROM tags ORDER BY priority, id LIMIT :limit OFFSET :offset")
  override suspend fun getAll(limit: Int, offset: Int): List<TagEntity>

  @Query("SELECT * FROM tags WHERE name = :name ORDER BY priority, id")
  suspend fun getAllByName(name: String): List<TagEntity>

  @Query("SELECT * FROM tags WHERE predefined = 0 ORDER BY priority, id")
  suspend fun getAllNotPredefined(): List<TagEntity>

  @Query(
    """
    SELECT pn.*
    FROM song_number_tags  snt
    INNER JOIN psalmnumbers pn on snt.song_number_id = pn._id
    WHERE snt.tag_id = :tagId
    """
  )
  fun getTagSongsFlow(tagId: TagId): Flow<List<SongNumberWithSongWithBookWithFavorite>>

  suspend fun getLastCustomTag() =
    getAllNotPredefined()
      .filter { it.isCustomTag() }
      .sortedBy { it.id.customTagNumber() }
      .lastOrNull()

  suspend fun getNextCustomTagId(): TagId {
    val lastCustomTagIdNumber = getLastCustomTag()?.id?.customTagNumber()
    return TagId.createCustomTag(lastCustomTagIdNumber?.plus(1) ?: 1)
  }

  @Query("SELECT count(id) FROM tags")
  suspend fun count(): Int

  @Delete
  suspend fun delete(tag: TagEntity)

  @Delete
  suspend fun delete(tags: List<TagEntity>)

  @Query("DELETE FROM tags")
  suspend fun deleteAll()

  // flows

  @Query("SELECT * FROM tags ORDER BY priority, id")
  fun getAllFlow(): Flow<List<TagEntity>>
}