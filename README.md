# Spleef-OG

A standalone Classic and Bow Spleef plugin for Purpur / Paper 1.19.4.

Spleef-OG runs multiple concurrent arenas in WorldGuard regions inside ordinary worlds. It never creates, copies, or unloads a world; an arena is just a region of a world you already have.

## Gameplay

- Classic Spleef gives each player a shovel. Broken snow floors award snowballs, and thrown snowballs break configured floor blocks.
- Bow Spleef gives each player an infinity bow and one arrow. Arrow impacts break configured floor blocks.
- An arena starts a 20-second countdown as soon as at least two players are waiting. The countdown pauses if fewer than two remain.
- The last surviving player wins. Matches draw when the configured time limit expires.
- Waiting players, active players, and spectators have their inventory, location, gamemode, health, experience, effects, flight state, and scoreboard restored when they leave.
- Recovery snapshots survive plugin or server restarts.
- Players cannot walk, teleport, or command their way out of an arena while holding a Spleef kit. See Protection below.

Spleef statistics are stored in `plugins/Spleef-OG/stats.yml`. Use `/spleef stats [player]` to see wins, losses, ties, and games played.

## Requirements

- Java 17+
- Purpur / Paper 1.19.4
- WorldGuard 7

Every dependency resolves from a public Maven repository; nothing needs to be vendored to build this.

## Configuration

`config.yml` defaults to one allowed world:

```yaml
world-whitelist:
  - world
```

It also controls the minimum player count, 20-second waiting period, match time limit, victory delay, tools, and scoreboard.

Values may be written in MiniMessage or in legacy `&` colour codes; both are accepted in the same string.

### Protection

```yaml
protection:
  block-teleports: true
  block-all-commands: false
  whitelisted-commands: [msg, r, reply, tell, whisper, list]
  blacklisted-commands: [spawn, home, back, tp, gamemode, ...]
```

- `block-teleports` cancels any teleport whose destination is outside the arena region. Destinations inside the
  region are always allowed, so the plugin's own teleports need no special case.
- Command matching resolves namespaced spellings and registered aliases, so `home` also covers `essentials:home`.
  Multi-word entries such as `union home` match the start of what the player typed.
- `/spleef` is always permitted, so a player can never be trapped in an arena.
- `spleef.bypass.teleport` and `spleef.bypass.commands` exempt a player. Both default to nobody.

### Performance

```yaml
performance:
  reset-blocks-per-tick: 8192
```

Floor restoration is capped per tick. An arena larger than the budget finishes over the following ticks rather than
stalling the server. Restoration skips blocks that already match, clamps to the world's build limits, and loads the
chunks it needs deliberately instead of incidentally.

## Arena Setup

The region must deny building to ordinary players. Spleef re-allows only the block breaks a match needs; the floor
between matches, and everything in the arena that is not a layer, is protected by WorldGuard alone. A region created
with `/rg define` and no members does this by default. Spleef logs a warning if the region's flags say otherwise.

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
/spleef layer material classic minecraft:snow_block
/spleef deathregion classic
/spleef info classic
```

`layer add` and `deathregion` ask for two block clicks. After the second corner of a layer, run
`/spleef layer material <arena> <block data>` to finish it; the prompt is clickable and the block data argument
tab-completes. `/spleef info` lists whatever setup steps are still outstanding. Arena definitions are stored in
`plugins/Spleef-OG/arenas.yml`.

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
| `/spleef help` | Show the command list. |

## Administration

| Command | Description |
|---|---|
| `/spleef create <name> [classic\|bow]` | Bind a new arena to the smallest WorldGuard region containing you. |
| `/spleef delete <arena>` | Delete an inactive arena. |
| `/spleef setwait <arena>` | Set its waiting spawn. |
| `/spleef setspectator <arena>` | Set its spectator spawn. |
| `/spleef addspawn <arena>` | Add a player spawn. |
| `/spleef clearspawns <arena>` | Remove all player spawns. |
| `/spleef layer add <arena>` | Click two corners to select a floor layer. |
| `/spleef layer material <arena> <block data>` | Finish the selected layer. |
| `/spleef layer <remove\|list\|clear> <arena> [index]` | Manage existing layers. |
| `/spleef deathregion <arena>` | Select the elimination volume. |
| `/spleef mode <arena> <classic\|bow>` | Change game mode. |
| `/spleef enable <arena>` | Enable an arena. |
| `/spleef disable <arena>` | Disable an inactive arena. |
| `/spleef info <arena>` | Show setup and runtime state. |
| `/spleef reload` | Reload `config.yml` and `arenas.yml`. Arenas in use are left alone. |

Player permissions are `spleef.play`, `spleef.spectate`, and `spleef.stats`. Administration uses `spleef.admin`.
The bypasses `spleef.bypass.teleport` and `spleef.bypass.commands` default to nobody.

## Integrations

- Combat tags prevent joining. EternalCombat-OG is asked first and BattleTracker second, because BattleTracker's
  combat-log feature is commonly disabled where both are installed and would otherwise answer "not in combat" for
  everyone. Duels and Spleef activity are excluded from BattleTracker's SMP statistics.
- GameModeInventories-OG has `gamemodeinventories.use` and `.death` suspended while Spleef owns a player's inventory,
  and `.spectator` granted so eliminated players can actually become spectators. Because it vetoes adventure mode in
  worlds on its `restrict_adventure_worlds` list, Spleef clears that veto for its own players; without this the
  plugin cannot put anyone into adventure or spectator mode at all.
- Scoreboard-OG is asked to close its sidebar on entry and reopen it a tick after a player is restored. It asserts
  its sidebar only once and has no re-assert loop, so without this its sidebar stays blank after a match.
- Spawn-OG's respawn handling is overridden inside an arena by listening at a higher priority, and recovery after a
  relog is deferred a few ticks so its asynchronous login teleport cannot land on top of the restore.
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
