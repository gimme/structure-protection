# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Fabric support: the mod now runs on both NeoForge and Fabric from a shared common codebase
- `protectsOnlyPhysical` flag on `structureProtection` rules: when `true`, a rule guards only blocks that block motion (walls, floors, stairs, fences, doors) and leaves non-physical blocks like torches, carpets, and flowers freely editable — protecting the structure's shape rather than every decoration. For placement it's judged by the block being placed, not its support. The bundled defaults enable it on the protected structures; the config key itself defaults to `false`. Existing config files are preserved on update, so add the key manually (or regenerate the file) to opt in.

### Changed

- Reworked protection: instead of forcing players into Adventure mode near structures, the mod now prevents placing and breaking blocks within the real bounds of protected structure pieces (read live from the server's structure data). Each `structureProtection` entry bundles a structure regex, a `protected` flag, a `breachable` flag (always-protected vs. breach-from-outside/locked-inside), and its own `canPlaceOn`/`canBreak` regex allow-lists, so rules can differ per structure. A structure's allowed edits are the union of every matching rule, and a rule with `protected = false` only contributes allow-lists without protecting anything on its own, so common exceptions can live in a single shared `".*"` base rule rather than being repeated per structure. Removed the combat-mode timer and on-screen mode text. Creative mode bypasses protection.
- Updated to Minecraft 26.2 (Java 25), NeoForge 26.2 and Fabric API 0.152.1
- Modernized the build: Gradle 9.5.1, Fabric Loom 1.17, and dropped Parchment mappings

## [v1.21.11-0.1.1] - 2026-01-17

### Added

- Initial release
