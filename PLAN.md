# PLAN.md

Tracks in-progress and upcoming work for this project. See `CLAUDE.md` for durable project reference.

## Current Milestone

Development is intentionally iterative. The first version focuses on a single scenario: launching a parent coroutine with child coroutines, and showing that cancelling the parent cancels all children.

## Next Steps

The following have not yet been added and are the immediate next steps:

1. **Coroutines dependency** — add `kotlinx-coroutines-core` to `shared/build.gradle.kts` `commonMain.dependencies`, with a version entry in `gradle/libs.versions.toml`.
2. **SKIE plugin** — apply `co.touchlab.skie` in `shared/build.gradle.kts`. See [SKIE docs](https://touchlab.co/skie) for the exact plugin and version declarations.
