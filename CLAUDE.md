# Project instructions

- Always commit and push completed changes after the relevant checks pass,
  unless the user explicitly asks otherwise. Report any blocker instead of
  silently leaving changes uncommitted or unpushed.

- Use Kotlin for shared mod logic and Java for Minecraft mixins.
- Target Minecraft 1.21.11 initially. Share sources through Stonecutter when
  adding versions; keep integration differences small and test every target.
- Always add or update meaningful unit tests for changed behavior, including
  regression tests for fixes. Run `./gradlew :1.21.11:test` and the build before
  considering code changes complete. Report results or explicit blockers.
- Add GameTests when implementing gameplay behavior that unit tests cannot
  exercise. Do not treat a successful compilation as proof that a mixin works.
- A "notification" means the mod's own chat line shown with `Notices.show`:
  the pink `[MC Tiny Tweak]` tag followed by light pink text, local only and
  never sent to the server or the agent's chat log. Its text goes in the lang
  file under `notice.mc_tiny_tweak.*`. Use it for things the mod does by itself
  or toggles, like the MCP agent taking over or fullbright on/off.
- Use Prism Launcher for instructions about installing the built mod.
- Keep the README short with minimal formatting and no em dash. Put development
  details in DEVELOPMENT.md.

## Memory

This repository holds only the mod's code. Persistent memory lives in the
parent workspace's `memory/` folder (`../memory/`), never in this repository.
Follow the memory protocol in the workspace instructions.
