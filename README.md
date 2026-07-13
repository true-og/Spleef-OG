# Spleef-OG

A standalone Classic and Bow Spleef plugin for Purpur / Paper 1.19.4.

Spleef-OG runs multiple concurrent arenas in WorldGuard regions inside normal or MyWorlds-managed worlds. It does not create or copy worlds.

## Gameplay

- Classic Spleef gives each player a shovel. Broken snow floors award snowballs, and thrown snowballs break configured floor blocks.
- Bow Spleef gives each player an infinity flame bow and one arrow. Arrow impacts break configured floor blocks.
- An arena starts a 20-second countdown as soon as at least two players are waiting. The countdown pauses if fewer than two remain.
- The last surviving player wins. Matches draw when the configured time limit expires.
- Waiting players, active players, and spectators have their inventory, location, gamemode, health, experience, effects, flight state, and scoreboard restored when they leave.
- Recovery snapshots survive plugin or server restarts.

Spleef statistics are stored in `plugins/Spleef-OG/stats.yml`. Use `/spleef stats [player]` to see wins, losses, ties, and games played.

## Requirements

- Java 17+
- Purpur / Paper 1.19.4
- WorldGuard 7
- MyWorlds is optional. Declaring it as a soft dependency ensures managed worlds load before arena locations are read.

## Configuration

`config.yml` defaults to one allowed world:

```yaml
world-whitelist:
  - world
```

It also controls the minimum player count, 20-second waiting period, match time limit, victory delay, tools, and scoreboard.

## Arena Setup

Create a WorldGuard region and allow Spleef in it:

```text
/rg define spleef_classic
/rg flag spleef_classic allow-spleef allow
```

Stand inside the region, then configure the arena:

```text
/spleef create classic classic
/spleef setwait classic
/spleef setspectator classic
/spleef addspawn classic
/spleef addspawn classic
/spleef layer add classic
/spleef deathregion classic
/spleef info classic
```

`layer add` and `deathregion` prompt for two block clicks. Layer setup then prompts for Bukkit block data such as `minecraft:snow_block`. Arena definitions are stored in `plugins/Spleef-OG/arenas.yml`.

Use `bow` instead of `classic` in the create or mode command for Bow Spleef.

## Player Commands

| Command | Description |
|---|---|
| `/spleef arenas` | Show currently available arenas and their join commands. |
| `/spleef join` | Show currently available arenas. |
| `/spleef join <arena>` | Join a named arena. |
| `/spleef leave` | Leave the current arena or spectator session. |
| `/spleef spectate <arena>` | Spectate an arena. |
| `/spleef stats [player]` | Show Spleef statistics. |

## Administration

| Command | Description |
|---|---|
| `/spleef create <name> [classic\|bow]` | Bind a new arena to the smallest WorldGuard region containing you. |
| `/spleef delete <arena>` | Delete an inactive arena. |
| `/spleef setwait <arena>` | Set its waiting spawn. |
| `/spleef setspectator <arena>` | Set its spectator spawn. |
| `/spleef addspawn <arena>` | Add a player spawn. |
| `/spleef clearspawns <arena>` | Remove all player spawns. |
| `/spleef layer <add\|remove\|list\|clear> <arena> [index]` | Manage floor layers. |
| `/spleef deathregion <arena>` | Select the elimination volume. |
| `/spleef mode <arena> <classic\|bow>` | Change game mode. |
| `/spleef enable <arena>` | Enable an arena. |
| `/spleef disable <arena>` | Disable an inactive arena. |
| `/spleef info <arena>` | Show setup and runtime state. |
| `/spleef reload` | Reload `config.yml`. |

Player permissions are `spleef.play`, `spleef.spectate`, and `spleef.stats`. Administration uses `spleef.admin`.

## Integrations

- BattleTracker combat tags prevent joining. Duels and Spleef activity are excluded from BattleTracker's SMP statistics.
- GameModeInventories-OG is suspended while Spleef owns a player's temporary inventory.
- HorseTp-OG and PetTeleport-OG suppress teleports while `SpleefAPI.isInSpleef` is true.
- PlayerBounties-OG claims and placements involving Spleef players are cancelled.

## API

```java
import net.trueog.spleefog.api.SpleefAPI;

if (SpleefAPI.isInSpleef(player)) {
    // Player is waiting, playing, or spectating.
}
```

`SpleefJoinEvent` and `SpleefLeaveEvent` are available in the same package.

## Building

```text
./gradlew build
```

The jar is written to `build/libs`.
