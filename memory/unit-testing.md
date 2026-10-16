---
name: unit-testing
description: User requires unit testing for code changes
type: decision
---

*(as of 2026-10-16)* The user explicitly requested an always-do-unit-testing rule in both workspace `AGENTS.md` and `CLAUDE.md`. Both now require meaningful tests for changed behavior, regression tests for fixes, running the unit suite, and reporting actual results or blockers. No Gradle project exists yet in the clone.

See [[fabric-development]].

*(as of 2026-10-16)* A Gradle/Stonecutter scaffold now exists in `mc-tiny-tweak/`. Its current unit-test command is `./gradlew :1.21.11:test` and full build command is `./gradlew :1.21.11:build`. Project-local AGENTS.md and CLAUDE.md carry the testing rules too. Initial tests verify packaged metadata, the real Kotlin entrypoint adapter, and mixin configuration. Build verification is ongoing; no success has yet been claimed.

*(as of 2026-10-16)* `./gradlew :1.21.11:build --console=plain` now passes, including all three loading tests. Tests use the mod container's `findPath` to resolve resources: a global classpath lookup for `fabric.mod.json` can select a dependency mod instead. The remapped output JAR was inspected and contains the correct expanded metadata, Kotlin entrypoint class, and mixin configuration. Client startup verification is in progress.

*(as of 2026-10-16)* Client startup verification subsequently confirmed that Minecraft loads the mod and begins rendering. The client was deliberately stopped after the smoke check. Gameplay tests remain for future implemented features. See [[fabric-development]] for environment warnings and exact results.
