# Changelog

## 2.0.1-SNAPSHOT

- Replaced the external arena framework with a native WorldGuard-region arena engine.
- Added persistent arena definitions, player recovery snapshots, and Spleef-owned statistics.
- Added multiple concurrent Classic and Bow Spleef arenas in whitelisted worlds.
- Added a 20-second automatic countdown once at least two players are waiting.
- Added Duels-style region setup commands for waiting, spectator, and player spawns.
- Added click-based layer and death-region editors.
- Added inventory, location, gamemode, health, experience, effect, flight, and scoreboard restoration.
- Added waiting, countdown, match, victory, spectator, floor reset, and restart recovery lifecycles.
- Renamed the plugin and API implementation packages to `net.trueog.spleefog`.
- Kept GameModeInventories-OG, HorseTp-OG, PetTeleport-OG, PlayerBounties-OG, and BattleTracker integrations.
