package io.github.alelk.pws.contentdelivery.catalog

import arrow.core.Either
import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.repository.BookCatalogRepository
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.error.ReadError
import io.github.alelk.pws.portable.serialization.CatalogSerializer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import timber.log.Timber

private const val CATALOG_TIMEOUT_MS = 60_000L
private const val CATALOG_MAX_ATTEMPTS = 3

class BookCatalogRepositoryImpl(
    private val catalogUrl: String,
    private val bundleVariant: String,
    private val httpClient: HttpClient,
    private val maxAttempts: Int = CATALOG_MAX_ATTEMPTS,
) : BookCatalogRepository {

    override suspend fun getAvailableBooks(): Either<ReadError, List<BookCatalogEntry>> {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) delay(5_000L * attempt)
            runCatching {
                Timber.d("Fetching catalog from $catalogUrl (attempt ${attempt + 1}/$maxAttempts)")
                val json = withTimeout(CATALOG_TIMEOUT_MS) {
                    httpClient.get(catalogUrl) {
                        header(HttpHeaders.UserAgent, "pws-android/1.0 (github.com/alelk/pws-android)")
                        header(HttpHeaders.Accept, "application/json")
                        header(HttpHeaders.CacheControl, "no-cache")
                    }.bodyAsText()
                }
                val catalog = CatalogSerializer.decode(json)
                Timber.d("Catalog version ${catalog.version}, ${catalog.books.size} books")
                catalog.books.map { portableEntry ->
                    val bookId = portableEntry.book.id
                    BookCatalogEntry(
                        bookId = bookId,
                        locales = portableEntry.book.locales.map { Locale.of(it.toString()) },
                        name = portableEntry.book.name,
                        displayName = portableEntry.book.displayName,
                        bundleVersion = portableEntry.book.version,
                        downloadUrl = buildDownloadUrl(bookId.toString(), catalog.version),
                        fileSizeBytes = portableEntry.fileSizeBytes,
                        checksum = portableEntry.checksum,
                        songCount = portableEntry.songCount,
                    )
                }
            }.fold(
                onSuccess = { return Either.Right(it) },
                onFailure = { e ->
                    lastError = e
                    Timber.w(e, "Catalog fetch attempt ${attempt + 1} failed")
                },
            )
        }
        Timber.e(lastError, "Failed to load catalog after $maxAttempts attempts")
        return Either.Left(ReadError.UnknownError(lastError ?: Exception("unknown")))
    }

    // Bundle files are published under names like `{bookId}-{variant}-{version}.book.yaml.gz.enc`
    // alongside the catalog. The catalog itself contains no `downloadUrl` field.
    private fun buildDownloadUrl(bookId: String, version: String): String {
        val baseUrl = catalogUrl.substringBeforeLast('/')
        return "$baseUrl/$bookId-$bundleVariant-$version.book.yaml.gz.enc"
    }
}
