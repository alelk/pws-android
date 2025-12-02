package io.github.alelk.pws.domain.song.repository

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.result.DeleteResourceResult
import io.github.alelk.pws.domain.core.result.UpdateResourceResult
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.command.UpdateSongCommand

/** Mutation operations for Song aggregate. */
interface SongWriteRepository {
  /** Create a new song. */
  suspend fun create(command: CreateSongCommand): CreateResourceResult<SongId>

  /** Patch/update semantics. */
  suspend fun update(command: UpdateSongCommand): UpdateResourceResult<SongId>

  suspend fun delete(id: SongId): DeleteResourceResult<SongId>
}

