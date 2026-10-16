---
name: kotlin-stack-evaluation
description: Kotlin versus Java for MC Tiny Tweak and the user's stack priorities
type: reference
---

*(as of 2026-10-16)* The user considers stack selection crucial and wants a modern stack that will not bottleneck the mod. They asked about Kotlin but have not yet confirmed a language or Minecraft version.

*(as of 2026-10-16)* FabricMC maintains Fabric Language Kotlin, providing Kotlin entrypoints and bundling the standard library and common kotlinx libraries. It must be declared as a mod dependency. Source: https://github.com/FabricMC/fabric-language-kotlin .

*(as of 2026-10-16)* Kotlin offers nullable types, extension functions, and less boilerplate. Java interoperability can expose platform types with weaker null guarantees. Sources: https://wiki.fabricmc.net/tutorial:kotlin and https://kotlinlang.org/docs/java-interop.html .

*(as of 2026-10-16)* Recommended stack under discussion: Kotlin/JVM for main logic, Java for thin mixins when necessary, Fabric Loader/API, Loom, Gradle Kotlin DSL, IntelliJ, Fabric Loader JUnit, GameTests, and GitHub Actions. This is an assistant recommendation, not a confirmed user decision. Fabric's Kotlin wiki advises Java mixins delegating to Kotlin: https://wiki.fabricmc.net/tutorial:kotlin . Its old dependency/JDK examples should not be copied for modern targets.

See [[fabric-development]] and [[unit-testing]].

*(as of 2026-10-16)* The user accepted the Kotlin/JVM plus Java mixins stack and authorized starting implementation. They selected Minecraft 1.21.11 as the first target and want shared source across game versions rather than maintaining a copied codebase per version. Stonecutter is being evaluated for this purpose.
