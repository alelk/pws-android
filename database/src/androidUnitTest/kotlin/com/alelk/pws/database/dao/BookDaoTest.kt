package com.alelk.pws.database.dao

import android.database.sqlite.SQLiteException
import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.common.entity.bookEntity
import io.github.alelk.pws.domain.model.bookExternalId
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.firstOrNull
import org.junit.jupiter.api.assertThrows

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class BookDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("book dao crud test") {
    scenario("insert books one by one and get by id") {
      checkAll(10, Arb.bookEntity().removeEdgecases()) { book ->
        db.bookDao().insert(book) shouldBe book.id
        db.bookDao().getById(book.id!!) shouldBe book
      }
    }

    scenario("get book by external id") {
      checkAll(10, Arb.bookEntity().removeEdgecases()) { book ->
        db.bookDao().insert(book)
        db.bookDao().getByExternalId(book.externalId).firstOrNull() shouldBe book
      }
    }

    scenario("count books") {
      db.bookDao().count() shouldBe 20
    }

    scenario("delete book") {
      checkAll(10, Arb.bookEntity()) { book ->
        db.bookDao().insert(book)
        db.bookDao().delete(book)
        db.bookDao().getById(book.id!!) shouldBe null
        db.bookDao().count() shouldBe 20
      }
    }

    scenario("delete all books") {
      db.bookDao().deleteAll()
      db.bookDao().count() shouldBe 0
    }
  }

  feature("insert/get/delete book collection") {
    val books = Arb.bookEntity().take(100).toList().distinctBy { it.externalId }

    scenario("insert book collection") {
      db.bookDao().insert(books) shouldContainExactly books.map { it.id }
    }

    scenario("check count") {
      db.bookDao().count() shouldBe books.size
    }

    scenario("get books collection by ids") {
      db.bookDao().getByIds(books.map { it.id!! }) shouldContainExactlyInAnyOrder books
    }

    scenario("get books collection by external ids") {
      val expected = books.take(100)
      db.bookDao().getByExternalIds(expected.map { it.externalId }) shouldContainExactlyInAnyOrder expected
    }

    scenario("delete books collection") {
      val extraBook = Arb.bookEntity().next()
      db.bookDao().insert(extraBook)
      db.bookDao().delete(books)
      db.bookDao().count() shouldBe 1
      db.bookDao().getById(extraBook.id!!) shouldBe extraBook
    }
  }

  feature("autogenerate id") {
    scenario("autogenerate singe book id") {
      checkAll(100, Arb.bookEntity(id = Arb.constant(null)).removeEdgecases()) { book ->
        book.id shouldBe null
        val id = db.bookDao().insert(book)
        id shouldNotBe null
        val created = db.bookDao().getById(id)
        created shouldNotBe null
        created!!.id shouldBe id
        created.copy(id = null) shouldBe book
      }
    }
    scenario("autogenerate collection books id") {
      val books = Arb.bookEntity(id = Arb.constant(null)).take(100).toList().distinctBy { it.externalId }
      val ids = db.bookDao().insert(books)
      ids shouldHaveSize books.size
      val created = db.bookDao().getByIds(ids)
      created.forEach { it.id shouldNotBe null }
      created.map { it.copy(id = null) } shouldContainExactlyInAnyOrder books
    }
  }

  feature("check constraints") {
    scenario("add 2 books with the same book external id (should be unique)") {
      val (book1, book2) = Arb.bookEntity(externalId = Arb.bookExternalId("book-id")).take(2).toList().let { it[0] to it[1] }
      db.bookDao().insert(book1)
      assertThrows<SQLiteException> {
        db.bookDao().insert(book2)
      }.message shouldContain "UNIQUE constraint failed: books.edition"
    }
  }
})