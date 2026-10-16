---
name: prism-settings-source
description: Path of the user's real 1.21.11 Prism instance whose settings the dev client should use
type: fact
---

*(as of 2026-10-16)* On the user's main computer, their real Minecraft 1.21.11 Prism instance is `/home/ayaya/didi/apps/games/minecraft/prism/instances/1.21.11`; game files are in its `minecraft/` subfolder (`options.txt` holds settings and keybinds). The user says this path is always the same for this version, except on their laptop where it differs. They want the dev client (`runClient`) to default to their own settings instead of vanilla defaults. See [[prism-launcher]].

*(as of 2026-10-16)* Implemented: `runClient` depends on `copyPrismOptions`, which copies `options.txt` from the folder in Gradle property `mctinytweak.prismInstance` into `versions/1.21.11/run/` on every launch, and skips with a message if unset or missing. This machine sets the property in `~/.gradle/gradle.properties` (outside the repo) to the instance's `minecraft/` folder. The laptop needs its own line there. Verified: the copy ran and the client loaded the mod; the game drops keybinds of mods absent from the dev client.
