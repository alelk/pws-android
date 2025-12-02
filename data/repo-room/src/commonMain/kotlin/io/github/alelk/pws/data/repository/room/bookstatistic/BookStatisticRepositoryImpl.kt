package io.github.alelk.pws.data.repository.room.bookstatistic

import io.github.alelk.pws.database.bookstatistic.BookStatisticDao
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.domain.bookstatistic.command.UpdateBookStatisticCommand
import io.github.alelk.pws.domain.bookstatistic.model.BookStatisticDetail
import io.github.alelk.pws.domain.bookstatistic.query.BookStatisticQuery
import io.github.alelk.pws.domain.bookstatistic.repository.BookStatisticRepository
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BookStatisticRepositoryImpl(
  private val dao: BookStatisticDao,
) : BookStatisticRepository {

  override fun observe(id: BookId): Flow<BookStatisticDetail?> =
    dao.observeById(id).map { it?.toDomain() }.distinctUntilChanged()

  override fun observeMany(query: BookStatisticQuery): Flow<List<BookStatisticDetail>> =
    dao
      .observeAll(minPriority = query.minPriority, maxPriority = query.maxPriority)
      .map { list -> list.map { it.toDomain() } }
      .distinctUntilChanged()

  override suspend fun get(id: BookId): BookStatisticDetail? = dao.getById(id)?.toDomain()

  override suspend fun update(command: UpdateBookStatisticCommand): BookStatisticDetail {
    val existing = dao.getById(command.id)
    val merged = BookStatisticEntity(
      id = command.id,
      priority = command.priority ?: existing?.priority,
      readings = command.readings ?: existing?.readings,
      rating = command.rating ?: existing?.rating,
    )
    dao.upsert(merged)
    return merged.toDomain()
  }
}

