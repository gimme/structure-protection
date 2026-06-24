# Architecture

A short map of how the mod is organized, for contributors. For *what* the mod does and how to configure it,
see the [README](README.md).

## Ubiquitous language

The terms below are used consistently in code, config, and docs — each maps to a type or method so the model
reads the way the feature is described.

| Term | Meaning | In code |
| --- | --- | --- |
| **Structure** | A generated structure, identified by its registry id (e.g. `minecraft:fortress`). | `Identifier` |
| **Piece** | One axis-aligned chunk of a structure's real bounds. Protection follows pieces, not chunks/radius. | `ProtectedPiece` |
| **Structure rule** | One policy entry: which structures it applies to, what it protects, and the exceptions that survive. | `StructureRule` |
| **Protect** | A rule marks a block protected — by name pattern (`protect`) and/or by shape (`protectStructural`, every structural block). | `StructureRule.protects(...)` |
| **Structural** | A block that's part of the shape, judged by the block type (its default state blocks motion) — the same for placing and breaking. | `BlockEdit.isStructural()` |
| **Breach** | A protected block may still be *broken* from outside the structure, but never once inside it (and never placed). | `StructureRule.breachable()` |
| **Allow-list** | Per-rule exceptions (`canPlace` / `canBreak`) that override protection for matching blocks. | `StructureRule.allowsPlacing/allowsBreaking(...)` |
| **Block edit** | A player's attempt to place or break one block — the thing weighed against the policy. | `BlockEdit` |
| **Id pattern** | A regex over namespaced ids — the shared vocabulary for naming sets of structures and blocks. | `IdPattern` |
| **Policy** | A structure's effective rules: the union of every rule that applies to it. | `StructureSource.Match` |

## Layers

```
mixin / network  →  application  →  domain  ←  infrastructure
(loader-facing)     (orchestration)  (the model)   (config adapter)
```

- **`domain`** — the model and all policy logic, free of loader and I/O concerns. `StructureRule`, `IdPattern`,
  `BlockEdit`, and `ProtectedPiece` are immutable value objects; a rule answers questions about itself
  (`appliesTo`, `protects`, `allowsPlacing`, `allowsBreaking`). `StructureSource` is the port that supplies which
  pieces/rules cover a position, with two adapters: `ServerStructureSource` (live `StructureManager`, authoritative)
  and `ClientStructureSource` (streamed pieces). `config.ServerConfig` is the outbound port for the policy.
- **`application`** — `BlockProtection`: the stateless decision service. It translates a Minecraft place/break into a
  `BlockEdit` and asks the domain whether to prevent it. The same code runs on both sides; only the `StructureSource`
  differs.
- **`infrastructure`** — `FcapServerConfig`: backs `ServerConfig` with the on-disk TOML config (Forge Config API Port),
  turning config entries into domain `StructureRule`s.
- **`network`** — streams the reach-range protected pieces + rules to capable clients so protected blocks *feel*
  unbreakable. Purely cosmetic; the server's verdict is always authoritative.
- **`mixin`** — the only loader/Minecraft entry points. They read the composition roots (`Main`, `ClientProtection`)
  and call `BlockProtection`; they hold no logic of their own.

## Why the decision is shared

Protection is enforced server-side. The client mirrors a subset of structure pieces only to remove doomed feedback
(no selection outline, no mining cracks, no place-then-revert flicker) on blocks the server will reject. Running the
*same* `BlockProtection` against a `ClientStructureSource` keeps the client's feel in lockstep with the server's
authority, with no second implementation to drift.
