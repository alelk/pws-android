package io.github.alelk.pws.database.dao

import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.entity.bookEntity
import io.github.alelk.pws.database.entity.bookStatisticEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take

class BookStatisticDaoTest : FeatureSpec({

  val db = pwsDbForTest(inMemory = true)
  val bookStatisticDao = db.bookStatisticDao()

  afterContainer { db.clean() }
  beforeContainer { db.clean() }

  feature("book statistic dao crud test") {

    val books = Arb.bookEntity().take(100).toList().distinctBy { it.externalId }.distinctBy { it.id }

    scenario("insert book") {
      db.bookDao().insert(books)
    }

    val bookStatistics = Arb.bookStatisticEntity(id = Arb.constant(null), bookId = Arb.of(books.map { it.id!! })).take(50).distinctBy { it.bookId }.toList()

    scenario("insert and get by id") {
      bookStatistics.forEach { bookStatistic ->
        val id = bookStatisticDao.insert(bookStatistic)
        val retrieved = bookStatisticDao.getById(id)
        retrieved shouldBe bookStatistic.copy(id = id)
      }
    }

    scenario("get by book id") {
      bookStatistics.forEach { bookStatistic ->
        val retrieved = bookStatisticDao.getByBookId(bookStatistic.bookId)
        retrieved shouldNotBe null
        retrieved!!.id shouldNotBe null
        retrieved shouldBe bookStatistic.copy(id = retrieved.id)
      }
    }

    scenario("count book statistics") {
      bookStatisticDao.count() shouldBe bookStatistics.count()
    }

    scenario("delete book statistic") {
      bookStatistics.forEach { bookStatistic ->
        val entity = bookStatisticDao.getByBookId(bookStatistic.bookId)
        entity shouldNotBe null
        bookStatisticDao.delete(entity!!)
        bookStatisticDao.getById(entity.id!!) shouldBe null
      }
    }

    scenario("delete all") {
      bookStatistics.forEach { db.bookStatisticDao().insert(it) }
      db.bookStatisticDao().deleteAll()
      db.bookStatisticDao().count() shouldBe 0
    }
  }
})