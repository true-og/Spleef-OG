# Changelog

## 2.0.1-SNAPSHOT:

- First beta-ready snapshot of the Spleef-OG fork. Targets Purpur / Paper 1.19.4.
- **Editor layer lookup**: `SpleefMap.positionToLayers` is now maintained on `addLayer`, `removeLayer`, and `clearLayers`. Layers created via `/spleef layer add` are immediately usable; no server restart required. Previously the lookup map was only populated in `postProcess`, so runtime-added layers were invisible to block-break, decay, and projectile-hit logic.
- **Decay scheduler crash**: `SpleefCompetition.beginLayerDecay` now guards against empty layers and clamps the per-block interval to `Math.max(1, totalTicks / blocks)`. Prevents `ArithmeticException` on empty layers and runaway 0-tick-period timers on very large layers.
- **Splegg phantom eggs**: `SpleefArena.onInteract` now returns early when the held item is not a tagged spleef item. Previously an egg was launched on every right-click while in SPLEGG mode, regardless of the item in hand.
- **NPE on layer removal**: `SpleefMap.removeLayer` and `clearLayers` null-guard `this.layers`, matching the lazy init in `addLayer`. `/spleef layer clear` on a fresh map no longer throws.
- **Bow Spleef broken**: `SpleefEventResolvers.PROJECTILE_HIT` now accepts any `Projectile`, with dedicated branches for `Egg` (splegg or `projectiles-break-blocks`) and `AbstractArrow` (BOW_SPLEEF or `projectiles-break-blocks`). Arrows now break layer blocks in bow-spleef mode.
- **`SpleefConfig.getShovel` NPE**: Now null-safe when the configured shovel name is unknown, honoring the `@Nullable` contract. Prevents crash on typos in arena YAML (e.g. `give-shovel{shovel=clasic}`).
- **PlayerMoveEvent hot path**: `SpleefArena.onMove` now returns early when `from` and `to` share block coordinates, cutting per-tick overhead for the death-region and decay-trigger checks by a large margin.
- **Scheduler leak on disable**: `ArenaSpleef.onDisable` cancels all scheduler tasks owned by the plugin, preventing leaked decay tasks from firing against stale map references after reload.
- Gradle build updated to use the TrueOG bootstrap maven-local repository when available, falling back to `~/.m2`.
