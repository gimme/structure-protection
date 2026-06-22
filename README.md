# Adventure Zones

Protect generated structures from being placed in or broken into, for an extra exploration challenge.

Features:
- Blocks inside a generated structure piece (e.g., a Nether fortress, woodland mansion, or trial chamber) cannot be placed or broken. Protection follows each piece's real bounding boxes — read live from the server — rather than a chunk-and-radius approximation, so it tracks the actual shape of the structure. Creative-mode players bypass the protection.

- Two protection modes, chosen per structure in the config with a `breachable` flag:
  - **Always protected** (`breachable = false`, the default): the structure's blocks can never be placed or broken.
  - **Breach from outside, locked inside** (`breachable = true`): you can break in through a wall from *outside* the structure, but once you are standing *inside* a protected piece you can no longer break or place protected blocks — no tunneling straight to the loot. This is meant for sealed structures with no natural entrance, such as strongholds.

- Per-structure allow-lists let you carve out exceptions, using regular expressions: for each protected-structure entry, which items may still be placed on which blocks (`canPlaceOn`) and which items may still break which blocks (`canBreak`). Because they live on the entry, you can grant different exceptions to different structures. By default `canPlaceOn` permits light sources (torches, lanterns) and `canBreak` is empty, since breaking is the main way to escape a protected structure.

Each config entry bundles a structure regex, its breach mode, and its own place/break exceptions, so you can define one set of rules for some structures and a different set for others. By default a set of challenging structures is protected, but you can add or remove structures as you see fit.

Also check out my other mod, [Adventure Mode Adjust](https://github.com/gimme/adventure-mode-adjust), which lets you customize which blocks can be placed and broken in vanilla Adventure mode, using regular expressions!

### Known limitations

- Only block placement (via items) and block breaking are intercepted. Other world edits — buckets/fluids, item frames, dispenser auto-placement, explosions, and pistons — are not.
- Breach mode decides protection from where you stand, so a player can still breach a single boundary block from outside into any room they can already reach through natural terrain. It preserves the "no cheesing once inside" challenge rather than being airtight anti-grief.


![Logo](/images/logo.png)


## Credits

Project template used: https://github.com/jaredlll08/MultiLoader-Template
