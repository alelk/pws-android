package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.core.SongNumber
import kotlinx.datetime.LocalDateTime

data class HistoryItem(val songNumber: SongNumber, val timestamp: LocalDateTime)