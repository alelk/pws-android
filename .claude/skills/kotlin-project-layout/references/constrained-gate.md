# The gate in a memory-poor environment

`./gradlew build` is the one gate. Agent sandboxes and small runners sometimes cannot run it whole:
the kernel's OOM killer ends a process (exit 137, or a Gradle task "killed" with no Kotlin error),
most often the wasm optimiser (`wasm-opt`) of a Kotlin/Wasm production link, or a module's heavy
test task running beside the daemons.

*Mine:* an agent that quietly shrinks the gate until it passes — and reports "build green" for a
build that never compiled the production web bundle or ran the UI tests.

## Rules

1. **The full gate stays the rule.** CI runs it whole. A constrained form is an **agreed
   exception for one environment**, written in the step README ("Gate in this environment"), not
   chosen per session by an agent.
2. **Name every exclusion.** Each `-x` task is listed, with why (OOM, not a test failure), and goes
   into the step's **"not run"** list at close
   ([`doc-first-agentic-workflow`](../../doc-first-agentic-workflow/references/owner-questions-and-close.md)).
3. **Split before you skip.** A test task that only fails for memory runs as a **second
   invocation** of its own when the stage touched that module — it is not dropped.
4. **Only optimisation and packaging tasks may be excluded**, never compilation of a target (the
   KMP purity check is the `wasmJs` compile, not the optimiser) and never static analysis.
5. **Output to a file**: `> build/gate.log 2>&1; tail -40 build/gate.log`. The full log in an agent's
   context costs more than the build.

## Recipe

```bash
# 1. Everything except the wasm optimiser and the heaviest test task, fewer workers
./gradlew build \
  -x :webapp:compileProductionExecutableKotlinWasmJsOptimize \
  -x :features:jvmTest \
  --max-workers=2 > build/gate.log 2>&1; tail -40 build/gate.log

# 2. The split-off test task, alone — when the stage touched that module or the client
./gradlew :features:jvmTest --max-workers=2 > build/gate-features.log 2>&1; tail -40 build/gate-features.log
```

Knobs, in the order to try them:

| Knob | Effect |
|------|--------|
| `--max-workers=2` | fewer tasks (and their JVMs) at once |
| `-Papp.maxParallelTestTasks=1` | test JVMs of different modules queue ([`templates/convention-plugins/src/main/kotlin/TestSlots.kt`](../templates/convention-plugins/src/main/kotlin/TestSlots.kt)) |
| `./gradlew --stop; pkill -f "[K]otlinCompileDaemon"` between invocations | an idle Kotlin daemon holds its heap |
| `kotlin.daemon.jvmargs` with G1 + `G1PeriodicGCInterval` (template `gradle.properties`) | the daemon returns heap after the wasm link |
| `-x <…Optimize>` | the optimiser is the single largest peak; only the production bundle needs it |

## What the constrained gate cannot tell you

- The production web bundle builds and its **size** — measure it elsewhere and say so
  ([`compose-web-shell`](../../compose-web-shell/SKILL.md)).
- Whatever the excluded test task covers, if it was not run as step 2.
- Cache illusions: tasks `FROM-CACHE` from an earlier green run did not run against today's code
  only if their inputs changed — state which modules actually executed when it matters.

## Evidence

family-planner step READMEs 73–81 "Build in the sandbox" (`wasm-opt` OOM-killed; gate
`-x :webapp:compileProductionExecutableKotlinWasmJsOptimize -x :features:jvmTest --max-workers=2`,
`:features:jvmTest` as its own run), the web-bundle measure listed as "not run" in every close
since step 72; `TestSlots.kt`, `gradle.properties` (`kotlin.daemon.jvmargs` with G1).
