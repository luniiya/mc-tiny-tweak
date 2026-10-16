---
name: private-workspace-folders
description: Where non-public material lives (inspiration clones, decompiled Minecraft source)
type: decision
---

*(as of 2026-10-16)* Anything that must not go in the public repo lives in the parent workspace `/home/ayaya/dev/myMcTweaks/`, in a folder named for what it is. The user cloned Creative Editor into `inspiration/Creative-Editor/` (reference only, see [[infinity-item-editor-port]]). Decompiled Minecraft 1.21.11 source (Mojang mappings, produced locally by `./gradlew :1.21.11:genSources`) is extracted at `minecraft-1.21.11-source/`; Mojang code cannot be redistributed. Regenerate it with genSources after a Minecraft or mappings change.
