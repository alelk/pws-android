package io.github.alelk.pws.domain.song.repository

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.command.UpdateSongCommand

/**
 * Mutation operations for Song aggregate.
 * All methods return Boolean when the underlying entity existed and was modified, unless noted otherwise.
 */
interface SongWriteRepository {
  /** Create a new song. Throws if id already exists or preconditions fail. */
  suspend fun create(command: CreateSongCommand)

  /** Patch/update semantics; returns true if applied, false if song not found or version check failed. */
  suspend fun update(command: UpdateSongCommand): Boolean

  /** Delete song by id; returns true if existed. */
  suspend fun delete(id: SongId): Boolean
}

