package io.github.myorg.myapp.infra

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.sql.Connection
import java.sql.DriverManager

/**
 * Structural invariants of the schema, asserted against pg_catalog after migrations. Each rule is
 * one query returning offenders; the expected set is empty or a pinned/shrinking KNOWN list.
 */
class SchemaBaselineTest :
    BaseRepositoryTest({
        test("every tenant policy is backed by enabled and forced RLS") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT policy.tablename
                        FROM pg_policies policy
                        JOIN pg_class table_class ON table_class.relname = policy.tablename
                        JOIN pg_namespace namespace ON namespace.oid = table_class.relnamespace
                        WHERE policy.schemaname = 'public'
                          AND policy.policyname = 'tenant_isolation'
                          AND namespace.nspname = 'public'
                          AND (NOT table_class.relrowsecurity OR NOT table_class.relforcerowsecurity)
                        ORDER BY policy.tablename
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        test("tenant tables with an id have tenant-scoped identity") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        WITH tenant_tables AS (
                            SELECT DISTINCT table_class.oid, table_class.relname::text AS table_name
                            FROM pg_class table_class
                            JOIN pg_namespace namespace ON namespace.oid = table_class.relnamespace
                            JOIN information_schema.columns tenant_column
                              ON tenant_column.table_schema = namespace.nspname
                             AND tenant_column.table_name = table_class.relname
                             AND tenant_column.column_name = 'tenant_id'
                            JOIN information_schema.columns id_column
                              ON id_column.table_schema = namespace.nspname
                             AND id_column.table_name = table_class.relname
                             AND id_column.column_name = 'id'
                            JOIN pg_policies policy
                              ON policy.schemaname = namespace.nspname
                             AND policy.tablename = table_class.relname
                             AND policy.policyname = 'tenant_isolation'
                            WHERE namespace.nspname = 'public' AND table_class.relkind = 'r'
                        )
                        SELECT tenant.table_name
                        FROM tenant_tables tenant
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM pg_constraint constraint_record
                            WHERE constraint_record.conrelid = tenant.oid
                              AND constraint_record.contype IN ('p', 'u')
                              AND (
                                  SELECT array_agg(attribute.attname::text ORDER BY key_column.ordinality)
                                  FROM unnest(constraint_record.conkey)
                                       WITH ORDINALITY AS key_column(attnum, ordinality)
                                  JOIN pg_attribute attribute
                                    ON attribute.attrelid = constraint_record.conrelid
                                   AND attribute.attnum = key_column.attnum
                              ) = ARRAY['tenant_id', 'id']
                        )
                        ORDER BY tenant.table_name
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        test("every version column is a non-null bigint defaulting to one") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT table_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND column_name = 'version'
                          AND table_name <> 'flyway_schema_history'
                          AND (data_type <> 'bigint' OR is_nullable <> 'NO' OR column_default <> '1')
                        ORDER BY table_name
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        // A foreign key between two tenant-scoped tables carries tenant_id, so a row cannot point
        // into another tenant's data. Without this, tenant isolation would rest on the repository
        // layer alone (multi-tenancy.md, layer 2) with nothing under it.
        test("a foreign key between two tenant-scoped tables carries tenant_id") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT child.relname || '.' || fk.conname
                        FROM pg_constraint fk
                        JOIN pg_class child ON child.oid = fk.conrelid
                        JOIN pg_class parent ON parent.oid = fk.confrelid
                        JOIN pg_namespace ns ON ns.oid = child.relnamespace AND ns.nspname = 'public'
                        WHERE fk.contype = 'f'
                          AND parent.relname <> 'tenant'
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a
                              WHERE a.attrelid = fk.conrelid AND a.attname = 'tenant_id' AND a.attnum > 0)
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a
                              WHERE a.attrelid = fk.confrelid AND a.attname = 'tenant_id' AND a.attnum > 0)
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_attribute a
                              WHERE a.attrelid = fk.conrelid AND a.attname = 'tenant_id'
                                AND a.attnum = ANY (fk.conkey))
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        // FORCE with RLS not enabled is a no-op that reads as protection: it holds the table
        // owner to policies that never run, because ENABLE is the switch and FORCE only extends
        // it to the owner.
        test("no table is forced into row-level security without having it enabled") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT table_class.relname
                        FROM pg_class table_class
                        JOIN pg_namespace ns ON ns.oid = table_class.relnamespace AND ns.nspname = 'public'
                        WHERE table_class.relkind = 'r'
                          AND table_class.relforcerowsecurity
                          AND NOT table_class.relrowsecurity
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        // Deleting the parent of a cascading key makes Postgres scan the child looking for rows
        // to remove; without an index that scan reads the whole child table on every delete.
        test("every cascading foreign key has an index that serves it") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT child.relname || '.' || fk.conname
                        FROM pg_constraint fk
                        JOIN pg_class child ON child.oid = fk.conrelid
                        JOIN pg_namespace ns ON ns.oid = child.relnamespace AND ns.nspname = 'public'
                        WHERE fk.contype = 'f'
                          AND fk.confdeltype <> 'a'
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_index ix
                              WHERE ix.indrelid = fk.conrelid
                                AND ix.indpred IS NULL
                                AND (ix.indkey::smallint[])[0:array_length(fk.conkey, 1) - 1] @> fk.conkey
                                AND fk.conkey @> (ix.indkey::smallint[])[0:array_length(fk.conkey, 1) - 1])
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_index ix
                              JOIN pg_attribute lead
                                ON lead.attrelid = ix.indrelid
                               AND lead.attnum = (ix.indkey::smallint[])[0]
                              WHERE ix.indrelid = fk.conrelid
                                AND lead.attname <> 'tenant_id'
                                AND lead.attnum = ANY (fk.conkey)
                                AND (ix.indpred IS NULL
                                     OR pg_get_expr(ix.indpred, ix.indrelid)
                                        = '(' || lead.attname || ' IS NOT NULL)'))
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        // Times come from the application's Clock, never from the database: a column default is
        // a second clock that no test with a fixed instant controls.
        test("no timestamp column is filled by the database clock") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT table_name || '.' || column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND data_type = 'timestamp with time zone'
                          AND column_default IS NOT NULL
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        // A narrower index whose columns are a prefix of a wider one on the same table serves
        // every query the narrow one would, so the narrow one only costs writes and disk.
        test("no index is a strict prefix of another index on the same table") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        WITH indexes AS (
                            SELECT ix.indrelid,
                                   table_class.relname AS table_name,
                                   index_class.relname AS index_name,
                                   (SELECT array_agg(column_number)
                                      FROM unnest(ix.indkey::smallint[]) column_number) AS columns,
                                   ix.indisunique,
                                   coalesce(pg_get_expr(ix.indpred, ix.indrelid), '') AS predicate
                            FROM pg_index ix
                            JOIN pg_class table_class ON table_class.oid = ix.indrelid
                            JOIN pg_class index_class ON index_class.oid = ix.indexrelid
                            JOIN pg_namespace ns
                              ON ns.oid = table_class.relnamespace AND ns.nspname = 'public'
                            WHERE ix.indexprs IS NULL
                        )
                        SELECT narrow.table_name || '.' || narrow.index_name
                        FROM indexes narrow
                        JOIN indexes wide
                          ON wide.indrelid = narrow.indrelid
                         AND wide.index_name <> narrow.index_name
                        WHERE narrow.indisunique = false
                          AND narrow.predicate = wide.predicate
                          AND array_length(narrow.columns, 1) < array_length(wide.columns, 1)
                          AND narrow.columns = wide.columns[1:array_length(narrow.columns, 1)]
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldBeEmpty()
            }
        }

        test("exactly the pinned tenant-scoped tables stay outside row-level security") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT table_class.relname
                        FROM pg_class table_class
                        JOIN pg_namespace ns ON ns.oid = table_class.relnamespace AND ns.nspname = 'public'
                        WHERE table_class.relkind = 'r'
                          AND EXISTS (
                              SELECT 1 FROM pg_attribute a
                              WHERE a.attrelid = table_class.oid AND a.attname = 'tenant_id' AND a.attnum > 0)
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_policies p
                              WHERE p.schemaname = 'public'
                                AND p.tablename = table_class.relname
                                AND p.policyname = 'tenant_isolation')
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(TENANT_TABLES_OUTSIDE_RLS)
            }
        }

        test("append-only journals grant no update and no delete to the runtime role") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT journal.table_name || '.' || privilege.kind
                        FROM (VALUES ('points_entry'), ('audit_log')) AS journal(table_name)   -- your append-only journals
                        CROSS JOIN (VALUES ('UPDATE'), ('DELETE')) AS privilege(kind)
                        WHERE has_table_privilege('app_runtime', 'public.' || journal.table_name, privilege.kind)
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(emptySet())
            }
        }

        // A journal whose history matters may let the application stamp ONE column (a withdrawal time)
        // and nothing else: pin the column-level grant, not just the table-level one.
        test("a history-keeping table grants update of its closing column alone to the runtime role") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT 'consent.' || a.attname || '=' ||
                               has_column_privilege('app_runtime', 'public.consent', a.attname, 'UPDATE')
                        FROM pg_attribute a
                        WHERE a.attrelid = 'public.consent'::regclass AND a.attnum > 0 AND NOT a.attisdropped
                        ORDER BY 1
                        """.trimIndent(),
                    ).filter { it.endsWith("=true") }
                    .shouldMatch(setOf("consent.withdrawn_at=true"))
            }
        }

        // A uuid that names a row of this database is a foreign key — or a reason in the map.
        test("every uuid column other than id is part of a foreign key, or is named in KNOWN_UNREFERENCED_IDS") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT rel.relname || '.' || a.attname
                        FROM pg_attribute a
                        JOIN pg_class rel ON rel.oid = a.attrelid AND rel.relkind = 'r'
                        JOIN pg_namespace ns ON ns.oid = rel.relnamespace AND ns.nspname = 'public'
                        WHERE a.attnum > 0 AND NOT a.attisdropped
                          AND a.atttypid = 'uuid'::regtype AND a.attname <> 'id'
                          AND rel.relname <> 'flyway_schema_history'
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_constraint k
                              WHERE k.contype = 'f' AND k.conrelid = rel.oid AND a.attnum = ANY (k.conkey))
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(KNOWN_UNREFERENCED_IDS.keys)
            }
        }

        // A ceiling the domain keeps and the schema does not is what this catches when a column arrives:
        // a length CHECK, a closed list, or a DOMAIN with a CHECK — or a reason in the map.
        test("every text column has a ceiling, or is named in KNOWN_UNBOUNDED_TEXT") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT rel.relname || '.' || a.attname
                        FROM pg_attribute a
                        JOIN pg_class rel ON rel.oid = a.attrelid AND rel.relkind = 'r'
                        JOIN pg_namespace ns ON ns.oid = rel.relnamespace AND ns.nspname = 'public'
                        JOIN pg_type t ON t.oid = a.atttypid
                        WHERE a.attnum > 0 AND NOT a.attisdropped
                          AND rel.relname <> 'flyway_schema_history'
                          AND (a.atttypid = 'text'::regtype OR (t.typtype = 'd' AND t.typbasetype = 'text'::regtype))
                          AND NOT EXISTS (SELECT 1 FROM pg_constraint d WHERE d.contypid = a.atttypid AND d.contype = 'c')
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_constraint k
                              WHERE k.contype = 'c' AND k.conrelid = rel.oid AND a.attnum = ANY (k.conkey)
                                AND (pg_get_constraintdef(k.oid) ~ ('(char_length|length)\(\(?' || a.attname || '\M')
                                  OR pg_get_constraintdef(k.oid)
                                     ~ ('\(?' || a.attname || '\)?(::text)? = (ANY \(ARRAY|''[^'']*''::text)')))
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(KNOWN_UNBOUNDED_TEXT.keys)
            }
        }

        test("a rule used by more than one column is a domain, not a repeated check") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT count(*)::text || ' tables: '
                             || string_agg(rel.relname, ', ' ORDER BY rel.relname)
                             || ' -> ' || pg_get_constraintdef(con.oid)
                        FROM pg_constraint con
                        JOIN pg_class rel ON rel.oid = con.conrelid
                        JOIN pg_namespace ns
                          ON ns.oid = rel.relnamespace AND ns.nspname = 'public'
                        WHERE con.contype = 'c'
                        GROUP BY pg_get_constraintdef(con.oid)
                        HAVING count(*) > 1
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(REPEATED_TABLE_CHECKS)
            }
        }

        test("every domain is used by more than one column") {
            adminConnection().use { connection ->
                connection
                    .queryNames(
                        """
                        SELECT d.typname || ' (' || count(*) || ')'
                        FROM pg_type d
                        JOIN pg_namespace dn
                          ON dn.oid = d.typnamespace AND dn.nspname = 'public'
                        JOIN pg_attribute a
                          ON a.atttypid = d.oid AND a.attnum > 0 AND NOT a.attisdropped
                        JOIN pg_class rel ON rel.oid = a.attrelid AND rel.relkind = 'r'
                        WHERE d.typtype = 'd'
                        GROUP BY d.typname
                        ORDER BY 1
                        """.trimIndent(),
                    ).shouldMatch(
                        setOf(
                            // "catalog_name (4)",   // pin today's domains and their usage counts
                        ),
                    )
            }
        }
    })

private fun adminConnection(): Connection =
    DriverManager.getConnection(BaseRepositoryTest.containerJdbcUrl(), "admin", "admin")

private fun Connection.queryNames(sql: String): List<String> =
    createStatement().use { statement ->
        statement.executeQuery(sql).use { rows ->
            buildList {
                while (rows.next()) add(rows.getString(1))
            }
        }
    }

/** Exact-equality comparison against a shrinking list, printing the actual set on failure. */
private fun List<String>.shouldMatch(known: Set<String>) {
    withClue("Actual:\n${joinToString("\n")}") {
        toSortedSet() shouldBe known.toSortedSet()
    }
}

/**
 * Read before the tenant is known, so a fail-closed policy on `app.tenant_id` could not be
 * satisfied. Same names as TenantIsolationTest, each explained there. PINNED, not shrinking: a new
 * name has to be a decision ("its purpose is to turn an identifier into a tenant").
 */
private val TENANT_TABLES_OUTSIDE_RLS =
    setOf(
        "invitation",
        "refresh_session",
    )

/**
 * Cross-column rules a DOMAIN cannot express (it sees one value; these relate two), pinned with
 * the tables that repeat them. Everything else that repeats across tables should become a domain.
 */
private val REPEATED_TABLE_CHECKS =
    setOf<String>(
        // "3 tables: backlog, task, task_series -> CHECK (((project_id IS NULL) OR (goal_id IS NULL)))",
    )

/** uuid columns that are deliberately not foreign keys — column → why. Shrinks or carries a reason. */
private val KNOWN_UNREFERENCED_IDS: Map<String, String> =
    mapOf(
        // "outbox.actor_id" to "the journal outlives whoever acted; an actor may be a member, the system or a device",
    )

/** text columns without a ceiling — column → why. A new unbounded column is a decision. */
private val KNOWN_UNBOUNDED_TEXT: Map<String, String> =
    mapOf(
        // "refresh_session.token_hash" to "fixed-length hash written by the server, never by a client",
    )
