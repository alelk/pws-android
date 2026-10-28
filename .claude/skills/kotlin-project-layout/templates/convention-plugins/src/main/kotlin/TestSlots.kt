import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType

/**
 * How many test tasks of the whole build may run at once — across modules, not within one
 * (`maxParallelForks` is that). With `org.gradle.parallel` every module's test JVM can otherwise
 * start together on a many-core machine: N forked workers of up to `maxHeapSize` each, next to the
 * Gradle daemon, the Kotlin daemon and wasm-opt. The OOM killer answers to the total (exit 137),
 * never to one JVM's -Xmx. Compilation and the other tasks stay fully parallel.
 *
 * `app.maxParallelTestTasks` in `gradle.properties` (or `-P`) sets the number.
 */
abstract class TestSlots : BuildService<BuildServiceParameters.None>

fun Project.limitConcurrentTestTasks() {
    val slots =
        gradle.sharedServices.registerIfAbsent("testSlots", TestSlots::class) {
            maxParallelUsages.set(
                providers.gradleProperty("app.maxParallelTestTasks").map(String::toInt).orElse(DEFAULT_TEST_SLOTS),
            )
        }
    tasks.withType<Test>().configureEach { usesService(slots) }
}

private const val DEFAULT_TEST_SLOTS = 2
