package io.github.alelk.pws.domain.core

import io.github.alelk.pws.domain.core.OptionalField.Clear
import io.github.alelk.pws.domain.core.OptionalField.Unchanged

/** Optional mutation semantic: Unchanged / Set(value) / Clear. */
sealed interface OptionalField<out T> {
  data object Unchanged : OptionalField<Nothing>
  data class Set<T>(val value: T) : OptionalField<T>
  data object Clear : OptionalField<Nothing>

  fun forEach(block: (value: T?) -> Unit) =
    when (this) {
      Unchanged -> {}
      is Set -> block(value)
      Clear -> block(null)
    }


  companion object {
    fun <T> fromNullable(nullable: T?, treatNullAsClear: Boolean = false): OptionalField<T> =
      if (nullable == null) if (treatNullAsClear) Clear else Unchanged else Set(nullable)
  }
}

inline fun <R, T : R> OptionalField<T>.getOrElse(defaultValue: () -> R): R = when (this) {
  Unchanged -> defaultValue()
  is OptionalField.Set -> value
  Clear -> defaultValue()
}

