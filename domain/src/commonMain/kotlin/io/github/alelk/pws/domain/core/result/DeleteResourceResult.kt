package io.github.alelk.pws.domain.core.result

sealed interface DeleteResourceResult<out R : Any> {
  data class Success<out R : Any>(val resource: R) : DeleteResourceResult<R>

  data class NotFound<out R : Any>(val resource: R) : DeleteResourceResult<R>

  data class ValidationError<out R : Any>(val resource: R, val message: String) : DeleteResourceResult<R>

  data class UnknownError<out R : Any>(
    val resource: R,
    val exception: Throwable?,
    val message: String = exception?.message ?: exception?.let { "Unknown error: ${it::class.simpleName}" } ?: "Unknown error"
  ) : DeleteResourceResult<R>
}