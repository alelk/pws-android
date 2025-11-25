package io.github.alelk.pws.domain.song.repository

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.command.CreateSongResult
import io.github.alelk.pws.domain.song.command.UpdateSongCommand
import io.github.alelk.pws.domain.song.command.UpdateSongResult

/** Mutation operations for Song aggregate. */
interface SongWriteRepository {
  /** Create a new song. */
  suspend fun create(command: CreateSongCommand): CreateSongResult

  /** Patch/update semantics. */
  suspend fun update(command: UpdateSongCommand): UpdateSongResult

  /** Delete song by id; returns true if existed. */
  suspend fun delete(id: SongId): Boolean
}

