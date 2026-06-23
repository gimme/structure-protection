# Adventure Zones

Protect generated structures from being placed in or broken into, for an extra exploration challenge.

Features:
- Blocks inside a generated structure piece (e.g., a Nether fortress, woodland mansion, or trial chamber) cannot be placed or broken. Protection follows each piece's real bounding boxes — read live from the server — rather than a chunk-and-radius approximation, so it tracks the actual shape of the structure. Creative-mode players bypass the protection.

- Two protection modes, chosen per structure in the config with a `breachable` flag:
  - **Always protected** (`breachable = false`, the default): the structure's blocks can never be placed or broken.
  - **Breach from outside, locked inside** (`breachable = true`): you can break in through a wall from *outside* the structure, but once you are standing *inside that structure* you can no longer break or place its protected blocks — no tunneling straight to the loot. "Outside" is relative to the structure being breached, so standing inside an unrelated protected structure doesn't count. This is meant for sealed structures with no natural entrance, such as strongholds.

- Protect only the structure's shape via a `protectsOnlyPhysical` flag: with `protectsOnlyPhysical = true`, a rule guards only blocks that block motion (walls, floors, stairs, fences, doors) and leaves non-physical blocks such as torches, carpets, and flowers freely editable. Use it when the point is to keep the structure's physical layout intact rather than freeze every decoration. ("Physical" means the block has a motion-blocking collision box; for placement it's judged by the block being placed, not the block it rests on.) The config key defaults to `false` (every block in scope is protected), but the bundled defaults enable it on the protected structures.

- Per-structure allow-lists let you carve out exceptions, using regular expressions: for each rule, which items may still be placed on which blocks (`canPlaceOn`) and which items may still break which blocks (`canBreak`). Because they live on the rule, you can grant different exceptions to different structures. By default `canPlaceOn` permits light sources (torches, lanterns) and `canBreak` is empty, since breaking is the main way to escape a protected structure.

- Shared base rules via a `protected` flag: a rule with `protected = false` never protects a structure on its own — it only contributes its allow-lists to structures that some *other* rule protects. A structure's allowed edits are the union of every rule that matches it, so common exceptions can live in a single non-protecting `".*"` base rule instead of being repeated in every structure. (A `".*"` rule must be non-protecting, since most structures should not be touched at all.) Rules with no `protected` key default to `true`.

Each config entry bundles a structure regex, whether it protects, its breach mode, and its own place/break exceptions, so you can define one set of rules for some structures and a different set for others. By default a set of challenging structures is protected — guarding their physical shape (`protectsOnlyPhysical`) rather than every decoration — with a shared base granting the common light-source exception; you can add or remove structures as you see fit.

Also check out my other mod, [Adventure Mode Adjust](https://github.com/gimme/adventure-mode-adjust), which lets you customize which blocks can be placed and broken in vanilla Adventure mode, using regular expressions!

### Known limitations

- Only block placement (via items) and block breaking are intercepted. Other world edits — buckets/fluids, item frames, dispenser auto-placement, explosions, and pistons — are not.
- Breach mode decides protection from where you stand, so a player can still breach a single boundary block from outside into any room they can already reach through natural terrain. It preserves the "no cheesing once inside" challenge rather than being airtight anti-grief.


![Logo](/images/logo.png)


## Credits

Project template used: https://github.com/jaredlll08/MultiLoader-Template
