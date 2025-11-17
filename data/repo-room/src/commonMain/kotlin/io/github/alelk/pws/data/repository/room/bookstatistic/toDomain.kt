package io.github.alelk.pws.data.repository.room.bookstatistic

import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.domain.bookstatistic.model.BookStatisticDetail

fun BookStatisticEntity.toDomain() = BookStatisticDetail(id = id, priority = priority, readings = readings, rating = rating)