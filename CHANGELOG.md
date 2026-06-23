# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Fabric support: the mod now runs on both NeoForge and Fabric from a shared common codebase

### Changed

- Reworked protection: instead of forcing players into Adventure mode near structures, the mod now prevents placing and breaking blocks within the real bounds of protected structure pieces (read live from the server's structure data). Each `structureProtection` entry bundles a structure regex with what it protects — a `protect` block-name regex (`.*` for everything) and/or a `protectStructural` flag (every motion-blocking block, i.e. the structure's shape, leaving decoration editable) — a `breachable` flag (always-protected vs. breach-from-outside/locked-inside), and `canPlace`/`canBreak` block-name regex allow-lists that override protection. A structure's policy is the union of every matching rule, and a rule that protects nothing contributes only its allow-lists, so a shared `".*"` rule can grant common exceptions without protecting every structure. Removed the combat-mode timer and on-screen mode text. Creative mode bypasses protection.
- Updated to Minecraft 26.2 (Java 25), NeoForge 26.2 and Fabric API 0.152.1
- Modernized the build: Gradle 9.5.1, Fabric Loom 1.17, and dropped Parchment mappings

## [v1.21.11-0.1.1] - 2026-01-17

### Added

- Initial release
