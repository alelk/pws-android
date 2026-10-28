---
name: ktor-api-contract
description: Use when designing or modifying the HTTP API contract of a Ktor backend with a Kotlin Multiplatform client — contract-first DTOs in a shared KMP module (api:contract) with ONE declared JSON policy (ApiJson) and contract headers (ApiHeaders), Ktor @Resource-typed nested routes (no path strings, tenant from the token), <Verb><Noun>Request / …Dto naming, sealed DTOs with @JsonClassDiscriminator("type") and snake_case @SerialName, OptionalFieldDto for PATCH, client-generated ids with idempotent create replay (same id + same body = same answer, different body = 409) and a version field for CAS, ApiErrorDto with code + correlationId + details, error codes and details.reason as shared constants (ApiErrorCodes, ValidationReasons; screens never branch on message), enum values pinned as wire vocabulary, cursor pagination (PageDto with nextCursor), Domain↔DTO mapping that never throws (api:mapping), and a typed client (api:client, Either<ApiError, T>). Apply when adding any endpoint, DTO, error code or reason.
metadata:
  version: "2.1.0"
  requires: "kotlin-project-layout, kotlin-clean-architecture, kmp-architecture"
  project-types: "ktor-server, full-stack-kmp-app"
---

# Ktor API Contract

## When to use

- Adding an endpoint, request/response DTO, error code or list.
- Designing URLs for a new area.
- Auditing whether the wire is stable, additive-evolvable and KMP-compatible.

## Module layout

```
api/
├── contract/    KMP: DTOs, @Resource routes, ApiJson, ApiHeaders, ApiErrorDto, PageDto, OptionalFieldDto
├── mapping/     KMP: domain ↔ DTO (+ DTO → Command with structural validation) — depends on domain
├── client/      KMP: one interface per API area, Either<ApiError, T>, CursorPage — does NOT see domain
└── client/di/   KMP: Koin + Ktor engine per platform
```

The same DTOs, routes and JSON policy compile for the JVM server and the wasm client — the wire
cannot drift between them.

## Hard rules

- ❌ Never return a domain entity from a route — map to a DTO.
- ❌ Never put the tenant id in a path or body — the tenant comes from the token.
- ❌ Never write a path string in a route or client call — `@Resource` classes, nested via `parent`.
- ❌ Never declare a second `Json { }` — `ApiJson` in `api:contract` is the only policy
  (`ignoreUnknownKeys = true`, `encodeDefaults = true`, `explicitNulls = false`, no `isLenient`).
- ❌ Never use `Any`, `JsonElement` or `Map<String, Any>` in DTOs.
- ❌ Never let DTO → domain mapping throw: an unknown enum string is a `Left`, not an exception.
- ❌ Never expose `PageDto` or HTTP status codes above `api:client` — callers see `CursorPage` and `ApiError`.
  Defects these prevent: a leaked entity freezes the schema into the wire; a tenant forged in a path reads a stranger's data;
  a second `Json` makes client and server disagree on one body; a throwing mapper turns a new enum value into a 500.
- ✅ Every DTO `@Serializable`; every new field optional with a default (additive evolution).
- ✅ Every sealed DTO: `@JsonClassDiscriminator("type")`; every `@SerialName` is `snake_case` and
  wire-stable forever.
- ✅ Requests `<Verb><Noun>Request`; resource bodies `<Noun>Dto`; non-resource answers
  `<Verb><Noun>Response`; no type is both a request body and a response.
- ✅ Creates carry a client-generated id in a field called **`id`** (never `reportId`/`messageId`);
  a replay with the same `id` and the same body answers the same resource and code and writes or
  holds nothing again; the same `id` with a different body is `409`. The only creates without a
  client id are the named pre-tenant ones (`KNOWN_SERVER_ID_CREATES`) →
  [`references/idempotent-creates.md`](references/idempotent-creates.md).
- ✅ Mutations carry the last-seen `version` **in the body** (called `version` everywhere), not
  `ETag`/`If-Match`: a queued offline command must carry its own version.
- ✅ Every list answers a `PageDto` (cursor), never a bare array. The closed exceptions are streams
  (`application/zip` downloads, SSE), named in the API design.
- ✅ Async work is a **status on the resource + a push** (SSE) — no `202` job queues, no polling
  endpoints. `202` is for "accepted, done later" with the resource state in the body.
- ✅ Error `code`s and `details.reason`s are **constants in `api:contract`** (`ApiErrorCodes`,
  `ValidationReasons`), read by server and client; the transport writes no literal code. A screen
  branches on `code` + `details.reason`, **never on `message`** (logs only)
  → [`references/error-format.md`](references/error-format.md).
- ✅ Enums travel as their stable string names; the client maps them back through `api:mapping`
  (the UI never compares wire strings). The values are **pinned** (`wire-vocabulary.txt`): renaming
  a constant changes wire, column and `CHECK` at once, so it must be red.

Most of these rules are cheap to **test against the contract's own source** —
`ContractConventionsTest`, `ErrorCodeLiteralTest`, `ErrorReasonTest`, `WireVocabularyTest` in
[`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) (the vocabulary and stored-shape
recipe: [`format-locks`](../format-locks/SKILL.md)).

## Templates

| File                                                                                   | Purpose                                        |
|----------------------------------------------------------------------------------------|------------------------------------------------|
| [`templates/api-contract/build.gradle.kts.snippet`](templates/api-contract/build.gradle.kts.snippet) | `api:contract` build file          |
| [`templates/api-contract/ApiJson.kt`](templates/api-contract/ApiJson.kt)               | the one JSON policy                            |
| [`templates/api-contract/ApiHeaders.kt`](templates/api-contract/ApiHeaders.kt)         | header names shared by both banks              |
| [`templates/api-contract/ApiErrorDto.kt`](templates/api-contract/ApiErrorDto.kt)       | error envelope                                 |
| [`templates/api-contract/ApiErrorCodes.kt`](templates/api-contract/ApiErrorCodes.kt)   | the codes and `details.reason` names, shared by both ends |
| [`templates/api-contract/PageDto.kt`](templates/api-contract/PageDto.kt)               | cursor envelope                                |
| [`templates/api-contract/OptionalFieldDto.kt`](templates/api-contract/OptionalFieldDto.kt) | tri-state PATCH field + serializer         |
| [`templates/api-mapping/build.gradle.kts.snippet`](templates/api-mapping/build.gradle.kts.snippet) | `api:mapping` build file             |
| [`templates/api-mapping/Mapping.kt`](templates/api-mapping/Mapping.kt)                 | page, OptionalField and enum mapping helpers   |
| [`templates/api-client/ApiError.kt`](templates/api-client/ApiError.kt)                 | client error model                             |
| [`templates/api-client/HttpClientFactory.kt`](templates/api-client/HttpClientFactory.kt) | the one HttpClient (Auth refresh, request id, SSE) |

## References (load on demand)

- [`references/url-design.md`](references/url-design.md) — URL hierarchy, methods, transitions as sub-resources.
- [`references/ktor-resources.md`](references/ktor-resources.md) — nested `@Resource`, server and client usage.
- [`references/json-policy.md`](references/json-policy.md) — `ApiJson`, additive evolution, what each flag is for.
- [`references/sealed-dtos.md`](references/sealed-dtos.md) — discriminators, `OptionalFieldDto`.
- [`references/error-format.md`](references/error-format.md) — `ApiErrorDto`, code table, `details.reason`, constants, which refusals name a reason.
- [`references/idempotent-creates.md`](references/idempotent-creates.md) — client ids, replay semantics, one-time secrets, ledger keys, `version` vs `ETag`, `Idempotency-Key` as a seam.
- [`references/pagination-envelope.md`](references/pagination-envelope.md) — cursor `PageDto`, `CursorPage`.
- [`references/domain-dto-mapping.md`](references/domain-dto-mapping.md) — `toDto()` / `toCommand()` rules.
- [`references/typed-client.md`](references/typed-client.md) — client interfaces, `Either<ApiError,T>`, refresh.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/api-checklist.md`](checklists/api-checklist.md).
