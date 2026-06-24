# Structure Protection

Protect generated structures from being placed in or broken into, for an extra exploration challenge.

Features:
- Blocks inside a generated structure piece (e.g., a Nether fortress, woodland mansion, or trial chamber) cannot be placed or broken. Protection follows each piece's real bounding boxes — read live from the server — rather than a chunk-and-radius approximation, so it tracks the actual shape of the structure. Creative-mode players bypass the protection.

- Choose *what* each rule protects, by block name and/or by shape — the two compose:
  - `protect`: a block-name regex. `.*` protects everything; you can also target specific blocks (e.g. `spawner`).
  - `protectStructural`: protect every block that blocks motion (walls, floors, stairs, fences, doors) — the structure's shape — while leaving non-physical blocks such as torches, carpets, and flowers freely editable. Structural-ness is a property of the block itself (judged by its default state, so e.g. a fence gate stays protected whether open or closed), the same whether you're placing or breaking it. Because the two combine, you can protect the shape *plus* a specific non-physical block (e.g. `protectStructural = true` with `protect = "sculk_sensor"`).

- Two protection modes, chosen per rule with a `breachable` flag:
  - **Always protected** (the default): the protected blocks can never be placed or broken.
  - **Breach from outside, locked inside** (`breachable = true`): you can break your way in through a wall from *outside* the structure, but once you are standing *inside that structure* you can no longer break its protected blocks — no tunneling straight to the loot. Breaching only ever lets you break a way in; placing protected blocks is never allowed. "Outside" is relative to the structure being breached, so standing inside an unrelated protected structure doesn't count. This is meant for sealed structures with no natural entrance, such as strongholds.

- Per-rule allow-lists carve out exceptions with regexes, matched against the block being placed or broken: `canPlace` (blocks you may still place) and `canBreak` (blocks you may still break) override protection. By default a shared base lets you break a few loot/navigation blocks (`decorated_pot`, `gilded_blackstone`, `gold_block`, any `*_ore`) while the structure's shape stays locked. The defaults set no `canPlace`: since only structural (motion-blocking) blocks are protected, non-physical blocks like ladders, torches, and carpets can already be placed freely. Keeping place and break separate lets you express asymmetric exceptions — e.g. break a Decorated Pot to loot it without being able to place new ones.

- Shared / library rules: a rule that protects nothing (no `protect` or `protectStructural`) contributes only its allow-lists, so a single `".*"` rule can grant a common exception to every protected structure instead of repeating it per rule. A structure's policy is the union of every rule that matches it.

Each config entry bundles a structure regex with what it protects, its breach mode, and its own place/break exceptions, so you can define one set of rules for some structures and a different set for others. By default a set of challenging structures has its shape protected (`protectStructural`) rather than every decoration — the sealed ones (ancient city, stronghold, trial chambers) breachable from outside — with a shared base granting common loot/navigation exceptions; you can add or remove structures as you see fit.

### Known limitations

- Only block placement (via items) and block breaking are intercepted. Other world edits — buckets/fluids, item frames, dispenser auto-placement, explosions, and pistons — are not.
- Breach mode decides protection from where you stand, so a player can still breach a single boundary block from outside into any room they can already reach through natural terrain. It preserves the "no cheesing once inside" challenge rather than being airtight anti-grief.


![Logo](/images/logo.png)


## Credits

Project template used: https://github.com/jaredlll08/MultiLoader-Template
