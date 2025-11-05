package io.github.alelk.pws.domain.core

/** Optional mutation semantic: Unchanged / Set(value) / Clear. */
sealed interface OptionalField<out T> {
  data object Unchanged : OptionalField<Nothing>
  data class Set<T>(val value: T) : OptionalField<T>
  data object Clear : OptionalField<Nothing>

  companion object {
    fun <T> fromNullable(nullable: T?, treatNullAsClear: Boolean = false): OptionalField<T> =
      if (nullable == null) if (treatNullAsClear) Clear else Unchanged else Set(nullable)
  }
}

