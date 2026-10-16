---
name: mc-tiny-tweak
description: Project identity and workspace layout
type: project
---

*(as of 2026-10-16)* MC Tiny Tweak is the user's opinionated Minecraft mods and tweaks project. Its public repository is https://github.com/luniiya/mc-tiny-tweak and its clone is `mc-tiny-tweak/` beneath this workspace. Other repositories should be cloned alongside this folder so they stay separate.

*(as of 2026-10-16)* The user requested a very short README with minimal formatting and no em dash.

*(as of 2026-10-16)* The user requested pushing all completed setup and adding an always-commit-and-push rule to both AGENTS.md and CLAUDE.md. Workspace and project instructions now require committing and pushing completed changes after checks pass unless the user explicitly asks otherwise. Persistent memory is being placed inside the repository with a workspace symlink to keep one canonical copy.

*(as of 2026-10-16)* The complete scaffold, project instructions, CI, and memory were committed as `2cffa1e` and pushed to `main`. Memory now lives in the repository; the parent workspace's `memory` path is a symlink to it. The HTTPS OAuth login cannot push workflow files because it lacks `workflow` scope. The existing SSH login authenticates as luniiya and successfully pushed the complete commit. Origin keeps its HTTPS fetch URL and uses `git@github.com:luniiya/mc-tiny-tweak.git` for pushes.
