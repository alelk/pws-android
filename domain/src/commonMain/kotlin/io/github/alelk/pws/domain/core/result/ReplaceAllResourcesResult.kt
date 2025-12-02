package io.github.alelk.pws.domain.core.result

sealed interface ReplaceAllResourcesResult<out R : Any> {
  data class Success<out R : Any>(val created: List<R>, val updated: List<R>, val unchanged: List<R>, val deleted: List<R>) : ReplaceAllResourcesResult<R>

  data class ValidationError<out R : Any>(val resource: R, val message: String) : ReplaceAllResourcesResult<R>

  data class UnknownError<out R : Any>(
    val resource: R,
    val exception: Throwable?,
    val message: String = exception?.message ?: exception?.let { "Unknown error: ${it::class.simpleName}" } ?: "Unknown error"
  ) : ReplaceAllResourcesResult<R>
}