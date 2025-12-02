package io.github.alelk.pws.domain.core.result

sealed interface UpdateResourceResult<out R : Any> {
  data class Success<out R : Any>(val resource: R) : UpdateResourceResult<R>

  data class NotFound<out R : Any>(val resource: R) : UpdateResourceResult<R>

  data class ValidationError<out R : Any>(val resource: R, val message: String) : UpdateResourceResult<R>

  data class UnknownError<out R : Any>(
    val resource: R,
    val exception: Throwable?,
    val message: String = exception?.message ?: exception?.let { "Unknown error: ${it::class.simpleName}" } ?: "Unknown error"
  ) : UpdateResourceResult<R>
}