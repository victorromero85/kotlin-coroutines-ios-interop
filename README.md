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

### A plain state-holder class, not `androidx.lifecycle.ViewModel`

`ProgressBarsController` is an ordinary Kotlin class that owns a `CoroutineScope`. The lifecycle
artifacts are multiplatform now, so extending `ViewModel` in `commonMain` and using `viewModelScope`
would work — but `viewModelScope` cancels its children off-screen, as a side effect of a lifecycle
callback. Cancellation propagating from parent to child is precisely what this project sets out to
show, so it should not happen somewhere the reader cannot see.

Keeping the class platform-free also means SwiftUI can hold it directly. SwiftUI has no
`ViewModelStoreOwner` to hand out a `ViewModel`, so a shared `ViewModel` would need a platform-specific
escape hatch on the iOS side.

The name reflects this: it is a controller, not an Android `ViewModel`, and calling it one would
mislead anyone reading the code to learn from it.

**Known trade-off:** because the controller is held by `remember` rather than by a real `ViewModel`,
rotating an Android device discards it mid-run and resets the bars. If that becomes a nuisance, a
thin Android-only `ViewModel` wrapping the shared controller fixes it without touching `shared`.

### One parent job, children launched inside it

`start()` calls `scope.launch { ... }` once, and the three per-bar coroutines are launched from
inside that parent's body. This nesting is what makes them children rather than siblings, and so it
is what makes a single `cancel()` on the parent stop all three. Launching the three directly on
`scope` would look almost identical and behave entirely differently.

Each bar picks its own random duration so the children are visibly independent while still sharing
one fate under cancellation.

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
