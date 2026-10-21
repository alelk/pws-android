package io.github.myorg.myapp.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory
import java.util.UUID

/*
 * TEMPLATE. `e2eApp`, `seedTenant`, `walkDoors`, `actionEffectVisible`, `journalRows` and
 * `TestDatabase.asAdmin` are your project's (an end-to-end harness over Testcontainers).
 */

/**
 * A side effect that must never block the action. When the journal cannot write, the action STOOD,
 * the client gets the same answers as with a working journal, and the log carries one
 * `ERROR audit write failed kind=… correlationId=…` per door — the only place the gap is seen.
 *
 * The failing adapter is the real one over a table that refuses (a trigger for this one tenant), so
 * no seam in the code is needed. A wrapper that writes the row inside the action's own transaction
 * is caught too: the action would roll back with the row and the effect assertion goes red.
 *
 * To see it fail: write the journal row inside the use-case's transaction — the action disappears.
 */
class FaultInjectionTest :
    FunSpec({
        test("guard: the doors answer as expected and the effect is visible") {
            e2eApp { client ->
                val tenant = seedTenant(client)
                walkDoors(client, tenant) shouldBe EXPECTED_ANSWERS
                actionEffectVisible(tenant.id) shouldBe true
            }
        }

        test("lock: the journal refusing every write — same answers, action stood, one ERROR per door, no row") {
            e2eApp { client ->
                val tenant = seedTenant(client)
                val (answers, log) = withRefusingTable("audit_journal", tenant.id) { capturingLog { walkDoors(client, tenant) } }

                answers shouldBe EXPECTED_ANSWERS
                actionEffectVisible(tenant.id) shouldBe true
                withClue("log: $log") {
                    log.filter { it.startsWith("ERROR audit write failed") }
                        .map { it.substringAfter("kind=").substringBefore(' ') } shouldContainExactly KINDS
                }
                journalRows(tenant.id).shouldBeEmpty()
            }
        }

        test("pair: the live journal — same answers, a row per door, no ERROR") {
            e2eApp { client ->
                val tenant = seedTenant(client)
                val (answers, log) = capturingLog { walkDoors(client, tenant) }

                answers shouldBe EXPECTED_ANSWERS
                log.filter { it.startsWith("ERROR audit write failed") }.shouldBeEmpty()
                journalRows(tenant.id).map { it.kind } shouldContainExactly KINDS
            }
        }
    })

private val EXPECTED_ANSWERS = listOf(HttpStatusCode.Accepted, HttpStatusCode.NoContent, HttpStatusCode.Unauthorized)
private val KINDS = listOf("deletion_requested", "deletion_cancelled", "ownership_transferred")

/** [table] refuses every insert of [tenantId] while [block] runs; trigger and function dropped afterwards. */
suspend fun <T> withRefusingTable(
    table: String,
    tenantId: String,
    block: suspend () -> T,
): T {
    val name = "refuses_${UUID.randomUUID().toString().replace("-", "")}"
    TestDatabase.asAdmin { conn ->
        conn.createStatement().use { st ->
            st.execute(
                "CREATE FUNCTION $name() RETURNS trigger LANGUAGE plpgsql AS " +
                    "\$\$ BEGIN RAISE EXCEPTION 'refused by the test'; END \$\$",
            )
            st.execute(
                "CREATE TRIGGER $name BEFORE INSERT ON $table FOR EACH ROW " +
                    "WHEN (NEW.tenant_id = '$tenantId'::uuid) EXECUTE FUNCTION $name()",
            )
        }
    }
    return try {
        block()
    } finally {
        TestDatabase.asAdmin { conn ->
            conn.createStatement().use { st ->
                st.execute("DROP TRIGGER IF EXISTS $name ON $table")
                st.execute("DROP FUNCTION IF EXISTS $name()")
            }
        }
    }
}

/** What the product's loggers wrote while [block] ran, as `LEVEL message` lines. */
suspend fun <T> capturingLog(block: suspend () -> T): Pair<T, List<String>> {
    val logger = LoggerFactory.getLogger("io.github.myorg.myapp") as Logger
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    val previous = logger.level
    logger.level = Level.INFO
    logger.addAppender(appender)
    val result =
        try {
            block()
        } finally {
            logger.detachAppender(appender)
            logger.level = previous
        }
    val lines = synchronized(appender.list) { appender.list.map { "${it.level} ${it.formattedMessage}" } }
    return result to lines
}
