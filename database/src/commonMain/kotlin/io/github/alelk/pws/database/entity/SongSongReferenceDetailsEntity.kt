package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.BookExternalId

/** Reference between two songs. */
data class SongSongReferenceDetailsEntity(
    val songId: Long,
    val refSongId: Long,
    val refReason: SongRefReason,
    val volume: Int,
    val refSongName: String,
    val refSongNumber: Int,
    val refSongNumberId: Long,
    val refSongNumberBookId: Long,
    val refSongNumberBookExternalId: BookExternalId,
    val refSongNumberBookDisplayName: String,
)