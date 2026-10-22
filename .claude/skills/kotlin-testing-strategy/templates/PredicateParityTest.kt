package io.github.myorg.myapp.infra

import io.github.myorg.myapp.domain.common.tx.TenantContext
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.days

/**
 * The same rule expressed twice — a pure domain predicate and a SQL WHERE — must answer the same
 * on every row. The second expression drifts not by malice but because nobody compares them (a
 * migration shifts a boundary by a day; a unit test of the pure function stays green).
 *
 * Seed a matrix over every dimension the rule reads (status × date × closedAt …) in a real
 * Postgres, then compare the id set the SQL returns with the ids the pure function accepts.
 */
// TEMPLATE: seedTenant, aWindow, aTask, FIXED_TODAY/FIXED_NOW, listWindow are your fixtures/ports.
class PredicateParityTest :
    BaseRepositoryTest({
        test("listWindow(SQL) returns exactly the rows Window.contains(domain) accepts") {
            val tenant = seedTenant()
            val window = aWindow(today = FIXED_TODAY, horizonDays = 14, doneRetentionDays = 7)
            val seeded =
                buildList {
                    for (status in TaskStatus.entries)
                        for (date in listOf(null, FIXED_TODAY.minus(8, DateTimeUnit.DAY), FIXED_TODAY, FIXED_TODAY.plus(14, DateTimeUnit.DAY), FIXED_TODAY.plus(15, DateTimeUnit.DAY)))
                            for (closedAt in listOf(null, FIXED_NOW - 6.days, FIXED_NOW - 8.days))
                                add(aTask(tenantId = tenant, status = status, scheduledDate = date, closedAt = closedAt))
                }
            txRunner.inRwTransaction(TenantContext.Tenant(tenant)) { seeded.forEach { tasks.insert(it) } }

            val fromSql =
                txRunner.inRoTransaction(TenantContext.Tenant(tenant)) { tasks.listWindow(tenant, window) }
                    .shouldBeRight().map { it.id }.toSet()
            val fromDomain = seeded.filter { window.contains(it) }.map { it.id }.toSet()

            fromSql shouldBe fromDomain
        }
    })
