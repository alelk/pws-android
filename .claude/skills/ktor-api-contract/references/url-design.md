# URL Design

## The tenant is not in the URL

Multi-tenant APIs take the tenant from the **token** (active tenant claim). `/v1/tasks`, not
`/v1/workspaces/{id}/tasks`:

- a forged path cannot select someone else's tenant — there is nothing to forge;
- switching tenant is a token refresh (`POST /v1/auth/switch-tenant`), not a URL rewrite;
- the same URL means the same thing for every member, so links can be shared inside a tenant.

Use a tenant path segment only for genuinely cross-tenant operator APIs (`/ops/tenants/{id}`),
behind a different auth scheme.

## Shape

```
/v1/tasks                               collection   GET (cursor page) · POST (create, client id)
/v1/tasks/{id}                          item         GET · PATCH (version) · DELETE
/v1/tasks/{id}/transitions              action       POST {type:"start"|…, version}
/v1/tasks/{id}/reports                  sub-collection of things that only exist under the task
/v1/me · /v1/auth/login · /v1/sync      singletons / verbs that are not resources
```

- Plural nouns, **kebab-case** segments (`/time-entries`).
- Sub-resources hang off their parent when they have no meaning apart from it; don't add a second
  top-level collection for the same tree.
- **One parameter name per path position** (`/v1/tasks/{id}/…` everywhere — never `{taskId}` in
  one route and `{id}` in another at the same position).
- State changes of an aggregate go through **one** transitions endpoint with a sealed body, not
  `/start`, `/cancel`, `/reopen` — the domain state machine is the single authority, and the wire
  mirrors its transition type.
- Versioning: `/v1` evolves additively (new optional fields, new sealed variants clients ignore);
  a breaking change is `/v2` for that resource.

## Methods & status codes

| Operation            | Method   | Success                                  |
|----------------------|----------|------------------------------------------|
| Create (client id)   | `POST`   | `201` (replay with same id → same body)  |
| Read one             | `GET`    | `200` / `404` (also for a foreign tenant)|
| Read list            | `GET`    | `200` + `PageDto` (empty page, not 404)  |
| Partial update       | `PATCH`  | `200` + the new DTO (new `version`)      |
| Delete               | `DELETE` | `204`                                    |
| Transition / action  | `POST`   | `200` + DTO, or `202` when async         |

The live surface (method, path, auth, limiter) is pinned by a test against the running routing
tree — [`architecture-fitness-tests`](../../architecture-fitness-tests/SKILL.md), "API surface".
