package io.github.myorg.myapp.app

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * <Operation> over the whole stack — the step's mine.
 *
 * A unit test "the debit is refused when the balance is too small" is green with serialisation
 * completely broken: fakes have no transactions, so the calls are sequential by construction. The
 * only check that catches a missing lock is N simultaneous requests THROUGH THE ROUTE against a
 * state that allows exactly one of them. Assert on the count of successes (and of written rows),
 * not only on the final state — the broken version may leave a state that is legal for other
 * reasons.
 *
 * To see it fail on purpose: <remove forUpdate() from Repository.lockForUpdate> — both succeed.
 * <Add SKIP LOCKED> — the second answers 404 instead of waiting.
 */
class ConcurrentRouteTest :
    FunSpec({
        test("two simultaneous requests against room for exactly one: one success, one refusal") {
            testApplication {
                val client = startApp()                 // real module over Testcontainers
                val actor = seedActorWithRoomForOne(client)

                val responses =
                    coroutineScope {
                        listOf(async { attempt(client, actor) }, async { attempt(client, actor) }).awaitAll()
                    }

                withClue("exactly one of two simultaneous requests may succeed") {
                    responses.count { it.status == HttpStatusCode.Created } shouldBe 1
                    responses.count { it.status == HttpStatusCode.UnprocessableEntity } shouldBe 1
                }
                writtenRows(client, actor) shouldBe 1
            }
        }

        test("ten simultaneous requests: exactly one success") {
            testApplication {
                val client = startApp()
                val actor = seedActorWithRoomForOne(client)
                val responses = coroutineScope { (1..10).map { async { attempt(client, actor) } }.awaitAll() }
                responses.count { it.status == HttpStatusCode.Created } shouldBe 1
                writtenRows(client, actor) shouldBe 1
            }
        }
    })
