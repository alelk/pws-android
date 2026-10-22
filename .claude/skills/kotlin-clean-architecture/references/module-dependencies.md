# Module Dependency Rules

Gradle enforces them: a module that is not a dependency cannot be imported. Keep each
`build.gradle.kts` matching this table.

| Module             | May depend on                                                  | Must NOT depend on                     |
|--------------------|----------------------------------------------------------------|----------------------------------------|
| `domain`           | stdlib, Arrow, kotlinx-coroutines, kotlinx-datetime            | everything else (incl. Koin, serialization) |
| `api:contract`     | stdlib, kotlinx.serialization, ktor-resources                  | domain, server:*, features             |
| `api:mapping`      | domain, api:contract                                           | server:*, api:client, features         |
| `api:client`       | api:contract, Ktor Client, Arrow                               | domain, server:*, features             |
| `api:client:di`    | api:client, Koin, Ktor engines                                 | domain, server:*, features             |
| `features`         | domain, api:client, api:mapping, Compose, Koin                 | server:*                               |
| shells             | features, api:client:di, Koin, platform SDK                    | server:*; domain/api:* directly        |
| `server:infra`     | domain                                                         | api:*, transport, di, app              |
| `server:transport` | domain, api:contract, api:mapping, Ktor Server                 | infra, di, app, features               |
| `server:di`        | domain, server:infra, server:transport, Koin                   | api:* (except via transport), app, features |
| `server:app`       | server:di (+ transitive), Ktor Server                          | —                                      |

Inside `features` the same strictness applies between packages:
`designsystem` (leaf) ← `components` ← screens ← `app` (composition root). See
[`compose-multiplatform-ui`](../../compose-multiplatform-ui/SKILL.md).

## Quick checks

```bash
# Domain must have zero framework deps
./gradlew :domain:dependencies --configuration commonMainRuntimeClasspath | grep -E "ktor|exposed|flyway|koin"
# commonMain must compile for the web target (no java.*)
./gradlew :domain:compileKotlinWasmJs :api:contract:compileKotlinWasmJs :api:mapping:compileKotlinWasmJs
```

Package-level rules (context order, cycles, design-system leaf) are tests, not review —
see [`architecture-fitness-tests`](../../architecture-fitness-tests/SKILL.md).
