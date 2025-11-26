package io.github.alelk.pws.domain.core.result

sealed interface CreateResourcesResult<out R : Any> {
  data class Success<out R : Any>(val resources: List<R>) : CreateResourcesResult<R>

  data class AlreadyExists<out R : Any>(val resource: R) : CreateResourcesResult<R>

  data class ValidationError<out R : Any>(val resource: R, val message: String) : CreateResourcesResult<R>

  data class UnknownError<out R : Any>(
    val resource: R,
    val exception: Throwable?,
    val message: String = exception?.message ?: exception?.let { "Unknown error: ${it::class.simpleName}" } ?: "Unknown error"
  ) : CreateResourcesResult<R>
}