# Schema invariants against `pg_catalog`

Run on the Testcontainers database after migrations, with an **admin** connection (catalog reads),
while the app itself stays on the runtime role. Each rule is one query returning offenders; the test
asserts the list is empty (or equals a pinned `KNOWN_*`).

| Rule                                                          | Query idea                                                                 |
|---------------------------------------------------------------|----------------------------------------------------------------------------|
| every policy table has ENABLE **and** FORCE                   | `pg_policies` ⋈ `pg_class` where `NOT relrowsecurity OR NOT relforcerowsecurity` |
| no table FORCEd without ENABLE                                | `pg_class` where `relforcerowsecurity AND NOT relrowsecurity`              |
| tenant tables with `id` have PK/UNIQUE `(tenant_id, id)`      | `pg_constraint.conkey` = `[tenant_id, id]`                                 |
| FKs between tenant tables include `tenant_id`                 | `pg_constraint contype='f'` whose columns lack `tenant_id`                 |
| every cascading FK has an index serving it                    | FK columns not a prefix of any `pg_index.indkey`                           |
| no index is a strict prefix of another on the same table      | compare `indkey` arrays pairwise                                           |
| no timestamp filled by the database clock                     | `information_schema.columns` with `column_default LIKE 'now()%'`           |
| `version` columns are `bigint NOT NULL DEFAULT 1`             | `information_schema.columns` where `column_name='version'`                 |
| append-only journals grant no UPDATE/DELETE to the runtime role | `has_table_privilege('app_runtime', t, 'UPDATE')` per journal          |
| a history-keeping table lets the runtime role UPDATE one column only | `has_column_privilege('app_runtime', t, col, 'UPDATE')` over `pg_attribute` |
| every uuid column other than `id` is a FK, or in `KNOWN_UNREFERENCED_IDS` | `pg_attribute` uuid columns not in any `pg_constraint contype='f'` `conkey` |
| every `text` column has a ceiling, or is in `KNOWN_UNBOUNDED_TEXT` | text (or text-based DOMAIN) columns with no `char_length` CHECK, no closed list, no DOMAIN CHECK |
| a rule used by more than one column is a DOMAIN               | identical `pg_get_constraintdef` across tables                             |
| every DOMAIN is used by more than one column                  | `information_schema.column_domain_usage` counts                            |
| exactly the exempt tables stay outside RLS                    | tenant tables without policy == pinned set **and its size** (see postgres-multitenancy-rls) |

Example:

```kotlin
test("every cascading foreign key has an index that serves it") {
    adminConnection().use { c ->
        c.queryNames(
            """
            SELECT con.conrelid::regclass || '.' || con.conname
            FROM pg_constraint con
            WHERE con.contype = 'f' AND con.confdeltype = 'c'
              AND NOT EXISTS (
                SELECT 1 FROM pg_index i
                WHERE i.indrelid = con.conrelid
                  AND (i.indkey::int2[])[0:array_length(con.conkey, 1) - 1] = con.conkey)
            ORDER BY 1
            """.trimIndent(),
        ).shouldBeEmpty()
    }
}
```

Every `KNOWN_*` here is a `Map<table.column, reason>`: an unbounded text column is fine when it
is a server-written hash, not when a client can fill it. The text-ceiling rule is the schema half of
a text policy kept in the domain — a ceiling the domain enforces and the schema does not is a ceiling
a second writer (a job, a migration, an operator script) can skip. Column-level grants pin
"history is kept": a consent is withdrawn by stamping `withdrawn_at`, never rewritten.

Template: [`../templates/server/SchemaBaselineTest.kt`](../templates/server/SchemaBaselineTest.kt).
The conventions these queries pin (DOMAIN over repeated CHECK, no database-clock defaults, append-only
grants) are written down in [`exposed-postgres` → flyway-migrations](../../exposed-postgres/references/flyway-migrations.md);
the domain half of the text ceilings in [`security-hardening-baseline` → text-policy](../../security-hardening-baseline/references/text-policy.md).
JSONB shapes (golden samples per stored variant) are pinned by the [`format-locks`](../../format-locks/SKILL.md) skill.

The test checks the **final** schema, not how the migrations got there: editing a baseline in place
versus adding a versioned migration, the schema fingerprint and the reset deploy are
[`pre-deploy-schema-policy`](../../pre-deploy-schema-policy/SKILL.md).

Why at the database level: the schema is edited by migrations written by different people and
agents over months; a rule checked against `pg_catalog` holds regardless of how the SQL was written.

Evidence: family-planner `SchemaBaselineTest` (step 76 S4/S9: `KNOWN_UNREFERENCED_IDS`,
`KNOWN_UNBOUNDED_TEXT`; step 81: audit journals and `child_consent` grants; "exactly five tables
stay outside RLS").
