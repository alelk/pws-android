// Prints how long each Gradle task took, so a slow CI build can be read from its log.
//   ./gradlew --init-script gradle/ci-task-timings.init.gradle.kts build
//
// Two outputs:
//  - a `[timing]` line as soon as a task that ran 10 s or longer finishes (so a job cut by its
//    timeout still leaves the slow tasks in the log);
//  - at the end of the build, the 25 slowest tasks with their outcome (EXECUTED / FROM-CACHE / UP-TO-DATE),
//    also appended to the job summary when GITHUB_STEP_SUMMARY is set.
// Time of configuration and of dependency resolution is not a task: for that, run with `--profile`.
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

class TaskRow(val path: String, val millis: Long, val outcome: String)

abstract class TaskTimings : BuildService<BuildServiceParameters.None>, OperationCompletionListener, AutoCloseable {
    var slowMillis = 10_000L
    var top = 25
    var requested = ""
    private val rows = ConcurrentLinkedQueue<TaskRow>()
    private val startedAt = System.currentTimeMillis()

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        val result = event.result
        val outcome = when (result) {
            is TaskSuccessResult -> if (result.isFromCache) "FROM-CACHE" else if (result.isUpToDate) "UP-TO-DATE" else "EXECUTED"
            is TaskSkippedResult -> "SKIPPED"
            is TaskFailureResult -> "FAILED"
            else -> "?"
        }
        val row = TaskRow(event.descriptor.taskPath, result.endTime - result.startTime, outcome)
        rows += row
        if (row.millis >= slowMillis) println("[timing] ${row.path} ${seconds(row.millis)} ${row.outcome}")
    }

    override fun close() {
        val slowest = rows.sortedByDescending { it.millis }.take(top)
        val executed = rows.filter { it.outcome == "EXECUTED" }
        val wall = System.currentTimeMillis() - startedAt
        val lines = buildList {
            add("Slowest ${slowest.size} of ${rows.size} tasks (${executed.size} executed, ${rows.count { it.outcome == "FROM-CACHE" }} from cache, ${rows.count { it.outcome == "UP-TO-DATE" }} up to date); task execution wall time ${seconds(wall)}")
            slowest.forEach { add("${seconds(it.millis).padStart(9)}  ${it.outcome.padEnd(10)}  ${it.path}") }
        }
        println("\n[timing] " + lines.first())
        lines.drop(1).forEach { println("[timing] $it") }

        System.getenv("GITHUB_STEP_SUMMARY")?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                File(path).appendText(buildString {
                    appendLine("### Gradle: slowest tasks of `./gradlew $requested`")
                    appendLine()
                    appendLine(lines.first())
                    appendLine()
                    appendLine("| seconds | outcome | task |")
                    appendLine("|---:|---|---|")
                    slowest.forEach { appendLine("| ${"%.1f".format(it.millis / 1000.0)} | ${it.outcome} | `${it.path}` |") }
                    appendLine()
                })
            }
        }
    }

    private fun seconds(millis: Long) = "%.1fs".format(millis / 1000.0)
}

abstract class TaskTimingsPlugin @Inject constructor(private val registry: BuildEventsListenerRegistry) : Plugin<Gradle> {
    override fun apply(gradle: Gradle) {
        val service = gradle.sharedServices.registerIfAbsent("taskTimings", TaskTimings::class.java) {}
        service.get().requested = gradle.startParameter.taskNames.joinToString(" ")
        registry.onTaskCompletion(service)
    }
}

apply<TaskTimingsPlugin>()
