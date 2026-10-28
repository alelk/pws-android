# Clean Architecture Checklist

## Layer separation

- [ ] `domain` has zero imports from `io.ktor.*`, `org.jetbrains.exposed.*`, `kotlinx.serialization.*`, `org.koin.*`
- [ ] `./gradlew :domain:compileKotlinWasmJs` passes (no `java.*` in `commonMain`)
- [ ] `server:infra` has zero imports from `api:contract` / `api:mapping`
- [ ] `server:transport` has no business `if/when` and no role checks — only DTO↔command and `Either` folding
- [ ] `server:di` is the only module binding ports to adapters
- [ ] Shells contain only entrypoint, DI start and platform implementations

## Contexts

- [ ] `domain` packages are `<context>.<part>`; the context order is written down
- [ ] No import points to a context later in the order; no new package cycle inside a context —
      enforced by a test that runs in `./gradlew build`
- [ ] Shared kernel (`common`) holds only pure values/functions used across contexts
- [ ] Synchronous cross-context reactions are reverse ports declared upstream, implemented downstream;
      handlers are `when` without `else`
- [ ] Effect funnels are ordered lists in the lock order, pinned by a test on the production DI wiring

## Ports & adapters

- [ ] Every external dependency (DB, HTTP, LLM, STT, mail, storage, clock) has a port in `domain`
- [ ] Adapters live in `server:infra`, named by technology (`Exposed*`, `Smtp*`, `OpenRouter*`)
- [ ] Optional/paid services have an `Unconfigured*` adapter; no nullable ports in use-cases
- [ ] Cross-cutting concerns (budget, audit) are decorators on the port, not code in use-cases

## Errors

- [ ] One `DomainError` sealed class; a variant per distinct client reaction
- [ ] Use-cases return `Either<DomainError, T>`; no thrown business exceptions
- [ ] Adapters catch specific exceptions, rethrow `CancellationException`, map SQLSTATEs meaningfully
- [ ] Transport maps every variant exhaustively (no `else`); foreign-tenant data is `404`

## Use-cases

- [ ] One class = one operation, verb-named, single `suspend operator fun invoke(command)`
- [ ] `TransactionRunner` + `TenantContext` injected/used; `Clock` injected
- [ ] Writes use `inRwTransaction` (a `Left` rolls back); `inRwTransactionKeepingWrites` only for files in `KNOWN_COMMITS_ON_LEFT`
