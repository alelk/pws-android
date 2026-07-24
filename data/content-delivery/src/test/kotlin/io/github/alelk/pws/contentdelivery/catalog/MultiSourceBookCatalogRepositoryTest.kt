package io.github.alelk.pws.contentdelivery.catalog

import arrow.core.Either
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.portableBook
import io.github.alelk.pws.domain.booklibrary.model.ContentSource
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.portable.model.BookCatalog
import io.github.alelk.pws.portable.serialization.CatalogSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import io.github.alelk.pws.portable.model.BookCatalogEntry as PortableEntry

@RobolectricTest(sdk = 34)
class MultiSourceBookCatalogRepositoryTest : FeatureSpec({

  val primaryUrl = "https://primary.test/catalog/index.json"
  val secondaryUrl = "https://secondary.test/catalog/index.json"

  fun catalogJson(bookId: String) = CatalogSerializer.encode(
    BookCatalog(
      version = "2026.1",
      books = listOf(
        PortableEntry(
          book = Arb.portableBook(
            id = Arb.constant(BookId.parse(bookId)),
            name = Arb.constant(bookId),
            version = Arb.constant(Version(1, 0)),
          ).next(RandomSource.seeded(20260621L)),
          songCount = 1,
          fileSizeBytes = 10,
          checksum = "sum",
        )
      ),
    )
  )

  fun sources() = listOf(
    ContentSource(name = "primary", catalogUrl = primaryUrl, priority = 0),
    ContentSource(name = "secondary", catalogUrl = secondaryUrl, priority = 1),
  )

  fun isPrimary(url: Any?) = url.toString().startsWith("https://primary.test")

  class InMemorySourceStore(var value: String? = null) : PreferredCatalogSourceStore {
    override fun get(): String? = value
    override fun set(catalogUrl: String) { value = catalogUrl }
  }

  feature("first launch — parallel race") {
    scenario("picks the fastest source and remembers it") {
      runBlocking {
        val engine = MockEngine { request ->
          // secondary is deliberately slow so the race is deterministic — primary wins
          if (!isPrimary(request.url)) delay(500)
          respond(catalogJson(if (isPrimary(request.url)) "Book-1" else "Book-2"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySourceStore(value = null)
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), preferredSourceStore = store)

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-1"
        store.value shouldBe primaryUrl
      }
    }

    scenario("remembers a slower source when the fastest one fails") {
      runBlocking {
        val engine = MockEngine { request ->
          if (isPrimary(request.url)) respondError(HttpStatusCode.BadGateway)
          else respond(catalogJson("Book-2"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySourceStore(value = null)
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), preferredSourceStore = store)

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-2"
        store.value shouldBe secondaryUrl
      }
    }
  }

  feature("subsequent launch — remembered source first") {
    scenario("queries the remembered source and does not touch the others when it succeeds") {
      runBlocking {
        var primaryHits = 0
        val engine = MockEngine { request ->
          if (isPrimary(request.url)) primaryHits++
          respond(catalogJson(if (isPrimary(request.url)) "Book-1" else "Book-2"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySourceStore(value = secondaryUrl)
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), preferredSourceStore = store)

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-2"
        // remembered source is queried first and, on success, no other source is contacted
        primaryHits shouldBe 0
      }
    }

    scenario("fails over to another source and remembers the new choice") {
      runBlocking {
        val engine = MockEngine { request ->
          if (isPrimary(request.url)) respondError(HttpStatusCode.BadGateway)
          else respond(catalogJson("Book-2"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySourceStore(value = primaryUrl)
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), preferredSourceStore = store)

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-2"
        store.value shouldBe secondaryUrl
      }
    }
  }

  feature("source fallback") {
    scenario("returns Left when every source fails") {
      runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), rounds = 1)

        repo.getAvailableBooks().shouldBeInstanceOf<Either.Left<*>>()
      }
    }
  }
})
