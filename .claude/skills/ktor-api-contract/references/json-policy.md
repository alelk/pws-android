# The JSON policy — declared once

```kotlin
// api:contract/common/ApiJson.kt
val ApiJson: Json = Json {
    ignoreUnknownKeys = true   // additive evolution: a newer peer's extra field costs nothing
    encodeDefaults = true      // defaulted fields are on the wire; {"type":"resync"} keeps its fields
    explicitNulls = false      // in RESPONSES absent == null; PATCH tri-state lives in OptionalFieldDto
    // isLenient deliberately NOT set (Ktor's DefaultJson turns it on) — a strict contract has no
    // reason to accept unquoted literals.
    // classDiscriminator deliberately NOT set — it stays "type", and every sealed DTO says so
    // itself with @JsonClassDiscriminator("type"). A rule that holds by coincidence does not hold.
}
```

Used by: server `ContentNegotiation`, the JWT challenge body, SSE payloads, the client's
`ContentNegotiation`, and tests. A fitness test asserts that `Json {` appears in exactly one file
across contract, client and transport.

## Why one policy matters

Two policies drift, and the drift is invisible until an older client meets a newer server. The
classic failure: the server uses Ktor's default `Json` (does **not** ignore unknown keys) while the
client ignores them — rule one of additive evolution silently becomes "a new field is a 400".

## Evolution rules this enables

- Adding an optional field with a default: compatible both ways.
- Adding a sealed variant: compatible for clients that treat unknown variants as "ignore/resync"
  (decode failures are mapped to a typed `Malformed` error, never a crash).
- Renaming a field or a `@SerialName`, changing a type, making a field required: breaking → new
  resource version.
- Renaming an **enum constant** is a rename of a wire value (mapping writes `.name`) — and of the
  stored column and its `CHECK`. Pin the values in `wire-vocabulary.txt` so the rename is red
  (`architecture-fitness-tests` → `references/wire-vocabulary.md`; full recipe: [`format-locks`](../../format-locks/SKILL.md)).
