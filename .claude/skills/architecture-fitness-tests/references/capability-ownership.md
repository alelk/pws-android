# Capability ownership: "only these files may …"

A capability with an invariant attached — take a lock first, seal the text first, write outside the
use-case's transaction, read the data as of a date — is legal only inside the components that
uphold the invariant. Everywhere else, the same call compiles and passes its unit tests while
skipping the invariant. These tests make the owners a closed set.

Template: [`../templates/domain/CapabilityOwnershipTest.kt`](../templates/domain/CapabilityOwnershipTest.kt).

## Four forms

| Form          | Question                                     | Pattern                                    | Example rule                                       |
|---------------|----------------------------------------------|--------------------------------------------|----------------------------------------------------|
| callers       | who **calls** `x(`?                          | `\.x\s*\(` (through a receiver)            | only the named use-cases keep their writes on a refusal |
| construction  | who **builds** `T(`?                         | `(?<![\w.])T\(` on comment-stripped text   | a sealed outbound text is made only by the gateway |
| knowledge     | who **names** `Port` at all?                 | `\bPort\b` over all main sources, KDoc included | the audit port is unreachable from any use-case  |
| perimeter     | who **imports** context B from context A?    | `^import <pkg>\.b\.` per file              | analytics reads context B through exactly one leaf file |

Choose the strongest that holds. "Knows" beats "calls": a file that imports the port can start
calling it in the next commit; a KDoc mention counts because the rule is about who *knows*.

## Rules of the regex

- **Call vs declaration.** `\.name\s*\(` matches `runner.name(`, not `fun name(` nor a KDoc `[name]`.
  For top-level functions use `(?<![\w.])name\s*\(` and exclude the declaring file.
- **Whole word.** `\bAuditJournal\b` — `AuditJournalViewModel` is somebody else.
- **Strip comments** when the rule is about code (`/\*.*?\*/` with `DOT_MATCHES_ALL`, `//[^\n]*`);
  keep them when the rule is about knowledge.
- **Compare by equality** to a `Map<file, reason>`; the allowed set is part of the rule.
- **Non-vacuity both ways**: a floor on the scanned files, and each allowed owner is itself found
  (`ALLOWED_ROOTS.keys.forEach { root -> knowing.any { it.startsWith(root) } shouldBe true }`).
- **Scope the scan to the rule.** A domain-only rule scans `domain/src/commonMain`; a "nobody in the
  repository" rule walks every `src/*Main` from the repository root (skip `build/` and dot-dirs) and
  runs in the module whose working directory makes that path stable (e.g. `server/app`).

## Perimeters (data-flow)

`ContextDependencyTest` pins the *order* of contexts. A perimeter is finer: "context A never imports
B, **except** through exactly one leaf file", compared by equality (`readers shouldBe setOf("x/BFactsOf.kt")`).
Opening the door later is done by naming a second leaf — a visible decision — never by deleting the
test. Add sibling assertions for what must not cross even through the leaf (the snapshot type has no
word for B; the external-model ports and adapters never name B's types).

**Wire variant.** When the perimeter is a projection per audience (a dependant sees fewer fields than
an adult), assert the **keys** of the JSON answer per role (`DEPENDANT_KEYS` vs `ADULT_KEYS`) on a fixture
where **every field is non-zero** — a field the projection forgets cannot hide as a default zero.

## Worked examples in other skills

| Form | Test | Skill |
|------|------|-------|
| callers | `CommitsOnLeftTest` | [`kotlin-domain-modeling`](../../kotlin-domain-modeling/templates/domain/CommitsOnLeftTest.kt) |
| callers (one file owns a call) | `LedgerGuardsTest` — only the debit component appends to the ledger or locks the wallet | [`ledger-and-wallet`](../../ledger-and-wallet/templates/test/LedgerGuardsTest.kt) |
| construction | `OutboundTextConstructionTest`, `OutboundBoundaryTest` | [`ai-egress-gateway`](../../ai-egress-gateway/templates/test/OutboundTextConstructionTest.kt) |
| knowledge | `AuditPortCallersTest` — the write port is named only by the transport door | [`audit-journal-at-the-door`](../../audit-journal-at-the-door/references/locks.md) |
| perimeter | `PersonalScopeParityTest` — one definition of "a member's own" | [`tenant-data-lifecycle`](../../tenant-data-lifecycle/templates/test/PersonalScopeParityTest.kt) |

## Same pattern elsewhere

- "Where a word may come from": a UI resource family read by a single file, a removed-keys list, a
  closed `SYNONYMS` table (`compose-design-system`).
- "Who may read the cross-tenant discovery pool": callers compared by function name
  (`outbox-and-background-jobs`).

Evidence: family-planner `CommitsOnLeftTest` (ADR 0087), `OutboundTextConstructionTest` (ADR 0077),
`AuditPortCallersTest` (ADR 0096), `HabitPerimeterTest` (ADR 0085), `DigestChildSurfaceTest`,
`JobsDiscoveryReaderTest`.
