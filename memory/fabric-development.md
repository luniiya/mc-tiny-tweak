---
name: fabric-development
description: Verified official Fabric tooling and test workflow references
type: reference
---

*(as of 2026-10-16)* Fabric's official project guide recommends its template generator or example mod as the starting point: https://docs.fabricmc.net/develop/getting-started/creating-a-project . The target Minecraft version has not been selected with the user.

*(as of 2026-10-16)* Minecraft 26.1 requires Java 25 and uses unobfuscated game names, with the `net.fabricmc.fabric-loom` plugin replacing the remapping plugin for this target: https://fabricmc.net/2026/03/14/261.html . Do not apply these requirements blindly to older Minecraft targets.

*(as of 2026-10-16)* Official testing docs describe Fabric Loader JUnit for unit tests, `useJUnitPlatform()`, and client/server GameTests for game behavior: https://docs.fabricmc.net/develop/automatic-testing . See [[unit-testing]].

*(as of 2026-10-16)* Loom supports `./gradlew runClient` and `./gradlew runServer`; IDE debug profiles allow limited hot swapping: https://docs.fabricmc.net/develop/getting-started/intellij-idea/launching-the-game .

*(as of 2026-10-16)* Minecraft 1.21.11 is selected. The scaffold uses JDK 21 (installed locally), official Mojang mappings, Loom's remapping plugin, Kotlin/JVM, and Stonecutter shared sources. Java 25 and unobfuscated Loom notes above concern newer targets, not this one. Fabric's 1.21.11 template was retrieved from https://github.com/FabricMC/fabric-example-mod/tree/1.21.11 .

*(as of 2026-10-16)* Stonecutter's current GitHub mirror is https://github.com/stonecutter-versioning/stonecutter/tree/0.9 ; the old Skidamek repository is outdated. Official website requests returned unusable content or 404, so source examples from the maintained mirror were used. Future versions should share sources with per-target dependencies and builds; compatibility still requires porting and testing changed Minecraft integration.

*(as of 2026-10-16)* The latest Loom 1.18.4 failed configuration under the installed JDK 21 because its plugin requires JDK 25. The scaffold was changed to stable Loom 1.14.10 (published for JVM 21) and Gradle 9.2.1 with a verified distribution checksum. Kotlin 2.4.21 supports Gradle 9.2.1. This keeps development and CI on JDK 21 for the selected Minecraft target.

*(as of 2026-10-16)* The user asked about hot reload. Standard debugger hot swapping can replace method bodies but has structural limits; normal Prism JAR replacement needs a restart. DEVELOPMENT.md documents this and resource/data reloads. Enhanced redefinition and mixin agents are optional future tooling, not configured or promised by the scaffold.

*(as of 2026-10-16)* Scaffold implementation is local in `mc-tiny-tweak/` with shared sources, exact 1.21.11 metadata, CI build/test configuration, Prism/development documentation, and unit tests. No gameplay tweak or actual mixin injection has been implemented yet. A Gradle resource task receiver issue was corrected by using `project.property(...)`; build and tests are still being verified. Nothing from this scaffold has been committed or pushed yet.

*(as of 2026-10-16)* Scaffold verification completed: `./gradlew :1.21.11:build --console=plain` succeeded with three tests passing and produced `versions/1.21.11/build/libs/mc-tiny-tweak-0.1.0+mc1.21.11.jar`. `./gradlew :1.21.11:runClient --console=plain` launched Minecraft, loaded the mod (confirmed by its log message), initialized graphics/audio/resources, and reached accessibility onboarding rendering. The temporary client was stopped with Ctrl+C after startup, yielding exit 130 intentionally. This machine logged a missing optional narrator `libflite.so` and an X11 cursor warning; these did not prevent startup. No gameplay or actual injections were tested, and nothing has been committed or pushed.
