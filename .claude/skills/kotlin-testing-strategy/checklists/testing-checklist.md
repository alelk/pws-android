# Testing Checklist

## Coverage by layer

- [ ] Use-cases: happy path, each refusal, events, idempotent replay, stale version, foreign tenant
- [ ] State machines: every transition × every source status
- [ ] Repositories: round-trip, tenant scoping, CAS, duplicate id, pagination; on Testcontainers as the runtime role
- [ ] Routes: status + error code per class, auth, tenant from token, headers
- [ ] A few e2e flows across the whole stack with faked providers

## Special forms (where applicable)

- [ ] Read-decide-write: concurrent requests through the route (2 and 10) → exactly one wins
- [ ] Counters: N+1 attempts through HTTP; parallel failures counted correctly
- [ ] Uploads: refusal before reading (bytes read ≈ 0); real-byte fixtures at limit / limit+1
- [ ] Rules expressed twice: SQL vs domain predicate parity on a seeded matrix
- [ ] Batches: partial failure retries only the remainder; lost response reads as "already exists"
- [ ] AI: adversarial inputs; unparsable output → failed, never accepted
- [ ] Second principal kind: wall walk over the live routing tree, both directions, all `401`, floors, chunked under the strictest bucket
- [ ] Fail-open side effects: DB-trigger fault injection — same answers, action stood, `ERROR` logged; plus the pair with it working
- [ ] Use-cases that write then may refuse: on the real DB a late refusal leaves no row; kept writes are a named list, each tested to survive
- [ ] Erasure/export: census over every uuid/text/json column of the catalog; fixture fills every tenant table; neighbour unchanged

## Discipline

- [ ] The step's mine has numbered locks (`<stage> lock N guard / lock N / lock N pair`), each proven red on a broken implementation, recipe in KDoc
- [ ] Every protection has its pair: a stranger is refused, one's own is not
- [ ] Every bug fix starts with a failing test
- [ ] Fakes honour port contracts; time pinned; two tenants where isolation matters
- [ ] No weakened/skipped tests; no real provider calls in CI
- [ ] All tests run in `./gradlew build`
