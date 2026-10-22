# Module Naming Conventions

Consistent names let the other skills (clean-architecture, ktor-backend,
compose-multiplatform-ui) target the right module without configuration.

| Pattern             | Kind (convention)        | When to use                                                  |
|---------------------|--------------------------|--------------------------------------------------------------|
| `:domain`           | `kmp`                    | Single domain module; bounded contexts are *packages* inside |
| `:api:contract`     | `kmp.serialization`      | HTTP DTOs + `@Resource` routes + JSON policy                 |
| `:api:mapping`      | `kmp`                    | Domain ↔ DTO mapping                                         |
| `:api:client`       | `kmp.serialization`      | Typed HTTP client (errors as `Either`)                       |
| `:api:client:di`    | `kmp`                    | Koin wiring + Ktor engine per platform                       |
| `:server:infra`     | `jvm.serialization`      | Port adapters: Exposed, LLM, mail, jobs                      |
| `:server:transport` | `jvm`                    | Ktor routing                                                 |
| `:server:di`        | `jvm`                    | Koin: the only module that knows ports *and* adapters        |
| `:server:app`       | `jvm`                    | Entrypoint, config, migration main                           |
| `:features`         | `compose`                | All Compose UI + ViewModels                                  |
| `:webapp`, `:<shell>` | `compose`              | Thin platform shells                                         |

Split a module only when build time or ownership demands it — contexts start as packages
(see `kotlin-clean-architecture`).

## Inter-module dependencies

```kotlin
dependencies {
    implementation(projects.domain)
    implementation(projects.api.contract)
}
```

Never the string form `":domain"` — typos don't fail at configure time.
