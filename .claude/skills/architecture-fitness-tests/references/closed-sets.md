# Closed-set guards

A sealed hierarchy or an enum that other code must **classify** (a ledger source -> spent /
penalized / reversed; a period -> its audiences; an audit kind -> its journal) is safe only while
every reader is a `when` without `else`: then adding a variant is a compile error at every site that
does not know it. These tests protect that property.

Template: [`../templates/domain/ClosedSetTest.kt`](../templates/domain/ClosedSetTest.kt).

| Guard                                                        | How                                                                     |
|--------------------------------------------------------------|-------------------------------------------------------------------------|
| the test's table == the variants                             | `EXPECTED.keys.map { it::class }.toSet() shouldBe T::class.sealedSubclasses.toSet()`; enums: `T.entries` |
| each variant has its class                                   | iterate `EXPECTED`, assert the classifier's answer                      |
| the classifier's `when` has no `else`                        | read the function's source slice, `Regex("""\belse\s*->""")` must not match |
| nobody compares a value                                      | `[!=]=\s*T\.` over the readers' sources (UI included) — an `if (x == A) … else …` puts the new value in the `else` |
| nobody lists values by hand                                  | `listOf\(\s*T\.\w+\s*[,)]` — the one declared table (`T_VARIANTS`) is the list |
| a sample used by SQL covers every variant                    | `ONE_OF_EACH.map { it::class }.toSet() shouldBe T::class.sealedSubclasses.toSet()` when a query builds its `IN (…)` from the classifier |
| wire/stored names of the set are pinned                      | see [`wire-vocabulary.md`](wire-vocabulary.md)                          |

Report both directions in the failure (`unknown: …; vanished: …`). The point of the test is the
moment a variant is added: it must be red **until someone decides** what the new variant is.

**A declared table instead of scattered lists.** When code needs "all periods" or "which audiences
each period has", declare one `val PERIOD_VARIANTS: Map<Period, Set<Audience>>` built by a `when`
without `else`, and forbid hand lists elsewhere. A hand list is a classification nobody re-reads.

Mine it prevents: a new negative ledger source counted as "spent" by an `else`; a third period
falling into the `else` of ten `if (period == Daily)` sites — a monthly report with a weekly subject line.

Evidence: family-planner `DebitClassTest` (ADR 0071), `DigestPeriodExhaustiveTest` (step 74, ADR 0084,
`DIGEST_VARIANTS`), `AuditVocabularyTest` (step 81).
