# R8 shrinking discipline

R8 does three things to a release build: **shrinks** (removes unreachable classes/members),
**obfuscates** (renames what is left) and **optimizes** (inlines, merges, devirtualizes). A keep rule
switches these off for what it matches. A wildcard keep switches them off for a whole tree — and
because it "fixes" every release-only crash at once, it is the rule an agent reaches for first.

*Mine:* a release build that passes every test and installs fine, but ships most of its libraries
unshrunk and unobfuscated — or, after a careless narrowing, loses users' stored data on update.

## What a wildcard keep costs (measured)

pws-android, before the rewrite (`docs/ai/plans/2026-09-10_app-optimization-r8_plan.md`):

| Play Console metric          | Value       |
|------------------------------|-------------|
| Optimization / Obfuscation / Shrinking | **18% / 18% / 19%** ("App optimization: Low") |
| Total uncompressed DEX       | **36.8 MB** (32 087 classes, 151 693 methods) |
| `string_data` section        | 8.56 MB — names that were never obfuscated |
| `androidx.compose.material.icons` | 11 400 classes, ~9.25 MB of code — **one** icon used |

Cause: eleven `-keep class <pkg>.** { *; }` lines (`kotlin`, `kotlinx`, `androidx.compose`,
`androidx.datastore`, `androidx.security.crypto`, `org.koin`, `cafe.adriel.voyager`,
`net.zetetic.database` and three own packages). A library **not** covered by any keep (Lucide icons,
~1 500 icons) shrank to 39 classes in the same APK — R8 worked; it had been forbidden.

## Rule kit — narrowest first

| Need                                                         | Rule                                                            | R8 still may              |
|--------------------------------------------------------------|-----------------------------------------------------------------|---------------------------|
| Class name is data (analytics screen name, stored FQCN)      | `-keepnames class * implements x.Screen`                        | remove unused, optimize   |
| Fields read by name (protobuf-lite, some JSON libs)          | `-keepclassmembers class * extends x.GeneratedMessageLite { <fields>; }` | remove/rename the class |
| Member exists only if class exists (serializer companions)   | `-if @x.Serializable class ** -keepclassmembers class <1> { … }` | remove unused classes     |
| Reflective construction by convention (`<FQCN>_Impl`)        | `-keep class * extends androidx.room.RoomDatabase { <init>(); }` | rename members           |
| Kept from rename, not from removal                           | `-keep,allowshrinking class …` (= `-keepnames`)                  | remove unused             |
| JNI: native code calls Java by name                          | `-keep class <jni.pkg>.** { *; }` — the one justified wide keep, in `KNOWN_WIDE_KEEPS` | nothing |
| Optional transitive class missing at build                    | `-dontwarn <pkg>.**` copied from `missing_rules.txt`            | everything                |

Before writing any rule, check whether the library already ships **consumer rules** in its AAR
(`configuration.txt` shows the merged set): coroutines, Room runtime, Compose, Koin and most SDKs do.
Duplicating them only adds risk of widening.

## The diagnostic files

AGP writes them to `build/outputs/mapping/<variant>/` on every minified build:

| File                | Answers                                                         |
|---------------------|-----------------------------------------------------------------|
| `seeds.txt`         | what keep rules hold — the **proof** file; a huge one means a wide rule |
| `usage.txt`         | what was removed — "is my class gone?"                          |
| `mapping.txt`       | what was renamed to what — "is my name changed?"; needed for retrace |
| `configuration.txt` | the final merged config, including every library's consumer rules |
| `missing_rules.txt` | `-dontwarn` suggestions when the build fails with "Missing class" |

## When something breaks only in release

```
Build fails "Missing class …"
  → copy ONLY the -dontwarn lines from missing_rules.txt, with a comment saying where they come from

ClassNotFound / NoSuchMethod / NoSuchField at runtime
  → grep the name in usage.txt   (removed?)  → -keep class <FQCN> { <that member>; }
  → grep the name in mapping.txt (renamed?)  → -keepnames class <FQCN>          ← preferred

SerializationException "Serializer for class X not found"
  → the -if rules did not apply to X → -keepclassmembers class X { *** Companion; kotlinx.serialization.KSerializer serializer(...); }

A class name ended up in data (logs, analytics, keys, stored files)
  → -keepnames on the exact class or a narrow `implements` pattern

ALWAYS: no `-keep class <pkg>.** { *; }`; a comment above the rule (what broke, how it showed, why
this fixes it); rebuild and re-measure.
```

Deobfuscate a crash: `retrace build/outputs/mapping/<variant>/mapping.txt crash.txt`.

## Changing rules safely

1. **Measure first**: build the release APK of every flavor, keep copies outside `build/`, record
   uncompressed DEX size, classes, methods (Play's numbers come from the same DEX). Save `seeds.txt`.
2. **Replace the file as a whole** when it is a pile of overlapping wide rules; editing line by line
   leaves you unable to tell which rule fixed what.
3. One change per commit (rules, dependency removal, resource filtering) and a measurement after
   each — so `git revert` undoes exactly one step.
4. Build **every flavor** — a flavor-only SDK has its own rules (`proguardFiles` inside the flavor).
5. Unit tests run on the non-minified debug variant: they must stay green but **cannot** catch R8
   damage. Smoke-test the release build on a device, including **an upgrade over the published
   version**: renamed protobuf-lite fields don't crash a fresh install — they silently drop
   DataStore settings, or make EncryptedSharedPreferences (and a DB passphrase stored in it)
   unreadable for existing users.
6. Roll out staged (e.g. 10%) and watch crash-free rate for ~48 h; an R8 data-loss bug cannot be
   rolled back by a new upload once users have updated.

## Locks

| Lock                                            | Runs in                          | Catches                                    |
|-------------------------------------------------|----------------------------------|--------------------------------------------|
| Header ban + "deliberately absent" section      | the rule file                    | the next reader's first impulse            |
| `ProguardRulesTest` ([template](../templates/ProguardRulesTest.kt)) | `./gradlew build` (unit test) | a new wildcard `-keep`, a vanished known one |
| DEX-size gate (`--max-dex-mb`, threshold = measured + ~25%) | release CI job        | growth from any cause (new library, consumer rules, resources) |
| `AGENTS.md` hard rule linking the test and the plan | agent context                  | an agent proposing the wide keep at all    |

The fitness test is a source-level lock; the DEX gate is the artifact lock (same principle as
"verify the artifact, not the sources" in
[`gradle-release-system`](../../gradle-release-system/references/release-artifact-verification.md)).
In pws-android the agent gate excludes `assemble*Release` (no signing in the sandbox), so the
fitness test is the only R8-related lock most changes ever meet — keep it.

## Worth doing alongside

- Remove dependencies pulled for one symbol (pws-android: `material-icons-extended` for a single
  heart icon → copy the vector). Even when R8 would drop it, any future wide keep brings it back.
- `androidResources { localeFilters += listOf(…) }` (AGP 9; AGP 8: `resourceConfigurations`) —
  synchronized with the locales the app really ships.
- Archive `mapping.txt` per variant as `mapping-<variant>-<versionName>-<versionCode>.txt` (pws-android
  `stageAppMetricaMapping<Variant>` via `SingleArtifact.OBFUSCATION_MAPPING_FILE`).
