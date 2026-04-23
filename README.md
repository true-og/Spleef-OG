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

## API
Spleef-OG exposes a small Bukkit-native API for other plugins. No BattleArena types on the surface — only `org.bukkit.entity.Player`.

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

Covers every mode (Classic, Splegg, Decay, Bow Spleef). State flips before the event fires, so listeners observe the post-transition value.

### Consumer setup (Gradle Kotlin DSL)
```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly(files("libs/Spleef-OG-<version>.jar"))
}
```

`plugin.yml`:
```yaml
depend: [Spleef-OG]
```

### Listener examples
Java:
```java
import org.battleplugins.arena.spleef.api.SpleefAPI;
import org.battleplugins.arena.spleef.api.SpleefJoinEvent;
import org.battleplugins.arena.spleef.api.SpleefLeaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SpleefHooks implements Listener {

    @EventHandler
    public void onJoin(SpleefJoinEvent event) {

        Player player = event.getPlayer();
        // SpleefAPI.isInSpleef(player) == true

    }

    @EventHandler
    public void onLeave(SpleefLeaveEvent event) {

        Player player = event.getPlayer();
        // SpleefAPI.isInSpleef(player) == false

    }

}
```

Kotlin:
```kotlin
import org.battleplugins.arena.spleef.api.SpleefAPI
import org.battleplugins.arena.spleef.api.SpleefJoinEvent
import org.battleplugins.arena.spleef.api.SpleefLeaveEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SpleefHooks : Listener {

    @EventHandler
    fun onJoin(event: SpleefJoinEvent) {
        val player = event.player
        // SpleefAPI.isInSpleef(player) == true
    }

    @EventHandler
    fun onLeave(event: SpleefLeaveEvent) {
        val player = event.player
        // SpleefAPI.isInSpleef(player) == false
    }

}
```

## Building
```
./gradlew build
```
Output: `build/libs/Spleef-OG-<version>.jar`

## Credits
Upstream: [BattlePlugins/ArenaSpleef](https://github.com/BattlePlugins/ArenaSpleef). Documentation: [BattleDocs](https://docs.battleplugins.org/books/additional-gamemodes/chapter/spleef).

## License
Released into the public domain. See `LICENSE`.
