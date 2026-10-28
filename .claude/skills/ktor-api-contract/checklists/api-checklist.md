# API Contract Checklist

## Modules

- [ ] `api:contract`, `api:mapping`, `api:client` are KMP and compile for wasm (`compileKotlinWasmJs`)
- [ ] `api:contract` does not depend on `domain`; `api:client` does not depend on `domain`
- [ ] `api:mapping` depends on `domain` + `api:contract` only

## JSON & DTOs

- [ ] Exactly one `Json { }` — `ApiJson` in `api:contract`, used by server, client, SSE, tests
- [ ] All DTOs `@Serializable`; new fields optional with defaults; no `Any`/untyped maps
- [ ] Every sealed DTO declares `@JsonClassDiscriminator("type")`; every `@SerialName` is `snake_case`
- [ ] Requests `<Verb><Noun>Request`; resources `<Noun>Dto`; other answers `<Verb><Noun>Response`
- [ ] No type is both a request body and a response
- [ ] Creates carry a client-generated id named `id` (pre-tenant creates are the named exceptions); mutations carry `version` (named exactly `version`) in the body
- [ ] Replay: same `id` + same body → same answer, nothing held again; different body → `409`; a one-time secret is never re-issued
- [ ] Enum values on the wire are pinned (`wire-vocabulary.txt`)
- [ ] PATCH nullable fields are `OptionalFieldDto<T>` defaulting to `Unchanged`

## URLs

- [ ] Every route is a `@Resource`; sub-resources nested via `parent`, no duplicated path strings
- [ ] The tenant comes from the token — no tenant id in any path or body
- [ ] Plural, kebab-case segments; one parameter name per path position
- [ ] Aggregate state changes go through one transitions endpoint with a sealed body
- [ ] The mounted surface is pinned (`api-surface.txt` test); public `/v1` routes are a named list

## Errors

- [ ] Every non-2xx body is `ApiErrorDto(code, message, correlationId, details)`
- [ ] `correlationId` equals `X-Request-Id` (`ApiHeaders.REQUEST_ID`)
- [ ] Every `DomainError` has a status + stable code; two distinct 409 codes (`ALREADY_EXISTS`, `CONFLICT`)
- [ ] Codes are `ApiErrorCodes` constants and reasons `ValidationReasons` constants in `api:contract`; no literal code in the transport
- [ ] A screen that must tell refusals apart reads `details.reason`; nothing reads `message`; the domain reason enum maps by a `when` without `else`
- [ ] Re-authentication `401` carries a reason; credential sign-in / PIN / refresh `401` carry none
- [ ] A `502` whose artefact survived names it in `details`
- [ ] Times/limits the UI must show travel in `details` (and `Retry-After`)

## Pagination

- [ ] Every list returns `PageDto` with `nextCursor` — never a bare list; no `total`; streams (zip, SSE) are the named exceptions
- [ ] Async effects: status on the resource + push; no `202` job queues or polling endpoints
- [ ] A malformed cursor is 422, not 500

## Mapping

- [ ] Domain → DTO is total; DTO → command returns `Either<DomainError, T>` and never throws
- [ ] Unknown enum strings: server → `Left`, client → `null`/unknown variant
- [ ] Round-trips through `ApiJson` tested in `commonTest`

## Client

- [ ] One interface per file, no method bodies; methods return `Either<ApiError, T>`
- [ ] `expectSuccess = false`; one `toApiError()` mapping; `CancellationException` never swallowed
- [ ] Stock `Auth`/bearer refresh with a clear-and-expire policy on failure
- [ ] The wire envelope (`PageDto`) never reaches callers — `CursorPage`
