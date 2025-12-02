package io.github.alelk.pws.domain.bookstatistic.usecase

import io.github.alelk.pws.domain.bookstatistic.model.BookStatisticDetail
import io.github.alelk.pws.domain.bookstatistic.repository.BookStatisticRepository
import io.github.alelk.pws.domain.bookstatistic.command.UpdateBookStatisticCommand

class UpdateBookStatisticUseCase(private val repository: BookStatisticRepository) {
  suspend operator fun invoke(command: UpdateBookStatisticCommand): BookStatisticDetail = repository.update(command)
}

