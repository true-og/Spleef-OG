package net.trueog.spleefog.player;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

// Tracks projectiles a player still has in the world when they enter Spleef.
//
// A thrown trident lives on as an entity until it is picked back up, so a player who throws one and
// immediately joins is teleported away from it and loses it: Loyalty cannot follow them into the
// arena and the entity despawns while the match runs. On entry any trident they still have out is
// taken out of the world and stashed in their PlayerSnapshot, which hands it back with the rest of
// their inventory whenever the snapshot is restored (leave, elimination, quit, or restart recovery).
//
// The trident is moved, never copied: the entity is removed first and the item is only stashed if
// that entity was still alive and still pickup-able, so one already picked up, despawned or returned
// by Loyalty yields nothing. Creative throws consume no item and are not tracked at all.
//
// Ender pearls get the opposite treatment: they carry no item worth returning, and one landing
// mid-match only produces a teleport Confinement has to cancel, so they are simply removed.
public final class ThrownProjectileTracker implements Listener {

    // A projectile older than this is assumed gone; the entry is dropped so the map
    // cannot grow without bound for a player who never joins.
    private static final long EXPIRY_MILLIS = 5L * 60L * 1000L;

    private final Map<UUID, List<Thrown>> thrown = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {

        EntityType type = event.getEntityType();
        if (type != EntityType.TRIDENT && type != EntityType.ENDER_PEARL) {

            return;

        }

        if (!(event.getEntity().getShooter() instanceof Player player)) {

            return;

        }

        // A creative throw keeps the item in the inventory, so returning one would
        // duplicate it.
        if (type == EntityType.TRIDENT && player.getGameMode() == GameMode.CREATIVE) {

            return;

        }

        ItemStack item = null;
        if (type == EntityType.TRIDENT) {

            item = heldTrident(player);
            // Without the thrown item there is nothing to hand back -- never fabricate one.
            if (item == null) {

                return;

            }

            item = item.clone();
            // A throw only ever puts a single trident in the world, whatever the stack
            // held.
            item.setAmount(1);

        }

        List<Thrown> entries = this.thrown.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
        removeExpired(entries);
        entries.add(new Thrown(event.getEntity(), item));

    }

    // A landed pearl or a recovered trident is no longer in flight.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(ProjectileHitEvent event) {

        if (!(event.getEntity().getShooter() instanceof Player player)) {

            return;

        }

        List<Thrown> entries = this.thrown.get(player.getUniqueId());
        if (entries == null) {

            return;

        }

        entries.removeIf(entry -> event.getEntity().equals(entry.entity.get()));
        if (entries.isEmpty()) {

            this.thrown.remove(player.getUniqueId());

        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        this.thrown.remove(event.getPlayer().getUniqueId());

    }

    // Called as the player enters an arena, after their snapshot was captured.
    // Returns true when the snapshot changed and has to be written back to disk.
    public boolean stashOnEntry(Player player, PlayerSnapshot snapshot) {

        List<Thrown> entries = this.thrown.remove(player.getUniqueId());
        if (entries == null || entries.isEmpty()) {

            return false;

        }

        boolean changed = false;
        for (Thrown entry : entries) {

            if (!(entry.entity.get() instanceof org.bukkit.entity.Projectile projectile) || !projectile.isValid()) {

                continue;

            }

            if (projectile instanceof EnderPearl) {

                projectile.remove();
                continue;

            }

            if (!(projectile instanceof Trident trident) || entry.item == null) {

                continue;

            }

            // Never the player's to keep: picked up already, or a creative-only throw.
            if (trident.getPickupStatus() != PickupStatus.ALLOWED) {

                continue;

            }

            // Take the entity out of the world BEFORE stashing the item so the trident
            // only ever exists in one place. A removed entity is no longer valid, so a
            // repeat pass cannot hand out a second copy.
            trident.remove();
            snapshot.addReturnedItem(entry.item);
            changed = true;

        }

        return changed;

    }

    public void clear() {

        this.thrown.clear();

    }

    private static ItemStack heldTrident(Player player) {

        PlayerInventory inventory = player.getInventory();
        ItemStack main = inventory.getItemInMainHand();
        if (main.getType() == Material.TRIDENT) {

            return main;

        }

        ItemStack off = inventory.getItemInOffHand();
        return off.getType() == Material.TRIDENT ? off : null;

    }

    private static void removeExpired(List<Thrown> entries) {

        long now = System.currentTimeMillis();
        Iterator<Thrown> iterator = entries.iterator();
        while (iterator.hasNext()) {

            Thrown entry = iterator.next();
            if (now - entry.creation > EXPIRY_MILLIS || entry.entity.get() == null) {

                iterator.remove();

            }

        }

    }

    // Held weakly so a despawned or unloaded projectile cannot keep an entity alive
    // through this map.
    private static final class Thrown {

        private final long creation;
        private final WeakReference<org.bukkit.entity.Entity> entity;
        private final ItemStack item;

        private Thrown(org.bukkit.entity.Entity entity, ItemStack item) {

            this.creation = System.currentTimeMillis();
            this.entity = new WeakReference<>(entity);
            this.item = item;

        }

    }

}
