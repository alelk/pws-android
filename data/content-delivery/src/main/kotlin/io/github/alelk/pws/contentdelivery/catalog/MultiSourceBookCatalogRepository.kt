package io.github.alelk.pws.contentdelivery.catalog

import arrow.core.Either
import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.model.ContentSource
import io.github.alelk.pws.domain.booklibrary.repository.BookCatalogRepository
import io.github.alelk.pws.domain.core.error.ReadError
import io.ktor.client.HttpClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MULTI_SOURCE_ROUNDS = 3
private const val MULTI_SOURCE_BACKOFF_MS = 5_000L

/**
 * Aggregates several catalog sources for resilience, remembering which one to trust.
 *
 * **First launch** (no remembered source): all sources are queried *in parallel* and whichever
 * answers first wins — the fastest mirror is picked and persisted via [preferredSourceStore].
 *
 * **Subsequent launches**: the remembered source is tried first, then the remaining sources in
 * priority order, one at a time. Each source gets one attempt per round, so a dead source fails
 * over immediately instead of exhausting its own retries first (which could block for minutes).
 * Whenever a *different* source ends up serving the catalog, that new choice is persisted.
 *
 * Only when every source fails in a round does it back off and retry the whole set.
 *
 * A source returning an older `bundleVersion` than what is installed is not filtered here — downgrade
 * protection lives in `BookLibraryItem.hasUpdate`, which only flags an update when the catalog
 * version is strictly newer than the installed one.
 */
class MultiSourceBookCatalogRepository(
    sources: List<ContentSource>,
    bundleVariant: String,
    httpClient: HttpClient,
    private val preferredSourceStore: PreferredCatalogSourceStore = PreferredCatalogSourceStore.NoOp,
    private val rounds: Int = MULTI_SOURCE_ROUNDS,
    private val backoffMs: Long = MULTI_SOURCE_BACKOFF_MS,
) : BookCatalogRepository {

    private data class Source(val url: String, val repository: BookCatalogRepositoryImpl)

    // One attempt per source — retries are orchestrated here, across sources, not inside each one.
    private val sources: List<Source> = sources
        .sortedBy { it.priority }
        .map { Source(it.catalogUrl, BookCatalogRepositoryImpl(it.catalogUrl, bundleVariant, httpClient, maxAttempts = 1)) }

    override suspend fun getAvailableBooks(): Either<ReadError, List<BookCatalogEntry>> {
        if (sources.isEmpty()) return Either.Left(ReadError.UnknownError(Exception("no content sources configured")))

        val preferred = sources.firstOrNull { it.url == preferredSourceStore.get() }
        return if (preferred != null) fetchSequential(preferred) else fetchFastest()
    }

    /** First launch: race every source in parallel and keep whichever answers first. */
    private suspend fun fetchFastest(): Either<ReadError, List<BookCatalogEntry>> {
        var lastError: ReadError = ReadError.UnknownError(message = "all content sources failed")
        repeat(rounds) { round ->
            if (round > 0) delay(backoffMs * round)
            when (val outcome = raceRound()) {
                is RaceOutcome.Success -> {
                    preferredSourceStore.set(outcome.url)
                    Timber.i("Selected catalog source ${outcome.url} (fastest of ${sources.size})")
                    return Either.Right(outcome.books)
                }
                is RaceOutcome.AllFailed -> lastError = outcome.error
            }
            Timber.w("All catalog sources failed (round ${round + 1}/$rounds)")
        }
        return Either.Left(lastError)
    }

    private suspend fun raceRound(): RaceOutcome = coroutineScope {
        val results = Channel<Pair<String, Either<ReadError, List<BookCatalogEntry>>>>(capacity = sources.size)
        val jobs = sources.map { source ->
            launch { results.send(source.url to source.repository.getAvailableBooks()) }
        }
        var lastError: ReadError = ReadError.UnknownError(message = "all content sources failed")
        try {
            repeat(sources.size) {
                val (url, result) = results.receive()
                when (result) {
                    is Either.Right -> return@coroutineScope RaceOutcome.Success(url, result.value)
                    is Either.Left -> lastError = result.value
                }
            }
            RaceOutcome.AllFailed(lastError)
        } finally {
            jobs.forEach { it.cancel() }
        }
    }

    /** Try the remembered source first, then the rest in priority order; remember whoever succeeds. */
    private suspend fun fetchSequential(preferred: Source): Either<ReadError, List<BookCatalogEntry>> {
        val ordered = listOf(preferred) + sources.filter { it.url != preferred.url }
        var lastError: ReadError = ReadError.UnknownError(message = "all content sources failed")
        repeat(rounds) { round ->
            if (round > 0) delay(backoffMs * round)
            for (source in ordered) {
                when (val result = source.repository.getAvailableBooks()) {
                    is Either.Right -> {
                        if (source.url != preferred.url) {
                            preferredSourceStore.set(source.url)
                            Timber.i("Catalog source failed over to ${source.url}")
                        }
                        return result
                    }
                    is Either.Left -> lastError = result.value
                }
            }
            Timber.w("All catalog sources failed (round ${round + 1}/$rounds)")
        }
        return Either.Left(lastError)
    }

    private sealed interface RaceOutcome {
        data class Success(val url: String, val books: List<BookCatalogEntry>) : RaceOutcome
        data class AllFailed(val error: ReadError) : RaceOutcome
    }
}
