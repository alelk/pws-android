# PWS Android - AI Agent Guide

Use this guide for fast onboarding and operational standards for the `pws-android` repository.

## Project Overview
- **Repository Role**: Android host for the PWS application.
- **Primary Module**: `:app-compose` (Jetpack Compose, Voyager, Koin).
- **Legacy Module**: `:app` (View-based). **Do not use for new features.**
- **Business Logic**: Most features, domain logic, and data repositories reside in the `pws-core` repository.

## Environment & Build
- **Composite Build**: Automatically links `../pws-core` if present. Always verify logic changes in `pws-core` first.
- **Build Script**: `build.sh` is available for command-line builds.
- **Flavors**: 
  - `ru`: Russian content (default package).
  - `uk`: Ukrainian content (suffix `.uk`).
  - `full`: Combined content (suffix `.full`).
  - `rustore`: RuStore specific (package `io.github.alelk.pws.app`).
- **JDK**: 21 required.

## Technical Architecture (:app-compose)

### UI and Navigation
- **Framework**: Jetpack Compose.
- **Navigation**: [Voyager](https://voyager.adriel.cafe/).
- **Root**: `AppRoot` (from `pws-core`) is the entry point for the UI tree.

### Dependency Injection
- **Koin**: Initialized in `PwsComposeApplication`.
- **Scope**: ScreenModels are typically scoped to Voyager screens.

### Data & State
- **State Holders**: `StateScreenModel` (from Voyager/pws-core).
- **Database**: Room. The actual DB implementation is in `pws-core`, but the provider is in `:data:db-android`.
- **Preferences**: DataStore is used for app settings (Theme, Font Scale, etc.).

## Common Implementation Patterns

### 1. External Actions (Android Interop)
Since features are in `pws-core` (platform-agnostic), Android-specific actions like "Share", "Send Email", or "Open URL" are passed via `ExternalActions` interfaces.
- See `MainActivity.kt` for `SettingsExternalActions` and `SongDetailExternalActions` implementations.

### 2. Display Settings
User preferences (font size, sort order) are collected in `MainActivity` (using DataStore flows) and passed down to `AppRoot`.
- **Pattern**: `collectAsState` in `MainActivity` -> Pass to `AppRoot` -> Propagate to feature components.

### 3. Backup & Restore
Handled by `BackupManager` and `BackupService` (portable).
- Implementation in `app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/BackupManager.kt`.

## Code Navigation Shortcuts
- **Main Activity**: `app-compose/.../MainActivity.kt`
- **DI Modules**: `app-compose/.../PwsComposeApplication.kt`
- **E2E Tests**: `e2e/` (Maestro flows).
- **Resource Overrides**: `app-compose/src/main/res` (Use for Android-specific resources).

## Workflow Checklist

### Adding a Feature
- [ ] Create domain models/use cases in `pws-core:domain`.
- [ ] Implement UI state and ScreenModel in `pws-core:features`.
- [ ] If Android-specific APIs are needed, add a callback to `ExternalActions` in `pws-core`.
- [ ] Update `MainActivity.kt` in `pws-android` to implement the new callback.

### Building & Testing
- **Local APK**: `./gradlew :app-compose:assembleRuDebug`
- **E2E Smoke**: `./e2e/scripts/run-local.sh --flavor ru`
- **Release Info**: See `docs/release-workflow.md`.

## Best Practices
- **Prefer `pws-core`**: If logic can be platform-agnostic, keep it in `pws-core`.
- **Edge-to-Edge**: Always use `enableEdgeToEdge()` (already in `MainActivity`).
- **Semantics**: Use `Modifier.semantics { testTagsAsResourceId = true }` for Maestro testability.
- **Localization**: Keep translations in `pws-core` when possible, or `strings.xml` in `app-compose`.

Last reviewed: 2025-05-04
