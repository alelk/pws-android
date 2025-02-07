package io.github.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.common.entity.bookEntity
import io.github.alelk.pws.database.common.entity.bookStatisticEntity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class BookStatisticDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("book statistic dao crud test") {

    val books = Arb.bookEntity().take(100).toList().distinctBy { it.externalId }.distinctBy { it.id }

    scenario("insert book") {
      db.bookDao().insert(books)
    }

    val bookStatistics =
      Arb.bookStatisticEntity(id = Arb.constant(null), bookId = Arb.of(books.map { it.id!! })).take(50).distinctBy { it.bookId }.toList()

    scenario("insert and get by id") {
      bookStatistics.forEach { bookStatistic ->
        val id = db.bookStatisticDao().insert(bookStatistic)
        val retrieved = db.bookStatisticDao().getById(id)
        retrieved shouldBe bookStatistic.copy(id = id)
      }
    }

    scenario("get by book id") {
      bookStatistics.forEach { bookStatistic ->
        val retrieved = db.bookStatisticDao().getByBookId(bookStatistic.bookId)
        retrieved shouldNotBe null
        retrieved!!.id shouldNotBe null
        retrieved shouldBe bookStatistic.copy(id = retrieved.id)
      }
    }

    scenario("count book statistics") {
      db.bookStatisticDao().count() shouldBe bookStatistics.count()
    }

    scenario("delete book statistic") {
      bookStatistics.forEach { bookStatistic ->
        val entity = db.bookStatisticDao().getByBookId(bookStatistic.bookId)
        entity shouldNotBe null
        db.bookStatisticDao().delete(entity!!)
        db.bookStatisticDao().getById(entity.id!!) shouldBe null
      }
    }

    scenario("delete all") {
      bookStatistics.forEach { db.bookStatisticDao().insert(it) }
      db.bookStatisticDao().deleteAll()
      db.bookStatisticDao().count() shouldBe 0
    }
  }
})