# Kotlin Coroutines / iOS Interop

A Kotlin Multiplatform showcase that makes coroutine behaviour visible. Concepts like structured
concurrency, cancellation and error propagation are usually explained with diagrams or log output;
here they drive real UI — progress bars, live task state, cancel buttons — running natively on both
Android and iOS.

The bridge to Swift is [SKIE](https://touchlab.co/skie), which turns Kotlin `StateFlow` into
`AsyncSequence` and `suspend` functions into `async` functions, so the iOS side reads as ordinary
Swift concurrency rather than callback plumbing.

See [PLAN.md](./PLAN.md) for what is built and what is next.

## Structure

```
shared/       # KMP library — all business logic
composeApp/   # Android app (Jetpack Compose)
iosApp/       # iOS app (SwiftUI, Xcode project)
```

`shared` produces a `Shared.xcframework` that the Xcode project consumes. Business logic lives there
in full; both UI layers are thin, observing state and calling functions on shared classes.

## Architectural choices

Several decisions here were made for the sake of the demonstration rather than for what would be
most convenient in a production app. They are worth stating explicitly, since in a few cases the
conventional choice would have quietly destroyed the thing being demonstrated.

### Native UI on each platform, not Compose Multiplatform

The Compose Multiplatform template this project grew from can render the same Compose UI on iOS.
Doing that would remove the entire point: with Kotlin UI reading Kotlin state directly there is no
language boundary, so nothing about interop is being shown. Android uses Compose and iOS uses
SwiftUI, and the shared module deliberately exposes no UI at all.

For this reason `App.kt` lives in `composeApp/androidMain` rather than `commonMain`, and
`composeApp` declares only an Android target.

## Build and run

```bash
# Android debug APK
./gradlew :composeApp:assembleDebug

# All tests
./gradlew test

# Shared module tests only
./gradlew :shared:test

# iOS framework — rebuild after any change to shared/
./gradlew :shared:assembleSharedReleaseXCFramework
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode and run from there, rebuilding the framework first
whenever `shared` has changed.
