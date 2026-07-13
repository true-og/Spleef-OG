package net.trueog.spleefog.arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import net.trueog.spleefog.hook.GameModeInventoriesHook;
import net.trueog.spleefog.hook.BattleTrackerHook;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import net.trueog.spleefog.player.PlayerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;

public final class ArenaManager implements Listener {

    private final SpleefPlugin plugin;
    private final SpleefConfig config;
    private final WorldGuardSupport worldGuard;
    private final ArenaRepository arenaRepository;
    private final StatsRepository stats;
    private final RecoveryStore recovery;
    private final GameModeInventoriesHook gameModeInventories;
    private final BattleTrackerHook battleTracker;
    private final Map<String, ArenaSession> sessions = new HashMap<>();
    private final Map<UUID, ArenaSession> playerSessions = new HashMap<>();
    private final Map<UUID, ArenaSession> pendingRespawns = new HashMap<>();
    private final Set<UUID> forcedDeaths = new HashSet<>();
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
        this.battleTracker = new BattleTrackerHook(plugin);
        for (SpleefArena arena : arenaRepository.load()) {

            this.sessions.put(normalize(arena.name()), new ArenaSession(this, arena));

        }

    }

    public void start() {

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {

            this.restoreRecovery(player);

        }

        for (ArenaSession session : this.sessions.values()) {

            if (this.isArenaRuntimeValid(session.arena())) {

                this.resetLayers(session.arena());

            }

        }

        this.tickTask = Bukkit.getScheduler().runTaskTimer(this.plugin,
                () -> new ArrayList<>(this.sessions.values()).forEach(ArenaSession::tick), 20L, 20L);

    }

    public void shutdown() {

        if (this.tickTask != null) {

            this.tickTask.cancel();

        }

        for (ArenaSession session : new ArrayList<>(this.sessions.values())) {

            session.shutdown();

        }

        this.playerSessions.clear();
        this.pendingRespawns.clear();
        this.forcedDeaths.clear();
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

        return this.battleTracker.isInCombat(player);

    }

    public List<ArenaSession> sessions() {

        return this.sessions.values().stream().sorted(Comparator.comparing(value -> value.arena().name())).toList();

    }

    public List<ArenaSession> availableSessions() {

        return this.sessions().stream().filter(ArenaSession::canJoin).toList();

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
        this.saveArenas();
        return true;

    }

    public void saveArenas() {

        this.arenaRepository.save(this.sessions.values().stream().map(ArenaSession::arena).toList());

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

    boolean enter(Player player, ArenaSession session, boolean spectator, Location destination) {

        if (destination == null || destination.getWorld() == null || this.recovery.contains(player.getUniqueId())) {

            return false;

        }

        this.recovery.capture(player);
        this.gameModeInventories.suspend(player);
        this.playerSessions.put(player.getUniqueId(), session);
        SpleefAPI.markJoined(player);
        this.preparePlayer(player, spectator ? GameMode.SPECTATOR : GameMode.ADVENTURE, destination);
        Bukkit.getPluginManager().callEvent(new SpleefJoinEvent(player, session.arena().name(), spectator));
        return true;

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

            try {

                snapshot.restore(player);
                this.recovery.remove(player.getUniqueId());

            } catch (RuntimeException ex) {

                this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not restore " + player.getName() + " after Spleef; recovery data was retained.", ex);

            }

        } else {

            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        }

        Bukkit.getScheduler().runTask(this.plugin, () -> this.gameModeInventories.release(player));

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

    void resetLayers(SpleefArena arena) {

        World world = Bukkit.getWorld(arena.worldName());
        if (world == null) {

            return;

        }

        for (SpleefLayer layer : arena.layers()) {

            for (int x = layer.bounds().minX(); x <= layer.bounds().maxX(); x++) {

                for (int y = layer.bounds().minY(); y <= layer.bounds().maxY(); y++) {

                    for (int z = layer.bounds().minZ(); z <= layer.bounds().maxZ(); z++) {

                        Location location = new Location(world, x, y, z);
                        if (this.worldGuard.isInside(location, arena.regionId())) {

                            world.getBlockAt(x, y, z).setBlockData(layer.blockData(), false);

                        }

                    }

                }

            }

        }

    }

    void updateScoreboards(ArenaSession session) {

        if (!this.config.scoreboardEnabled()) {

            return;

        }

        for (UUID playerId : session.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                player.setScoreboard(this.createScoreboard(session));

            }

        }

    }

    private Scoreboard createScoreboard(ArenaSession session) {

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("spleef", Criteria.DUMMY, this.config.scoreboardTitle());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore(ChatColor.GRAY + "Arena: " + ChatColor.WHITE + session.arena().name()).setScore(5);
        objective.getScore(ChatColor.GRAY + "Mode: " + ChatColor.WHITE + display(session.arena().gameType()))
                .setScore(4);
        objective.getScore(ChatColor.GRAY + "State: " + ChatColor.WHITE + display(session.state())).setScore(3);
        objective
                .getScore(ChatColor.GRAY + "Players: " + ChatColor.AQUA
                        + (session.state() == ArenaState.IN_GAME ? session.aliveCount() : session.playerCount()))
                .setScore(2);
        String timer = session.state() == ArenaState.WAITING ? "Waiting" : formatTime(session.secondsRemaining());
        objective.getScore(ChatColor.GRAY + "Time: " + ChatColor.AQUA + timer).setScore(1);
        return board;

    }

    private void preparePlayer(Player player, GameMode gameMode, Location destination) {

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
        player.teleport(destination);

    }

    private void restoreRecovery(Player player) {

        if (this.playerSessions.containsKey(player.getUniqueId())) {

            return;

        }

        PlayerSnapshot snapshot = this.recovery.get(player.getUniqueId());
        if (snapshot != null) {

            this.gameModeInventories.suspend(player);
            try {

                snapshot.restore(player);
                this.recovery.remove(player.getUniqueId());
                Messages.send(player, "&7Your pre-Spleef state was recovered after a restart.");

            } catch (RuntimeException ex) {

                this.plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not recover " + player.getName() + " after a restart; recovery data was retained.", ex);

            } finally {

                Bukkit.getScheduler().runTask(this.plugin, () -> this.gameModeInventories.release(player));

            }

        }

    }

    private ArenaSession activeSessionAt(Location location) {

        return this.sessions.values().stream().filter(ArenaSession::isActive)
                .filter(session -> this.worldGuard.isInside(location, session.arena().regionId())).findFirst()
                .orElse(null);

    }

    private SpleefLayer layerAt(SpleefArena arena, Location location) {

        if (!this.worldGuard.isInside(location, arena.regionId())) {

            return null;

        }

        return arena.layers().stream().filter(layer -> layer.bounds().contains(location)).findFirst().orElse(null);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        ArenaSession session = this.session(player);
        ArenaSession regionSession = this.activeSessionAt(event.getBlock().getLocation());
        if (session == null && regionSession == null) {

            return;

        }

        event.setCancelled(true);
        if (session == null || session.state() != ArenaState.IN_GAME || !session.isAlive(player.getUniqueId())
                || session.arena().gameType() != GameType.CLASSIC)
        {

            return;

        }

        SpleefLayer layer = this.layerAt(session.arena(), event.getBlock().getLocation());
        if (layer == null) {

            return;

        }

        event.setCancelled(false);
        event.setDropItems(false);
        int snowballs = snowballCount(event.getBlock());
        if (snowballs > 0) {

            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, snowballs));

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (this.session(event.getPlayer()) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        ArenaSession session = this.session(event.getPlayer());
        if (session != null && session.state() != ArenaState.IN_GAME) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {

        if (this.session(event.getPlayer()) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickup(EntityPickupItemEvent event) {

        if (event.getEntity() instanceof Player player && this.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player player && this.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player) || this.session(player) == null) {

            return;

        }

        if (!this.forcedDeaths.contains(player.getUniqueId())) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFood(FoodLevelChangeEvent event) {

        if (event.getEntity() instanceof Player player && this.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event) {

        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {

            return;

        }

        ArenaSession session = this.session(event.getPlayer());
        if (session == null || !session.isAlive(event.getPlayer().getUniqueId())) {

            return;

        }

        if (session.arena().deathRegion().contains(event.getTo())
                || !this.worldGuard.isInside(event.getTo(), session.arena().regionId()))
        {

            this.forcedDeaths.add(event.getPlayer().getUniqueId());
            event.getPlayer().setHealth(0.0D);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ArenaSession session = this.session(player);
        if (session == null || !session.isAlive(player.getUniqueId())) {

            return;

        }

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.deathMessage(null);
        this.forcedDeaths.remove(player.getUniqueId());
        this.pendingRespawns.put(player.getUniqueId(), session);
        session.eliminate(player);
        Bukkit.getScheduler().runTask(this.plugin, () -> {

            if (player.isOnline() && player.isDead()) {

                player.spigot().respawn();

            }

        });

    }

    @EventHandler(priority = EventPriority.HIGHEST)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {

        Block block = event.getHitBlock();
        Projectile projectile = event.getEntity();
        ProjectileSource source = projectile.getShooter();
        if (block == null || !(source instanceof Player player)) {

            return;

        }

        ArenaSession session = this.session(player);
        if (session == null || !session.isAlive(player.getUniqueId()) || session.state() != ArenaState.IN_GAME) {

            return;

        }

        boolean validProjectile = session.arena().gameType() == GameType.CLASSIC && projectile instanceof Snowball
                || session.arena().gameType() == GameType.BOW && projectile instanceof AbstractArrow;
        if (!validProjectile || this.layerAt(session.arena(), block.getLocation()) == null) {

            return;

        }

        block.setType(Material.AIR, false);
        projectile.remove();

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTntPrime(TNTPrimeEvent event) {

        ArenaSession session = this.activeSessionAt(event.getBlock().getLocation());
        if (session == null || session.state() != ArenaState.IN_GAME || session.arena().gameType() != GameType.BOW
                || this.layerAt(session.arena(), event.getBlock().getLocation()) == null)
        {

            return;

        }

        event.setCancelled(true);
        event.getBlock().setType(Material.AIR, false);
        TNTPrimed visual = event.getBlock().getWorld().spawn(event.getBlock().getLocation().toCenterLocation(),
                TNTPrimed.class, tnt -> tnt.setFuseTicks(Integer.MAX_VALUE));
        Bukkit.getScheduler().runTaskLater(this.plugin, visual::remove, 20L);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplosion(EntityExplodeEvent event) {

        event.blockList().removeIf(block -> {

            ArenaSession session = this.activeSessionAt(block.getLocation());
            return session != null && this.layerAt(session.arena(), block.getLocation()) != null;

        });

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {

        ArenaSession session = this.session(event.getPlayer());
        if (session != null) {

            session.leave(event.getPlayer());

        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        Bukkit.getScheduler().runTask(this.plugin, () -> this.restoreRecovery(event.getPlayer()));

    }

    private static int snowballCount(Block block) {

        if (block.getType() == Material.SNOW_BLOCK) {

            return 4;

        }

        BlockData data = block.getBlockData();
        if (data instanceof Snow snow) {

            return snow.getLayers();

        }

        return 0;

    }

    private static boolean sameBlock(Location first, Location second) {

        return first.getBlockX() == second.getBlockX() && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();

    }

    private static String formatTime(int seconds) {

        return String.format(Locale.ROOT, "%d:%02d", Math.max(0, seconds) / 60, Math.max(0, seconds) % 60);

    }

    private static String display(Enum<?> value) {

        String name = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);

    }

    private static String normalize(String name) {

        return name.toLowerCase(Locale.ROOT);

    }

    public record CreateResult(boolean success, String message, SpleefArena arena) {
    }

}
