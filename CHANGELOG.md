# Changelog

## 2.0.1-SNAPSHOT:

- First beta-ready snapshot of the Spleef-OG fork. Targets Purpur / Paper 1.19.4.
- **Mode scope**: Spleef-OG now supports only Classic Spleef and Bow Spleef. Unsupported projectile-cannon and TNT Run code paths and templates were removed.
- **Classic snowballs**: Breaking configured snow floor layers now grants throwable snowballs, and thrown snowballs break configured Spleef layer blocks.
- **WorldGuard region flow**: Arenas are intended to run in WorldGuard regions in the main spawn world. The default `world-whitelist` now targets `world`, and `/spleef worldguardregion <map> <region>` stores the map region ID.
- **Region whitelist**: `config.yml` now supports `region-whitelist` for limiting Spleef arena creation, joining, resets, floor breaking, projectile floor breaking, and Bow Spleef TNT handling to specific WorldGuard regions.
- **Editor layer lookup**: `SpleefMap.positionToLayers` is now maintained on `addLayer`, `removeLayer`, and `clearLayers`. Layers created via `/spleef layer add` are immediately usable; no server restart required.
- **NPE on layer removal**: `SpleefMap.removeLayer` and `clearLayers` null-guard `this.layers`, matching the lazy init in `addLayer`. `/spleef layer clear` on a fresh map no longer throws.
- **Bow Spleef projectiles**: Arrows break layer blocks in bow-spleef mode.
- **`SpleefConfig.getShovel` NPE**: Now null-safe when the configured shovel name is unknown, honoring the `@Nullable` contract. Prevents crash on typos in arena YAML (e.g. `give-shovel{shovel=clasic}`).
- **PlayerMoveEvent hot path**: `SpleefArena.onMove` now returns early when `from` and `to` share block coordinates, cutting per-tick overhead for death-region checks by a large margin.
- **Scheduler cleanup on disable**: `ArenaSpleef.onDisable` cancels all scheduler tasks owned by the plugin, preventing delayed tasks from firing against stale map references after reload.
- Gradle build updated to use the TrueOG bootstrap maven-local repository when available, falling back to `~/.m2`.
