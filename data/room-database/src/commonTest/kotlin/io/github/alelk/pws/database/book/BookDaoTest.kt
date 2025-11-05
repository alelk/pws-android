package io.github.alelk.pws.database.book

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.core.DaoTest
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.domain.distinctBy
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@DaoTest
class BookDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("book dao crud test") {
    scenario("insert books one by one and get by id") {
        checkAll(20, Arb.Companion.bookEntity().removeEdgecases()) { book ->
            db.bookDao().insert(book)
            db.bookDao().getById(book.id) shouldBe book
        }
    }

    scenario("count books") {
      db.bookDao().count() shouldBe 20
    }

    scenario("delete book") {
        checkAll(10, Arb.Companion.bookEntity()) { book ->
            db.bookDao().insert(book)
            db.bookDao().delete(book)
            db.bookDao().getById(book.id) shouldBe null
            db.bookDao().count() shouldBe 20
        }
    }

    scenario("delete all books") {
      db.bookDao().deleteAll()
      db.bookDao().count() shouldBe 0
    }
  }

  feature("insert/get/delete book collection") {
    val books = Arb.Companion.bookEntity().distinctBy { it.id }.take(100).toList()

    scenario("insert book collection") {
      db.bookDao().insert(books)
    }

    scenario("check count") {
      db.bookDao().count() shouldBe books.size
    }

    scenario("get books collection by ids") {
      db.bookDao().getByIds(books.map { it.id }) shouldContainExactlyInAnyOrder books
    }

    scenario("delete books collection") {
      val extraBook = Arb.Companion.bookEntity().next()
      db.bookDao().insert(extraBook)
      db.bookDao().delete(books)
      db.bookDao().count() shouldBe 1
      db.bookDao().getById(extraBook.id) shouldBe extraBook
    }
  }
})