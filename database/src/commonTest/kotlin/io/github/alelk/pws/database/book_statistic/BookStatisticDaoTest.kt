package io.github.alelk.pws.database.book_statistic

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.core.DaoTest
import io.github.alelk.pws.database.book.bookEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.domain.distinctBy
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take

@DaoTest
class BookStatisticDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("book statistic dao crud test") {

    val books = Arb.Companion.bookEntity().take(100).toList()

    scenario("insert book") {
      db.bookDao().insert(books)
    }

    val bookStatistics = Arb.Companion.bookStatisticEntity(id = Arb.Companion.of(books.map { it.id }).removeEdgecases()).distinctBy { it.id }.take(50).toList()

    scenario("insert and get by id") {
      bookStatistics.forEach { bookStatistic ->
        db.bookStatisticDao().insert(bookStatistic)
        val retrieved = db.bookStatisticDao().getById(bookStatistic.id)
        retrieved shouldBe bookStatistic
      }
    }

    scenario("count book statistics") {
      db.bookStatisticDao().count() shouldBe bookStatistics.count()
    }

    scenario("delete book statistic") {
      bookStatistics.forEach { bookStatistic ->
        val entity = db.bookStatisticDao().getById(bookStatistic.id)
        entity shouldNotBe null
        db.bookStatisticDao().delete(entity!!)
        db.bookStatisticDao().getById(entity.id) shouldBe null
      }
    }

    scenario("insert all") {
      db.bookStatisticDao().insert(bookStatistics)
      db.bookStatisticDao().count() shouldBe bookStatistics.count()
    }

    scenario("delete all") {
      db.bookStatisticDao().deleteAll()
      db.bookStatisticDao().count() shouldBe 0
    }
  }
})