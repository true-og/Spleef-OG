# Spleef-OG

A BattleArena-backed Spleef plugin for Purpur / Paper 1.19.4.

Spleef-OG is maintained for TrueOG-style server-spawn minigames. It runs arenas in WorldGuard regions in the main world, not in MyWorlds world copies.

## Supported Modes
- **Classic Spleef**: players receive a shovel and break configured snow floor layers. Breaking `snow_block` floors grants 4 snowballs; breaking `snow` layers grants snowballs equal to the layer count. Thrown snowballs break configured Spleef layer blocks.
- **Bow Spleef**: players receive a bow and arrow. Arrows break configured layer blocks, and TNT projectile handling is gated by a configured WorldGuard region.

For example configurations, see the [templates](./templates) folder.

## Region Model
- Spleef arenas are intended to live in WorldGuard regions in the main `world` near server spawn.
- `config.yml` defaults `world-whitelist` to `world`. Add your real spawn-world name there if it differs.
- `region-whitelist` can restrict Spleef to specific WorldGuard region IDs. Leave it empty to allow any region in an allowed world.
- Arena creation requires standing in a WorldGuard region with `allow-spleef` set to `allow`.
- Maps can set `worldguard-region` to their region ID. Bow Spleef maps should set it to the region that covers the TNT/layer area.
- When `region-whitelist` is set, joins, layer resets, block breaks, snowball/arrow block hits, and Bow Spleef TNT handling are limited to whitelisted regions.
- Spleef-OG does not depend on MyWorlds and does not create per-match world copies.

Example setup flow:
```text
/rg define spleef_classic
/rg flag spleef_classic allow-spleef allow
/spleef create classic
/spleef layer add classic
/spleef deathregion classic
```

For Bow Spleef, also run:
```text
/spleef worldguardregion bowspleef spleef_bow
```

Example `config.yml` whitelist:
```yaml
world-whitelist:
  - world
region-whitelist:
  - spleef_classic
  - spleef_bow
```

## Compatibility
- **Server**: Purpur / Paper 1.19.4
- **Java**: 17+
- **Depends**: BattleArena 4.0.0-SNAPSHOT, WorldGuard
- **Soft-depends**: GameModeInventories-OG, HorseTp-OG, PetTeleport-OG, PlayerBounties-OG, WorldEdit

## Integrations
- **PetTeleport-OG**: pet teleports are suppressed while a player is in a Spleef match. No configuration required; PetTeleport-OG reads `SpleefAPI.isInSpleef`.
- **HorseTp-OG**: horse teleports are suppressed while a player is in a Spleef match. No configuration required; HorseTp-OG reads `SpleefAPI.isInSpleef`.
- **PlayerBounties-OG**: bounty claims and new bounty placements are cancelled whenever the claimant, victim, or target is in a Spleef match.
- **GameModeInventories-OG**: gamemode-specific inventories are preserved while players enter and leave Spleef.

## Commands
| Command                                       | Description                                         |
|-----------------------------------------------|-----------------------------------------------------|
| `/spleef deathregion <map>`                   | Sets the death region for a Spleef arena.           |
| `/spleef layer add <map>`                     | Adds a floor layer to a Spleef arena.               |
| `/spleef layer remove <map> <index>`          | Removes a layer from a Spleef arena.                |
| `/spleef layer clear <map>`                   | Clears all layers from a Spleef arena.              |
| `/spleef layer index <map> <from> <to>`       | Changes the index of a layer.                       |
| `/spleef layer list <map>`                    | Lists all layers in a Spleef arena.                 |
| `/spleef worldguardregion <map> <region>`     | Sets the map WorldGuard region ID.                  |

## Permissions
| Permission                                   | Command                         |
|----------------------------------------------|---------------------------------|
| `battlearena.command.spleef.deathregion`     | `/spleef deathregion`           |
| `battlearena.command.spleef.layer.add`       | `/spleef layer add`             |
| `battlearena.command.spleef.layer.remove`    | `/spleef layer remove`          |
| `battlearena.command.spleef.layer.clear`     | `/spleef layer clear`           |
| `battlearena.command.spleef.layer.index`     | `/spleef layer index`           |
| `battlearena.command.spleef.layer.list`      | `/spleef layer list`            |
| `battlearena.command.spleef.worldguardregion` | `/spleef worldguardregion`      |

## API
Spleef-OG exposes a small Bukkit-native API for other plugins. No BattleArena types on the surface, only `org.bukkit.entity.Player`.

### Check if a player is in a Spleef match
Java:
```java
import org.battleplugins.arena.spleef.api.SpleefAPI;

if (SpleefAPI.isInSpleef(player)) {
    // ...
}
```

Kotlin:
```kotlin
import org.battleplugins.arena.spleef.api.SpleefAPI

if (SpleefAPI.isInSpleef(player)) {
    // ...
}
```

### Events
Both extend `org.bukkit.event.player.PlayerEvent`.

| Event              | Fires when                                | `SpleefAPI.isInSpleef(player)` during event |
|--------------------|-------------------------------------------|---------------------------------------------|
| `SpleefJoinEvent`  | Player joins any Spleef-mode competition  | `true`                                      |
| `SpleefLeaveEvent` | Player leaves any Spleef-mode competition | `false`                                     |

Covers Classic Spleef and Bow Spleef. State flips before the event fires, so listeners observe the post-transition value.

## Building
```text
./gradlew build
```
Output: `build/libs/Spleef-OG-<version>.jar`

## Credits
Upstream: [BattlePlugins/ArenaSpleef](https://github.com/BattlePlugins/ArenaSpleef). Documentation: [BattleDocs](https://docs.battleplugins.org/books/additional-gamemodes/chapter/spleef).

## License
Released into the public domain. See `LICENSE`.
