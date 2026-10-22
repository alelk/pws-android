# Error Response Format

Every non-2xx body is an [`ApiErrorDto`](../templates/api-contract/ApiErrorDto.kt):

```json
{ "code": "VALIDATION_ERROR", "message": "Too many open proposals",
  "correlationId": "0192…", "details": { "reason": "too_many_open_proposals" } }
```

- `code` — machine-readable, `SCREAMING_SNAKE_CASE`, **the thing the client branches on and
  localises**. Stable forever.
- `message` — English, for logs and developers. **Never shown to users, never branched on.**
- `correlationId` — equals the `X-Request-Id` response header.
- `details` — typed hints (strings only) the UI needs to phrase the refusal: `reason`, `resetAt`,
  `until`, `limitBytes`, `field`, `id`, `service`. Empty otherwise.

## Codes and reasons are constants in `api:contract`

```kotlin
object ApiErrorCodes { const val CONFLICT = "CONFLICT"; /* … */ }
object ValidationReasons {
    const val DETAILS_KEY = "reason"
    const val TEXT_TOO_LONG = "text_too_long"
    const val TOO_MANY_OPEN_PROPOSALS = "too_many_open_proposals"
    /** The reason of a 403, not of a 422: the tenant is being deleted. */
    const val TENANT_DELETING = "tenant_deleting"
}
```

Template: [`ApiErrorCodes.kt`](../templates/api-contract/ApiErrorCodes.kt). Both ends read the same
declaration, like the header names in `ApiHeaders`: renaming a code is a compile error on both, not a
silent mismatch. The server transport writes **no literal** (`ErrorCodeLiteralTest`), the UI reads no
`message` (`ErrorReasonTest`) — both in `architecture-fitness-tests` (`references/wire-vocabulary.md`).

On the domain side a refusal carries an enum, translated by one exhaustive `when` in the transport:

```kotlin
enum class ValidationReason { TextTooLong, TooManyOpenProposals /* … */ }
private fun ValidationReason.wireName(): String = when (this) {     // no else: a new reason needs a name
    ValidationReason.TextTooLong -> ValidationReasons.TEXT_TOO_LONG
    ValidationReason.TooManyOpenProposals -> ValidationReasons.TOO_MANY_OPEN_PROPOSALS
}
```

**A reason is added the day a screen needs to tell one refusal from another** — not to each of the
two hundred places a validation error is made. Most refusals are fine as a bare code.

Mine it prevents: a screen that matched on `message` and silently changed behaviour when a server
phrase was edited for a log line.

## Code table

| HTTP | Code                     | Meaning / client reaction                                       |
|------|--------------------------|-----------------------------------------------------------------|
| 400  | `BAD_REQUEST`            | didn't fit the contract — a client bug, not user input           |
| 401  | `UNAUTHORIZED`           | no/expired session → refresh, else sign-in                      |
| 403  | `FORBIDDEN`              | role may not → the UI shouldn't have offered it; state walls carry a `reason` (below) |
| 403  | `NO_ACTIVE_TENANT`       | valid session, no tenant chosen → open the chooser              |
| 404  | `NOT_FOUND`              | absent **or another tenant's** (deliberately indistinguishable) |
| 409  | `ALREADY_EXISTS`         | duplicate client id (with `details.field`)                      |
| 409  | `CONFLICT`               | stale `version` → re-read and replay; a one-at-a-time operation already running carries a `reason` |
| 410  | `GONE`                   | the address was valid, the thing no longer is → re-fetch        |
| 410  | `<THING>_ERASED`         | gone **because it was erased on purpose** (privacy) — chosen by the domain error type, not by the entity name |
| 413  | `PAYLOAD_TOO_LARGE`      | with `details.limitBytes`; decided **before** reading the body  |
| 415  | `UNSUPPORTED_MEDIA_TYPE` | outside the allow-list                                          |
| 422  | `VALIDATION_ERROR`       | a business rule said no; `details.reason` where a screen branches |
| 423  | `LOCKED`                 | temporary lockout, `Retry-After` + `details.until`              |
| 429  | `TOO_MANY_REQUESTS`      | rate/quota, `Retry-After` (+ `details.resetAt` for quotas)      |
| 500  | `INTERNAL_ERROR`         | we broke; cause in logs (a half-done multi-step import may name what it created in `details`) |
| 502  | `BAD_GATEWAY`            | a provider failed (`details.service`) → "try again"; **an artefact that survived is named** (`details.<thing>Id`) so the client offers to reuse it |
| 507  | `INSUFFICIENT_STORAGE`   | tenant storage full, `details.reason` names which ceiling class  |

Two 409s with different codes is intentional: the client (and an offline queue) reacts differently.

## Reasons on codes other than 422

`details.reason` is not only for `VALIDATION_ERROR`. It names **which wall** a `403`/`409`/`401` is:

- **State walls (`403`)** — the tenant is being deleted, the address is not verified yet, consent for
  an external processor is missing: the same status as a role refusal, a different screen.
- **Re-authentication (`401` + `reason=invalid_password`)** — for an irreversible action that asked
  for the current password (transfer, delete, export, change of address). A **credential** sign-in,
  PIN or refresh carries **no** reason: there the person is not known yet, and a reason would help
  enumeration ([`ktor-auth-sessions`](../../ktor-auth-sessions/SKILL.md)).
- **Single-flight (`409`)** — "an export link is already waiting" (`archive_in_progress`) vs a stale `version`.

Reasons other skills introduce (all in the one `ValidationReasons` object):

| Reason | Status | Introduced by |
|--------|--------|---------------|
| `tenant_deleting` | 403 | [`tenant-data-lifecycle`](../../tenant-data-lifecycle/SKILL.md) (deletion as a state) |
| `archive_in_progress` | 409 | [`tenant-data-lifecycle`](../../tenant-data-lifecycle/references/export-and-import.md) (one link at a time) |
| `archive_format_unsupported` + `details.format` | 422 | [`tenant-data-lifecycle`](../../tenant-data-lifecycle/references/export-and-import.md) (import) |
| `owns_tenant` + the tenants in `details` | 422 | [`tenant-data-lifecycle`](../../tenant-data-lifecycle/references/personal-scope.md) (account deletion) |
| `too_many_tenants`, `tenant_limit`, `text_too_long`, `storage_full` | 422 / 507 | [`security-hardening-baseline`](../../security-hardening-baseline/references/tenant-ceilings.md) |
| `ai_off` | 422 | [`ai-spend-and-entitlement`](../../ai-spend-and-entitlement/SKILL.md) |
| `ai_consent` | 403 | [`ai-spend-and-entitlement`](../../ai-spend-and-entitlement/SKILL.md) (per-member reach) |
| `details.resetAt` on `429` | 429 | [`ai-spend-and-entitlement`](../../ai-spend-and-entitlement/references/allowance-and-order.md) (allowances) |

A link to an exported archive or a one-time secret that is unknown, used or expired answers one
uniform `404` — never a reason that tells them apart.

## Rules

- `DomainError → (status, code, details)` is one exhaustive mapping on the server
  ([ktor-backend / error-handling.md](../../ktor-backend/references/error-handling.md)).
- The client maps `(status, code)` → a sealed `ApiError` in **one** place, keeping `details.reason`
  available to screens; UI code never sees a status number, and compares `reason` only with
  `ValidationReasons` constants.
- A time the UI must show ("try again at 15:40") is computed server-side in the tenant's timezone
  and sent in `details`, besides `Retry-After`.
- Refusals that must be cheap come **before work**: `413` before the body is read (a JSON body
  ceiling, e.g. 128 KB, on every route), `429` from a bucket every route stands in.

Evidence: family-planner step 76 (A1 `details.reason`, A2 `ApiErrorCodes`, `ErrorCodeLiteralTest`,
`ErrorReasonTest`, G14 "a reason the day a screen needs it"), step 78 (128 KB body limit, `storage_full`,
`email_unverified`), steps 80–81 (`family_deleting`, `ai_consent`, `invalid_password` on re-auth only,
`ATTACHMENT_ERASED`, `archive_in_progress`).
