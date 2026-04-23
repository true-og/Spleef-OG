# Spleef-OG

A spleef plugin using [BattleArena](https://github.com/BattlePlugins/BattleArena).

Spleef-OG is a fork of BattlePlugins' ArenaSpleef, maintained for Purpur 1.19.4. It keeps the classic spleef experience alongside multiple modes and addresses critical stability bugs that blocked beta release of upstream 2.0.x.

## Spleef Modes
- **Classic**: Players receive a shovel to break blocks and knock other players into the void.
- **Splegg**: Players receive an egg cannon to shoot eggs that break blocks.
- **Decay**: Blocks decay beneath the player. Commonly played as "TNT Run", but any block type works.
- **Bow Spleef**: Players receive a bow to shoot arrows that break blocks. TNT layers prime on hit.

For example configurations of these modes, see the [templates](./templates) folder.

## Compatibility
- **Server**: Purpur / Paper 1.19.4
- **Java**: 17+
- **Depends**: BattleArena 4.0.0-SNAPSHOT, WorldGuard
- **Soft-depends**: WorldEdit

## Notes
- **Arena reset**: Layer pasting now hard-resets configured layer volumes each round instead of only filling air blocks.
- **Bow Spleef TNT**: TNT priming is now gated by WorldGuard. Set `worldguard-region: <region-id>` on the map used by a Bow Spleef arena, and make sure that region covers the TNT layer.

## Commands
| Command                               | Description                                    |
|---------------------------------------|------------------------------------------------|
| /spleef deathregion <map> <region>    | Sets the death region for a spleef arena.      |
| /spleef layer add <map>               | Adds a layer to a spleef arena.                |
| /spleef layer remove <map> <index>    | Removes a layer from a spleef arena.           |
| /spleef layer clear <map>             | Clears all layers from a spleef arena.         |
| /spleef layer index <map> <from> <to> | Changes the index of a layer.                  |
| /spleef layer list <map>              | Lists all layers in a spleef arena.            |

## Permissions
| Permission                              | Command              |
|-----------------------------------------|----------------------|
| battlearena.command.spleef.deathregion  | /spleef deathregion  |
| battlearena.command.spleef.layer.add    | /spleef layer add    |
| battlearena.command.spleef.layer.remove | /spleef layer remove |
| battlearena.command.spleef.layer.clear  | /spleef layer clear  |
| battlearena.command.spleef.layer.index  | /spleef layer index  |
| battlearena.command.spleef.layer.list   | /spleef layer list   |

## Building
```
./gradlew build
```
Output: `build/libs/Spleef-OG-<version>.jar`

## Credits
Upstream: [BattlePlugins/ArenaSpleef](https://github.com/BattlePlugins/ArenaSpleef). Documentation: [BattleDocs](https://docs.battleplugins.org/books/additional-gamemodes/chapter/spleef).

## License
Released into the public domain. See `LICENSE`.
