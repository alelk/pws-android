# Wire vocabulary and error-code locks

Short reference. The full recipe — generating the vocabulary file, the stored-name/`CHECK` companion
and JSONB golden samples — is the [`format-locks`](../../format-locks/SKILL.md) skill.

## Enum values on the wire are data

Mapping writes `enum.name` and reads `valueOf`, so renaming a Kotlin constant changes the wire, the
column and its `CHECK` at once — and every test still passes, because both ends were renamed together.
Pin the values like the routes:

```
# wire-vocabulary.txt — every domain enum api:mapping touches, values in declaration order
ItemStatus: Pending, InProgress, Done
Role: Owner, Adult, Dependant
```

`WireVocabularyTest` (in the server transport module, which sees `api:mapping` and the domain on the
classpath) collects the `import <domain>.…` lines of `api:mapping`, resolves each by reflection
(nested: `Outer$Inner`), keeps the enums, renders `Name: A, B, C`, and compares with the file. The
failure prints the actual text to paste **if the change is intended** — editing the file is the decision.
The stored half (names in columns, `CHECK` lists) is pinned the same way against the schema.

## Error codes are constants, never literals

```kotlin
test("no ApiErrorDto is built with a literal code") {          // server transport
    sources.filter { Regex("ApiErrorDto\\(\\s*\"").containsMatchIn(it.readText()) }.shouldBeEmpty()
}
test("no screen reads info.message") {                          // UI module
    sources.filter { Regex("""\binfo\.message\b""").containsMatchIn(it.readText()) }.map { it.path }.toSet() shouldBe
        KNOWN_MESSAGE_READERS   // empty
}
```

Codes live in `ApiErrorCodes` and reasons in `ValidationReasons` in `api:contract`, read by server and
client alike ([`ktor-api-contract` / error-format.md](../../ktor-api-contract/references/error-format.md)).
The domain-side `ValidationReason` enum is translated by one exhaustive `when` (a closed set,
[`closed-sets.md`](closed-sets.md)). A screen branches on `code` + `details.reason`, never on `message`:
a server phrase edited for a log line must not change what a user sees.

Evidence: family-planner step 76 (A1, A2, A6): `WireVocabularyTest`, `VocabularyTest`,
`ErrorCodeLiteralTest`, `ErrorReasonTest`, `ApiErrorCodes.kt`.
