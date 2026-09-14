# Changelog

## 2.0.2

### Fixed

- GameModeInventories-OG suspension was keyed on the player's UUID, but a permission attachment belongs to one
  `Player` object. A player who left with a retained recovery entry (dead at quit, or a refused return teleport) and
  logged back in was restored with the suspension silently missing, so their kit or empty inventory could be filed
  as a real one. The attachment is now re-created whenever it belongs to a previous login.
- Corner selection for layers and death regions counted a right-click twice, once per hand, so a single click with
  an empty hand could set both corners on one block and save a one-block death region. Only the main hand counts.
- `/spleef create` printed its colour codes literally.
- Joining or spectating a refused arena printed two contradictory messages; each refusal is now explained once,
  including being in another arena already or having a recovery still pending.
- A pending layer or death-region selection survived `/spleef delete` of its arena and wrote into the deleted
  object, reporting success while saving nothing. Deleting an arena now cancels selections for it.
- Arena and recovery locations are stored by world name instead of as live `Location` objects. A live `Location`
  throws once its world is unloaded, which could abort a restore without parking the player and, because the
  exception escaped the recovery writer, stop `recovery.yml` from being saved for anyone. Arenas whose world is
  loaded after Spleef enables (by Multiverse or similar) also loaded with no spawns and were then saved back that way;
  they now load intact and become usable when the world appears. Old files are read transparently.
- `arenas.yml` and `stats.yml` were written in place, so a crash mid-write truncated them and the next save replaced
  the truncated file with whatever was in memory. All three data files now use the temporary-file-and-move write
  `recovery.yml` already had, and a file that fails to parse is moved aside under a `.corrupt-<timestamp>` name
  instead of being treated as empty and overwritten. Unreadable recovery entries are kept on disk verbatim.
- The combat-tag and Essentials hooks cached a plugin instance forever, so after `/plugman reload` of the provider
  they kept asking a dead object; the combat check then answered "not in combat" for everyone. Both now re-resolve
  whenever the provider instance changes, and the Essentials hook logs once instead of failing silently.
- `PlayerBounties-OG` protection ran at `NORMAL` priority, where a later listener could undo the cancellation. It now
  runs at `HIGHEST`.
- `SpleefJoinEvent` fired before the session listed the player, so a listener asking the API saw them as absent.
- A respawn inside a session whose arena world had vanished would have thrown on a null respawn location; the player
  is now taken out of the session instead.
- The per-player join cooldown map was never pruned, and the pending-recovery API set was not cleared on disable.
- A misleading "stand inside a flagged region" message when the WorldGuard flag itself had failed to register; the
  real cause is now reported to the admin.
- Documentation: `/spleef cancel` and the `spec` alias were undocumented, the `/spleef reload` and teleport-guard
  descriptions did not match the code, and the config value floors were not mentioned.

- An arena death kept the player's level but still spawned experience orbs for it, so every elimination was free
  experience for whoever collected the orbs. Arena deaths now drop none, and a player's own experience is held in
  their snapshot for the duration of a session instead of on the player.
- A player's inventory was handed back before they were teleported home and before their gamemode was verified.
  With GameModeInventories-OG refusing creative inside the arena, someone who joined from a creative region could
  leave with their creative inventory in survival. The teleport now comes first, the gamemode is checked at the
  destination, and the inventory is only released once that mode has stuck.
- A refused return teleport was treated as a completed restore, deleting the recovery entry and stranding the
  player in the arena with their real inventory. Every restore now reports its outcome; on failure the player is
  parked in survival with nothing, the entry is kept, and the teleport is retried before falling back to the next
  login. A refused match-start placement now removes that player from the match instead of leaving them off the
  floor with a kit.
- A failed write of `recovery.yml` was logged and ignored, so a disk or permission problem let a player enter with
  no durable copy of their inventory. Entry is now refused when the snapshot cannot be saved, and a failed write
  after a restore is retried until the obsolete copy on disk is gone.
- Floors larger than `reset-blocks-per-tick` were still being restored when the match started. The reset now
  begins with the countdown and the start is held until it has finished.
- A trident that had landed was forgotten by the pre-match trident return, so joining after a throw hit something
  left the trident behind or handed it into the temporary kit. Tridents are now tracked until their entity is gone.
- Players could teleport out of a running match with their Spleef kit. `PlayerMoveEvent` never fires for a
  teleport, so the region-exit check missed `/spawn`, `/home`, `/back`, `/tpa`, and every other teleport. Teleports
  landing outside the arena region are now cancelled.
- Disabling the plugin with anyone inside an arena threw `IllegalPluginAccessException` and aborted shutdown, so
  remaining players were never restored, floors were left dug out, and statistics were not saved. The scheduler is
  no longer used once the plugin is disabling, and one failing arena can no longer strand the others.
- GameModeInventories-OG cancels adventure mode in worlds on its `restrict_adventure_worlds` list and spectator
  mode when `restrict_spectator` is set, both before it consults `gamemodeinventories.use`. On a server whose main
  world is on that list this stopped Spleef from setting any gamemode at all. Spleef now clears that veto for its
  own players and grants `gamemodeinventories.spectator` for the duration of a session.
- An arena whose world could not be read was dropped from memory with a warning, and the next save erased it from
  `arenas.yml` for good. Definitions the plugin cannot read are now left on disk untouched.
- Restoring a player whose recorded world no longer exists skipped the teleport, leaving them standing in the arena.
  They now fall back to a world spawn.
- The arena sidebar was rebuilt from scratch once per second per player, which flickered and sent a full teardown
  every tick. Each arena now builds one scoreboard and updates the line text in place.

### Added

- Command blocking while in an arena, with `block-all-commands`, `whitelisted-commands`, and `blacklisted-commands`.
  Matching resolves namespaced spellings and registered aliases, and supports multi-word entries. `/spleef` is
  always allowed so a player cannot be trapped.
- `spleef.bypass.teleport` and `spleef.bypass.commands`, both defaulting to nobody.
- `/spleef layer material <arena> <block data>` with tab-completion over placeable blocks.
- `/spleef help`, and a full in-game command list split into player, setup, and administration sections.
- `/spleef info` now reports which setup steps are still outstanding.
- `/spleef reload` re-reads `arenas.yml` as well as `config.yml`, refusing while an arena is in use.
- Scoreboard-OG handover, so its sidebar comes back after a match instead of staying blank.

### Security

- Spectating skipped the combat-tag refusal that joining has, so `/spleef spectate` was a clean PvP escape: instant
  teleport into the arena, all damage cancelled, inventory and exact position restored on leaving.
- The bow no longer ships with Flame. An arrow sets its target alight before it deals damage, and the burn arrives
  later as a damager-less event, so cancelling the impact did not stop an arena player igniting anyone outside it.
  Arrows that land outside the arena are now removed as well.
- Portals and vehicles bypassed containment entirely. `PlayerPortalEvent` has its own handler list so the teleport
  guard never saw it, and passenger movement arrives as a vehicle packet rather than a player move, so riding out of
  the region was invisible. Portals are refused and session players cannot mount.
- The decorative TNT was a real primed entity kept harmless only by a scheduler task. Orphaned by a restart inside
  its one-second lifetime it detonated for real on the next chunk load. It is now tagged and refused permission to
  explode.
- The inventory, drop, pickup and interact guards moved to `LOWEST`. At `HIGHEST` they ran after a `NORMAL`-priority
  GUI plugin had already acted on the click.
- Joining is rate limited. Each entry snapshots the inventory and rewrites `recovery.yml`, so an unthrottled
  join/leave macro forced synchronous disk work every tick.
- Tab completion no longer lists every arena, including disabled and incomplete ones, to players without
  `spleef.admin`, and no longer scans the whole material registry for them.
- The carried cursor stack is captured and restored. Closing the inventory on entry dropped it on the ground.
- Creating an arena in a WorldGuard region that lets non-members build now logs a warning. Spleef only re-allows the
  specific breaks a match needs; everything else in the arena depends on that region denying them.

### Integration

- Combat-tag checks now ask EternalCombat-OG first and fall back to BattleTracker. Asking BattleTracker alone
  answered "not in combat" for everyone on any server that turns its combat-log feature off, which made joining an
  arena a free, lossless PvP escape.
- The gamemode veto is only cleared for the modes Spleef actually sets. Clearing it for creative let anyone with
  `/gmic` bypass the server's creative-region policy from inside an arena.
- Releasing GameModeInventories-OG re-checks that the player has not re-joined an arena in the meantime, so leaving
  and re-joining within one tick no longer runs a whole match with the suspension lifted.
- The respawn location is set at `MONITOR` instead of `HIGHEST`, where three plugins were writing it and ordering
  was decided by plugin load order.
- Scoreboard-OG is handed its sidebar back even while the plugin is disabling.
- Recovery after a relog re-checks the restored position a second later and reapplies it, instead of relying on a
  fixed delay to win a race against an asynchronous login teleport.

### Changed

- Floor restoration is bounded by `performance.reset-blocks-per-tick`. It resolves the WorldGuard region once
  instead of once per block, skips blocks that already match, clamps to the world's build limits, walks a chunk at
  a time, and continues over later ticks when an arena is larger than the budget.
- The layer block-data prompt no longer reads chat. Registering a listener for the legacy chat event makes the
  server route every player's chat down its legacy path, so the setup wizard was degrading chat server-wide.
- All output moved to Adventure. Configured strings accept MiniMessage or legacy `&` codes, including mixed in one
  string and uppercase codes, which the previous legacy-only path did not handle.
- WorldGuard flag registration survives a mid-session plugin reload instead of throwing out of `onLoad`.
- Dropped the unused MyWorlds soft dependency and the config comment that described it.
- Declared the EternalCombat-OG, BattleTracker, Scoreboard-OG, and Spawn-OG soft dependencies that were already
  being used.

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
