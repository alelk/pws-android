package io.github.alelk.pws.database.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// todo: rename
interface Pageable1<E> {

  suspend fun getAll(limit: Int, offset: Int = 0): List<E>

  suspend fun getAll(): Flow<E> = flow {
    var offset = 0
    while (true) {
      val next = getAll(GET_ALL_BATCH_SIZE, offset)
      if (next.isEmpty()) break
      else {
        next.forEach { emit(it) }
        offset += next.size
      }
    }
  }

  companion object {
    const val GET_ALL_BATCH_SIZE = 500
  }
}