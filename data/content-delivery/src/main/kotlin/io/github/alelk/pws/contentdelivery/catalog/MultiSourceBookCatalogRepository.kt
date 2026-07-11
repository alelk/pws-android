package io.github.alelk.pws.contentdelivery.catalog

import arrow.core.Either
import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.model.ContentSource
import io.github.alelk.pws.domain.booklibrary.repository.BookCatalogRepository
import io.github.alelk.pws.domain.core.error.ReadError
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import timber.log.Timber

private const val MULTI_SOURCE_ROUNDS = 3
private const val MULTI_SOURCE_BACKOFF_MS = 5_000L

/**
 * Aggregates several catalog sources for resilience.
 *
 * Each source is tried **once per round** in priority order, so a dead primary fails over to the
 * next source immediately instead of exhausting its own retries first (which could block for
 * minutes). Only when every source fails in a round does it back off and retry the whole set.
 *
 * A source returning an older `bundleVersion` than what is installed is not filtered here — downgrade
 * protection lives in `BookLibraryItem.hasUpdate`, which only flags an update when the catalog
 * version is strictly newer than the installed one.
 */
class MultiSourceBookCatalogRepository(
    sources: List<ContentSource>,
    bundleVariant: String,
    httpClient: HttpClient,
    private val rounds: Int = MULTI_SOURCE_ROUNDS,
    private val backoffMs: Long = MULTI_SOURCE_BACKOFF_MS,
) : BookCatalogRepository {

    // One attempt per source — retries are orchestrated here, across sources, not inside each one.
    private val repositories: List<BookCatalogRepositoryImpl> = sources
        .sortedBy { it.priority }
        .map { source -> BookCatalogRepositoryImpl(source.catalogUrl, bundleVariant, httpClient, maxAttempts = 1) }

    override suspend fun getAvailableBooks(): Either<ReadError, List<BookCatalogEntry>> {
        if (repositories.isEmpty()) return Either.Left(ReadError.UnknownError(Exception("no content sources configured")))

        var lastError: ReadError = ReadError.UnknownError(message = "all content sources failed")
        repeat(rounds) { round ->
            if (round > 0) delay(backoffMs * round)
            for (repo in repositories) {
                when (val result = repo.getAvailableBooks()) {
                    is Either.Right -> return result
                    is Either.Left -> lastError = result.value
                }
            }
            Timber.w("All catalog sources failed (round ${round + 1}/$rounds)")
        }
        return Either.Left(lastError)
    }
}
