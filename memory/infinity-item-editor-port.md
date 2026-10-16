---
name: infinity-item-editor-port
description: First feature request, a modern remake of Ruukas97's Infinity Item Editor
type: project
---

*(as of 2026-10-16)* The user's first requested feature is a remake of Infinity Item Editor (https://github.com/Ruukas97/Infinity-Item-Editor), a mod they loved that is no longer updated. It should be reachable from the mod menu, see [[mod-menu-keybind]]. Target is Minecraft 1.21.11, see [[fabric-development]].

*(as of 2026-10-16)* The user pointed out the mod continued as Creative Editor: https://github.com/Ruukas97/Creative-Editor . It is Forge 1.16.5, GPL-3.0, last pushed 2023-10-12. Infinity Item Editor itself is the 1.12.2 version. Creative Editor features: main item screen (id, count, damage, unbreakable, name), lore editor, enchantment/attribute/effect wheels, item flags, color picker, head collection, item spawner, tag/NBT explorer and raw NBT editor, armor stand editor, banners/fireworks/books. It is NBT based, but 1.21.11 items use data components, so this must be a rewrite rather than a port. MC Tiny Tweak's fabric.mod.json has no license field and the repo has no LICENSE file, so copying Creative Editor code would need a GPL-3.0 decision first.

*(as of 2026-10-16)* Decisions: write our own code and use Creative Editor only as a feature and design reference (no GPL code copied). First stage scope: backslash menu opening an editor for the held item with item id, count, damage, unbreakable, custom name with colors, lore, enchantments at any level, plus a raw data component editor as a catch-all. Works in creative mode.

*(as of 2026-10-16)* First stage implemented: `\` opens `ModMenuScreen`, which opens `ItemEditorScreen` (id, count, damage, unbreakable, `&`-coded name, 3x preview with tooltip) plus Lore, Enchantments (searchable, levels 1-255, paged) and Raw SNBT screens. Apply writes the held hotbar slot locally and via `handleCreativeModeItemAdd(stack, 36 + slot)`; counts above the normal stack limit set `max_stack_size` because the server rejects count > max stack size. Gotchas found by tests: `isDamageableItem()` is false for unbreakable items (use `has(MAX_DAMAGE)`), and `ItemStack.remove` on a component with a default (every item has empty lore) records a removal, so reset to the prototype value instead. 27 unit tests pass and `./gradlew :1.21.11:build` succeeds. Not yet tested in game: the user said they will do the in-game testing themselves, so automated client GameTests were skipped for now.
