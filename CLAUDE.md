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
- Use Prism Launcher for instructions about installing the built mod.
- Keep the README short with minimal formatting and no em dash. Put development
  details in DEVELOPMENT.md.

## Persistent Memory Protocol

Your working directory is not just a workspace — it is your long-term memory.
Everything you learn must survive context compaction and session restarts by
living on disk, not in the conversation.

### Layout

- `memory/INDEX.md` — one line per note: `- [title](file.md) — one-line hook`
- `memory/<topic>/<slug>.md` — one atomic note per file. kebab-case filenames.
  Use subdirs to group by domain once a topic has 3+ notes (e.g. `memory/user/`,
  `memory/project-x/`, `memory/reference/`).

### Note format

Every note starts with frontmatter:

```markdown
---
name: <slug>
description: <one line, used to judge relevance without opening the file>
type: fact | decision | project | reference
---
```

Body: the fact itself, dated. Cross-link related notes with `[[slug]]`.

### The two hard rules

1. **Search before you answer.** If you're not certain, check `INDEX.md`
   first, then read the matching note(s), before answering. Don't guess or
   re-derive something that's already written down. Only fall back to an
   external source (web, filesystem, asking the user) if the workspace has
   nothing.
2. **Write immediately after you learn.** The moment new info surfaces —
   user states a fact, you figure something out, an external lookup
   resolves something — write or update the note *right then*, not
   "later." If it isn't on disk, it doesn't count as known.

### Non-negotiables

- **Date every fact.** `*(as of YYYY-MM-DD)*` on every claim that could go
  stale. No exceptions.
- **Never overwrite, always append.** If something changes, add the new
  fact below the old one with its own date — don't delete or edit the old
  line. History must stay reconstructable.
- **Keep notes atomic and small.** One fact/decision per file. If a note is
  covering two unrelated things, split it.
- **Keep INDEX.md in sync.** Every new note gets a line the same turn it's
  created. INDEX.md is what gets skimmed each session — it must always be
  a true map of what's in `memory/`.
