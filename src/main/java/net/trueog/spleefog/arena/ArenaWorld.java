package net.trueog.spleefog.arena;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.WorldGuardSupport;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

// Everything that touches blocks: which of them a match may destroy, and putting them back afterwards.
//
// The arena itself is protected by WorldGuard denying non-members. Spleef only ever re-allows the specific breaks a
// match needs, and puts the floor back when it ends. Nothing here should widen what a player can destroy beyond the
// configured layers inside the arena region.
public final class ArenaWorld implements Listener {

    // Long enough to outlive the tick that removes it, short enough that a leaked
    // entity is not a lasting hazard.
    // The fuse is stored as a short, so Integer.MAX_VALUE would wrap round to an
    // instant detonation.
    private static final int VISUAL_TNT_FUSE_TICKS = 200;

    private final SpleefPlugin plugin;
    private final ArenaManager manager;
    private final WorldGuardSupport worldGuard;
    private final NamespacedKey visualTntKey;
    private final Map<String, FloorReset> pendingResets = new LinkedHashMap<>();
    private BukkitTask resetTask;

    ArenaWorld(SpleefPlugin plugin, ArenaManager manager, WorldGuardSupport worldGuard) {

        this.plugin = plugin;
        this.manager = manager;
        this.worldGuard = worldGuard;
        this.visualTntKey = new NamespacedKey(plugin, "visual-tnt");

    }

    void start() {

        this.resetTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::drainResets, 1L, 1L);

    }

    void shutdown() {

        if (this.resetTask != null) {

            this.resetTask.cancel();

        }

    }

    void forget(String arenaName) {

        this.pendingResets.remove(ArenaManager.normalize(arenaName));

    }

    void forgetAll() {

        this.pendingResets.clear();

    }

    // Restores an arena floor. Normal-sized arenas finish within one tick's budget;
    // anything larger continues over
    // the following ticks rather than freezing the server mid-match.
    void resetLayers(SpleefArena arena) {

        World world = Bukkit.getWorld(arena.worldName());
        ProtectedRegion region = world == null ? null : this.worldGuard.region(world, arena.regionId());
        if (region == null) {

            return;

        }

        String key = ArenaManager.normalize(arena.name());
        this.pendingResets.remove(key);
        FloorReset reset = new FloorReset(world, region, arena);
        int budget = Math.max(1, this.manager.config().resetBlocksPerTick());
        if (reset.run(budget)) {

            return;

        }

        if (!this.plugin.isEnabled()) {

            // Nothing will tick after this, so finish the floor now rather than leaving it
            // dug out until next boot.
            while (!reset.run(budget)) {

                continue;

            }

            return;

        }

        this.pendingResets.put(key, reset);

    }

    private void drainResets() {

        Iterator<Map.Entry<String, FloorReset>> pending = this.pendingResets.entrySet().iterator();
        if (!pending.hasNext()) {

            return;

        }

        // One arena per tick, so two arenas resetting at once cannot double the
        // per-tick cost.
        if (pending.next().getValue().run(this.manager.config().resetBlocksPerTick())) {

            pending.remove();

        }

    }

    private ArenaSession activeSessionAt(Location location) {

        return this.manager.allSessions().stream().filter(ArenaSession::isActive)
                .filter(session -> this.manager.isInsideArena(session.arena(), location)).findFirst().orElse(null);

    }

    private SpleefLayer layerAt(SpleefArena arena, Location location) {

        if (!this.manager.isInsideArena(arena, location)) {

            return null;

        }

        return arena.layers().stream().filter(layer -> layer.bounds().contains(location)).findFirst().orElse(null);

    }

    // Runs at HIGHEST and un-cancels, so a region that denies building still lets a
    // match dig its own floor.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        ArenaSession session = this.manager.session(player);
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

        if (this.manager.session(event.getPlayer()) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {

        Block block = event.getHitBlock();
        Projectile projectile = event.getEntity();
        ProjectileSource source = projectile.getShooter();
        if (block == null || !(source instanceof Player player)) {

            return;

        }

        ArenaSession session = this.manager.session(player);
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

    // Arrows that leave the arena would otherwise carry the match outside it,
    // priming TNT and igniting whatever
    // they land on with no session to attribute them to.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLeaveArena(ProjectileHitEvent event) {

        if (!(event.getEntity().getShooter() instanceof Player shooter)) {

            return;

        }

        ArenaSession session = this.manager.session(shooter);
        Location impact = event.getHitBlock() == null ? event.getEntity().getLocation()
                : event.getHitBlock().getLocation();
        if (session != null && !this.manager.isInsideArena(session.arena(), impact)) {

            event.getEntity().remove();

        }

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
                TNTPrimed.class, tnt ->
                {

                    tnt.setFuseTicks(VISUAL_TNT_FUSE_TICKS);
                    tnt.getPersistentDataContainer().set(this.visualTntKey, PersistentDataType.BYTE, (byte) 1);

                });
        Bukkit.getScheduler().runTaskLater(this.plugin, visual::remove, 20L);

    }

    // The decorative TNT is a real primed entity, so it is tagged and refused
    // permission to explode. Without this,
    // one orphaned by a restart inside its one-second lifetime detonates for real
    // on the next chunk load.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {

        if (event.getEntity().getPersistentDataContainer().has(this.visualTntKey, PersistentDataType.BYTE)) {

            event.setCancelled(true);
            event.getEntity().remove();

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {

        this.protectLayers(event.blockList());

    }

    // Beds and respawn anchors explode through a different event, and would
    // otherwise blow holes in a floor.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {

        this.protectLayers(event.blockList());

    }

    private void protectLayers(List<Block> blocks) {

        if (blocks.isEmpty()) {

            return;

        }

        // Resolved once per explosion rather than once per block. Arenas sit in the
        // live SMP world, so this runs
        // against every creeper, TNT and bed blast on the server and the per-block cost
        // is what matters.
        List<ArenaSession> active = this.manager.allSessions().stream().filter(ArenaSession::isActive)
                .filter(session -> session.arena().worldName().equalsIgnoreCase(blocks.get(0).getWorld().getName()))
                .toList();
        if (active.isEmpty()) {

            return;

        }

        blocks.removeIf(block -> active.stream()
                .anyMatch(session -> this.layerAt(session.arena(), block.getLocation()) != null));

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

}
