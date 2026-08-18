package io.github.alelk.pws.android.compose.payment

import ru.rustore.sdk.core.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Bridges a RuStore SDK [Task] into a coroutine. */
suspend fun <T> Task<T>.coAwait(): T =
  suspendCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
  }
