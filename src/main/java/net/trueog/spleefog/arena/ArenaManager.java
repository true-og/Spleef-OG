package net.trueog.spleefog.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.SpleefConfig;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.WorldGuardSupport;
import net.trueog.spleefog.api.SpleefAPI;
import net.trueog.spleefog.api.SpleefJoinEvent;
import net.trueog.spleefog.api.SpleefLeaveEvent;
import net.trueog.spleefog.data.ArenaRepository;
import net.trueog.spleefog.data.RecoveryStore;
import net.trueog.spleefog.data.StatsRepository;
import net.trueog.spleefog.hook.CombatHook;
import net.trueog.spleefog.hook.EssentialsHook;
import net.trueog.spleefog.hook.GameModeInventoriesHook;
import net.trueog.spleefog.hook.ScoreboardOGHook;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.player.PlayerSnapshot;
import net.trueog.spleefog.player.ThrownProjectileTracker;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

// Owns the arenas, who is in them, and the player state Spleef borrows while they are.
//
// The event handling is split across three collaborators so that each has one job and its coverage can be read off
// in one place: Confinement keeps players inside an arena, Protection stops anything touching them or being touched
// by them, and ArenaWorld owns blocks and floor restoration. What stays here is the session registry and the
// deliberate placement of players: entering, starting, ending, respawning, and being put back afterwards.
public final class ArenaManager implements Listener {

    public static final String ADMIN = "spleef.admin";
    public static final String BYPASS_TELEPORT = "spleef.bypass.teleport";
    public static final String BYPASS_COMMANDS = "spleef.bypass.commands";
    private static final long RECOVERY_DELAY_TICKS = 5L;
    private static final long RECOVERY_CONFIRM_TICKS = 20L;
    // Far enough that ordinary walking in the second after login never trips it,
    // close enough that being sent to a
    // spawn point always does.
    private static final double RECOVERY_DRIFT_SQUARED = 16.0D * 16.0D;
    private static final long ENTRY_COOLDOWN_MILLIS = 2000L;

    private final SpleefPlugin plugin;
    private final SpleefConfig config;
    private final WorldGuardSupport worldGuard;
    private final ArenaRepository arenaRepository;
    private final StatsRepository stats;
    private final RecoveryStore recovery;
    private final GameModeInventoriesHook gameModeInventories;
    private final CombatHook combat;
    private final ScoreboardOGHook scoreboardOG;
    private final ArenaWorld arenaWorld;
    private final Confinement confinement;
    private final Protection protection;
    private final ThrownProjectileTracker thrownProjectiles;
    private final EssentialsHook essentials;
    private final Map<String, ArenaSession> sessions = new HashMap<>();
    private final Map<UUID, ArenaSession> playerSessions = new HashMap<>();
    private final Map<UUID, ArenaSession> pendingRespawns = new HashMap<>();
    private final Set<UUID> forcedDeaths = new HashSet<>();
    private final Set<UUID> restoring = new HashSet<>();
    private final Map<UUID, Long> lastEntry = new HashMap<>();
    private BukkitTask tickTask;

    public ArenaManager(SpleefPlugin plugin, SpleefConfig config, WorldGuardSupport worldGuard,
            ArenaRepository arenaRepository, StatsRepository stats, RecoveryStore recovery,
            GameModeInventoriesHook gameModeInventories)
    {

        this.plugin = plugin;
        this.config = config;
        this.worldGuard = worldGuard;
        this.arenaRepository = arenaRepository;
        this.stats = stats;
        this.recovery = recovery;
        this.gameModeInventories = gameModeInventories;
        this.combat = new CombatHook(plugin);
        this.essentials = new EssentialsHook();
        this.scoreboardOG = new ScoreboardOGHook(plugin);
        this.arenaWorld = new ArenaWorld(plugin, this, worldGuard);
        this.confinement = new Confinement(this);
        this.protection = new Protection(this);
        this.thrownProjectiles = new ThrownProjectileTracker();
        for (SpleefArena arena : arenaRepository.load()) {

            this.sessions.put(normalize(arena.name()), new ArenaSession(this, arena));

        }

    }

    public void start() {

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        Bukkit.getPluginManager().registerEvents(this.confinement, this.plugin);
        Bukkit.getPluginManager().registerEvents(this.protection, this.plugin);
        Bukkit.getPluginManager().registerEvents(this.arenaWorld, this.plugin);
        Bukkit.getPluginManager().registerEvents(this.thrownProjectiles, this.plugin);
        this.arenaWorld.start();

        for (Player player : Bukkit.getOnlinePlayers()) {

            this.restoreRecovery(player);

        }

        this.resetAllArenas();
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickSessions, 20L, 20L);

    }

    private void tickSessions() {

        for (ArenaSession session : new ArrayList<>(this.sessions.values())) {

            try {

                session.tick();

            } catch (RuntimeException ex) {

                // Without this a single bad arena stops every arena after it in the list from
                // ever ticking again,
                // freezing their countdowns and leaving their players stuck in a match that
                // cannot end.
                this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Spleef arena " + session.arena().name() + " failed to tick.", ex);

            }

        }

    }

    public void shutdown() {

        if (this.tickTask != null) {

            this.tickTask.cancel();

        }

        this.arenaWorld.shutdown();
        for (ArenaSession session : new ArrayList<>(this.sessions.values())) {

            try {

                session.shutdown();

            } catch (RuntimeException ex) {

                // One bad arena must not strand the players waiting in every other arena.
                this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not cleanly shut down Spleef arena " + session.arena().name() + ".", ex);

            }

        }

        this.playerSessions.clear();
        this.pendingRespawns.clear();
        this.forcedDeaths.clear();
        this.restoring.clear();
        this.lastEntry.clear();
        this.thrownProjectiles.clear();
        this.stats.save();
        this.gameModeInventories.releaseAll();

    }

    public SpleefConfig config() {

        return this.config;

    }

    public StatsRepository stats() {

        return this.stats;

    }

    public ArenaSession session(Player player) {

        return player == null ? null : this.playerSessions.get(player.getUniqueId());

    }

    public ArenaSession get(String name) {

        return name == null ? null : this.sessions.get(normalize(name));

    }

    public boolean isInCombat(Player player) {

        return this.combat.isInCombat(player);

    }

    public List<ArenaSession> sessions() {

        return this.sessions.values().stream().sorted(Comparator.comparing(value -> value.arena().name())).toList();

    }

    public List<ArenaSession> availableSessions() {

        return this.sessions().stream().filter(ArenaSession::canJoin).toList();

    }

    Collection<ArenaSession> allSessions() {

        return this.sessions.values();

    }

    // Region ids are only unique within a world, so the world has to be compared
    // too. Without this an arena in the
    // main world reacts to blocks broken in an identically named region of a build
    // or staging world.
    boolean isInsideArena(SpleefArena arena, Location location) {

        return location != null && location.getWorld() != null
                && location.getWorld().getName().equalsIgnoreCase(arena.worldName())
                && this.worldGuard.isInside(location, arena.regionId());

    }

    // Set when the plugin kills a player itself, so the damage guard lets that one
    // death through.
    void markForcedDeath(UUID playerId) {

        this.forcedDeaths.add(playerId);

    }

    boolean isForcedDeath(UUID playerId) {

        return this.forcedDeaths.contains(playerId);

    }

    // True while this plugin is the reason the player's gamemode is changing,
    // either because they are in a session
    // or because their pre-Spleef state is being put back.
    boolean ownsGameModeOf(UUID playerId) {

        return this.playerSessions.containsKey(playerId) || this.restoring.contains(playerId);

    }

    // True while this plugin is putting a player's pre-Spleef state back. Their
    // stored location may sit inside an arena, so that teleport must not be
    // mistaken for an outsider walking in.
    boolean isRestoring(UUID playerId) {

        return this.restoring.contains(playerId);

    }

    public CreateResult create(Player player, String name, GameType gameType) {

        if (!name.matches("[A-Za-z0-9_-]+")) {

            return new CreateResult(false, "Arena names may contain letters, numbers, underscores, and hyphens.", null);

        }

        if (this.get(name) != null) {

            return new CreateResult(false, "An arena with that name already exists.", null);

        }

        if (!this.config.isWorldAllowed(player.getWorld().getName())) {

            return new CreateResult(false, "World &b" + player.getWorld().getName() + " &7is not whitelisted.", null);

        }

        WorldGuardSupport.RegionBinding region = this.worldGuard.findArenaRegion(player);
        if (region == null) {

            return new CreateResult(false,
                    "Stand inside a WorldGuard region with &ballow-spleef=allow &7before creating an arena.", null);

        }

        this.warnIfRegionAllowsBuilding(player.getWorld(), region.id());
        SpleefArena arena = new SpleefArena(name, player.getWorld().getName(), region.id(), region.bounds(), gameType);
        this.sessions.put(normalize(name), new ArenaSession(this, arena));
        this.saveArenas();
        return new CreateResult(true, "Created &b" + name + " &7in region &b" + region.id() + "&7.", arena);

    }

    public boolean delete(SpleefArena arena) {

        ArenaSession session = this.get(arena.name());
        if (session == null || session.isActive()) {

            return false;

        }

        this.sessions.remove(normalize(arena.name()));
        this.arenaWorld.forget(arena.name());
        this.saveArenas();
        return true;

    }

    public void saveArenas() {

        this.arenaRepository.save(this.sessions.values().stream().map(ArenaSession::arena).toList());

    }

    // Re-reads arenas.yml. Refused outright while any arena has players in it,
    // because swapping the definitions out
    // from under a running match would strand everyone in it.
    public ReloadResult reloadArenas() {

        List<String> busy = this.sessions.values().stream().filter(ArenaSession::isActive)
                .map(session -> session.arena().name()).sorted().toList();
        if (!busy.isEmpty()) {

            return new ReloadResult(false, busy, 0);

        }

        for (ArenaSession session : this.sessions.values()) {

            session.releaseScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        }

        this.arenaWorld.forgetAll();
        this.sessions.clear();
        for (SpleefArena arena : this.arenaRepository.load()) {

            this.sessions.put(normalize(arena.name()), new ArenaSession(this, arena));

        }

        this.resetAllArenas();
        return new ReloadResult(true, List.of(), this.sessions.size());

    }

    private void resetAllArenas() {

        for (ArenaSession session : this.sessions.values()) {

            if (this.isArenaRuntimeValid(session.arena())) {

                this.resetLayers(session.arena());

            }

        }

    }

    public boolean isArenaRuntimeValid(SpleefArena arena) {

        World world = Bukkit.getWorld(arena.worldName());
        return world != null && this.config.isWorldAllowed(arena.worldName())
                && this.worldGuard.regionExists(world, arena.regionId());

    }

    public boolean isSetupLocationValid(SpleefArena arena, Location location) {

        return location != null && location.getWorld() != null
                && location.getWorld().getName().equalsIgnoreCase(arena.worldName())
                && this.worldGuard.isInside(location, arena.regionId());

    }

    // Spleef only re-allows the specific block breaks a match needs; everything
    // else in the arena is protected by
    // WorldGuard denying non-members. If the region does not deny them, players can
    // dig the floor between matches
    // and build in it during one, and nothing in this plugin will stop them.
    private void warnIfRegionAllowsBuilding(World world, String regionId) {

        if (!this.worldGuard.allowsNonMemberBuilding(world, regionId)) {

            return;

        }

        this.plugin.getLogger().warning("WorldGuard region '" + regionId
                + "' lets non-members build. Spleef relies on that region denying them, so players will be able to "
                + "dig the arena floor outside matches. Remove the build/passthrough allow flag on that region.");

    }

    void resetLayers(SpleefArena arena) {

        this.arenaWorld.resetLayers(arena);

    }

    boolean enter(Player player, ArenaSession session, boolean spectator, Location destination) {

        if (destination == null || destination.getWorld() == null || player.isDead()
                || this.recovery.contains(player.getUniqueId()))
        {

            return false;

        }

        Long last = this.lastEntry.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < ENTRY_COOLDOWN_MILLIS) {

            // Each entry snapshots the inventory and rewrites recovery.yml, so an
            // unthrottled join/leave macro is a
            // cheap way to make the server do synchronous disk work every tick.
            Messages.send(player, Messages.bad("Wait a moment before joining Spleef again."));
            return false;

        }

        this.lastEntry.put(player.getUniqueId(), now);
        PlayerSnapshot snapshot = this.recovery.capture(player);
        // A trident still in flight is pulled out of the world and rides the snapshot
        // home; a pearl in flight is dropped so it cannot fire a teleport mid-match.
        if (this.thrownProjectiles.stashOnEntry(player, snapshot)) {

            this.recovery.persist();

        }

        this.gameModeInventories.suspend(player);
        this.scoreboardOG.close(player);
        this.playerSessions.put(player.getUniqueId(), session);
        SpleefAPI.markJoined(player);
        if (!this.preparePlayer(player, spectator ? GameMode.SPECTATOR : GameMode.ADVENTURE, destination)) {

            // Something refused the teleport. Undo the entry completely instead of
            // stranding the player in the
            // world with a cleared inventory and a session they cannot leave.
            this.playerSessions.remove(player.getUniqueId());
            SpleefAPI.markLeft(player);
            this.restoreAfterFailedEntry(player);
            return false;

        }

        Bukkit.getPluginManager().callEvent(new SpleefJoinEvent(player, session.arena().name(), spectator));
        return true;

    }

    private void restoreAfterFailedEntry(Player player) {

        PlayerSnapshot snapshot = this.recovery.get(player.getUniqueId());
        this.restoring.add(player.getUniqueId());
        try {

            if (snapshot != null) {

                snapshot.restore(player);
                this.essentials.setBackLocation(player, snapshot.location());
                this.recovery.remove(player.getUniqueId());

            }

        } catch (RuntimeException ex) {

            this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not undo a failed Spleef entry for " + player.getName() + "; recovery data was retained.",
                    ex);

        } finally {

            this.restoring.remove(player.getUniqueId());

        }

        this.releaseGameModeInventories(player);
        this.scoreboardOG.reopenLater(player);

    }

    void exit(Player player, ArenaSession expectedSession) {

        ArenaSession current = this.playerSessions.get(player.getUniqueId());
        if (current != expectedSession) {

            return;

        }

        this.playerSessions.remove(player.getUniqueId());
        this.pendingRespawns.remove(player.getUniqueId());
        this.forcedDeaths.remove(player.getUniqueId());
        SpleefAPI.markLeft(player);
        Bukkit.getPluginManager().callEvent(new SpleefLeaveEvent(player, expectedSession.arena().name()));

        PlayerSnapshot snapshot = this.recovery.get(player.getUniqueId());
        if (snapshot != null && !player.isDead()) {

            this.restoring.add(player.getUniqueId());
            try {

                snapshot.restore(player);
                this.essentials.setBackLocation(player, snapshot.location());
                this.recovery.remove(player.getUniqueId());

            } catch (RuntimeException ex) {

                this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not restore " + player.getName() + " after Spleef; recovery data was retained.", ex);

            } finally {

                this.restoring.remove(player.getUniqueId());

            }

        } else {

            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        }

        this.releaseGameModeInventories(player);
        this.scoreboardOG.reopenLater(player);

    }

    // Hands the player back to GameModeInventories-OG a tick later so its gamemode
    // listener has already seen the
    // restore, but never through the scheduler while the plugin is disabling; that
    // throws and would abort shutdown.
    private void releaseGameModeInventories(Player player) {

        if (!this.plugin.isEnabled()) {

            this.gameModeInventories.release(player);
            return;

        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {

            // The player may have re-joined an arena in the meantime; releasing now would
            // leave the rest of that
            // match running with the suspension lifted.
            if (!this.playerSessions.containsKey(player.getUniqueId())) {

                this.gameModeInventories.release(player);

            }

        });

    }

    void startPlayer(Player player, SpleefArena arena, Location spawn) {

        this.preparePlayer(player, GameMode.SURVIVAL, spawn);
        if (arena.gameType() == GameType.CLASSIC) {

            player.getInventory().setItem(0, this.config.classicTool());

        } else {

            player.getInventory().setItem(0, this.config.bowTool());
            player.getInventory().setItem(9, new ItemStack(Material.ARROW));

        }

        player.updateInventory();

    }

    void showEnding(ArenaSession session) {

        for (UUID playerId : session.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && !player.isDead()) {

                this.preparePlayer(player, GameMode.ADVENTURE, session.arena().waitingSpawn());

            }

        }

    }

    // Refreshes the arena sidebar in place. The board is built once per session, so
    // a tick only rewrites the line
    // text; players are never handed a replacement scoreboard unless they do not
    // have this one yet.
    void updateScoreboards(ArenaSession session) {

        if (!this.config.scoreboardEnabled()) {

            session.releaseScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            return;

        }

        Set<UUID> present = session.allPresent();
        if (present.isEmpty()) {

            return;

        }

        ArenaScoreboard board = session.scoreboard(this.config.scoreboardTitle());
        board.title(this.config.scoreboardTitle());
        board.line(0, Messages.body().append(Component.text("Arena: ")).append(Messages.name(session.arena().name()))
                .build());
        board.line(1, Messages.body().append(Component.text("Mode: "))
                .append(Messages.name(display(session.arena().gameType()))).build());
        board.line(2, Messages.body().append(Component.text("State: ")).append(Messages.name(display(session.state())))
                .build());
        int count = session.state() == ArenaState.IN_GAME ? session.aliveCount() : session.playerCount();
        board.line(3, Messages.body().append(Component.text("Players: "))
                .append(Messages.value(Integer.toString(count))).build());
        String timer = session.state() == ArenaState.WAITING ? "Waiting" : formatTime(session.secondsRemaining());
        board.line(4, Messages.body().append(Component.text("Time: ")).append(Messages.value(timer)).build());

        for (UUID playerId : present) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.getScoreboard() != board.board()) {

                player.setScoreboard(board.board());

            }

        }

    }

    // Returns false when the teleport was refused. The inventory has already been
    // cleared by then, so a caller that
    // cannot proceed has to put the player back rather than leave them empty-handed
    // out in the world.
    private boolean preparePlayer(Player player, GameMode gameMode, Location destination) {

        player.setItemOnCursor(null);
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {

            player.removePotionEffect(effect.getType());

        }

        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setAllowFlight(gameMode == GameMode.SPECTATOR);
        player.setGameMode(gameMode);
        return player.teleport(destination);

    }

    private void restoreRecovery(Player player) {

        // Offline: restoring onto a detached Player writes into an entity that will
        // never be saved, and the
        // recovery entry would then be deleted, losing the only surviving copy of the
        // real inventory.
        // Dead: setHealth on a player sitting on the death screen leaves them unable to
        // respawn at all. The respawn
        // handler picks them up instead.
        if (this.playerSessions.containsKey(player.getUniqueId()) || !player.isOnline() || player.isDead()) {

            return;

        }

        PlayerSnapshot snapshot = this.recovery.get(player.getUniqueId());
        if (snapshot == null) {

            return;

        }

        this.gameModeInventories.suspend(player);
        this.restoring.add(player.getUniqueId());
        try {

            Location expected = snapshot.location();
            snapshot.restore(player);
            this.essentials.setBackLocation(player, snapshot.location());
            this.recovery.remove(player.getUniqueId());
            this.scoreboardOG.reopenLater(player);
            this.confirmRecoveryLocation(player, expected);
            Messages.send(player, Messages.grey("Your pre-Spleef state was recovered after a restart."));

        } catch (RuntimeException ex) {

            this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not recover " + player.getName() + " after a restart; recovery data was retained.", ex);

        } finally {

            this.restoring.remove(player.getUniqueId());
            this.releaseGameModeInventories(player);

        }

    }

    // A spawn-management plugin may teleport a joining player asynchronously, and
    // that teleport can resolve after
    // the restore has already run. Rather than guess a delay long enough to lose
    // that race, the restored position
    // is checked once a second later and reapplied if something moved the player
    // somewhere else entirely.
    private void confirmRecoveryLocation(Player player, Location expected) {

        if (expected == null || expected.getWorld() == null || !this.plugin.isEnabled()) {

            return;

        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {

            if (!player.isOnline() || player.isDead() || this.playerSessions.containsKey(player.getUniqueId())) {

                return;

            }

            Location now = player.getLocation();
            if (now.getWorld() == expected.getWorld() && now.distanceSquared(expected) <= RECOVERY_DRIFT_SQUARED) {

                return;

            }

            player.teleport(expected);
            Messages.send(player, Messages.grey("Returned you to where you were before Spleef."));

        }, RECOVERY_CONFIRM_TICKS);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ArenaSession session = this.session(player);
        if (session == null) {

            return;

        }

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.deathMessage(null);
        this.forcedDeaths.remove(player.getUniqueId());
        this.pendingRespawns.put(player.getUniqueId(), session);
        // Anything that bypasses the damage event, such as /kill or a plugin setting
        // health directly, can kill a
        // waiting player or a spectator. They still have to keep their kit and respawn
        // inside the arena.
        if (session.isAlive(player.getUniqueId())) {

            session.eliminate(player);

        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {

            if (player.isOnline() && player.isDead()) {

                player.spigot().respawn();

            }

        });

    }

    // At MONITOR because several plugins write the respawn location at HIGHEST and
    // ordering between them is decided
    // by plugin load order. The event cannot be cancelled, so having the last word
    // here is safe.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {

        ArenaSession session = this.pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (session == null) {

            if (this.recovery.contains(event.getPlayer().getUniqueId())) {

                Bukkit.getScheduler().runTask(this.plugin, () -> this.restoreRecovery(event.getPlayer()));

            }

            return;

        }

        Location destination = session.state() == ArenaState.IN_GAME ? session.arena().spectatorSpawn()
                : session.arena().waitingSpawn();
        event.setRespawnLocation(destination);
        Bukkit.getScheduler().runTask(this.plugin, () -> {

            if (this.session(event.getPlayer()) == session) {

                this.preparePlayer(event.getPlayer(),
                        session.state() == ArenaState.IN_GAME ? GameMode.SPECTATOR : GameMode.ADVENTURE, destination);

            } else {

                this.restoreRecovery(event.getPlayer());

            }

        });

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {

        ArenaSession session = this.session(event.getPlayer());
        if (session != null) {

            session.leave(event.getPlayer());

        }

    }

    // Deliberately delayed rather than run on the next tick. A spawn-management
    // plugin may be teleporting this
    // player asynchronously from its own join handler, and an async teleport that
    // lands after the restore would
    // leave the player at spawn instead of where they stood before they joined
    // Spleef.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.restoreRecovery(event.getPlayer()),
                RECOVERY_DELAY_TICKS);

    }

    private static String formatTime(int seconds) {

        return String.format(Locale.ROOT, "%d:%02d", Math.max(0, seconds) / 60, Math.max(0, seconds) % 60);

    }

    private static String display(Enum<?> value) {

        String name = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);

    }

    static String normalize(String name) {

        return name.toLowerCase(Locale.ROOT);

    }

    public record CreateResult(boolean success, String message, SpleefArena arena) {
    }

    public record ReloadResult(boolean success, List<String> busyArenas, int arenaCount) {
    }

}
