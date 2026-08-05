package net.trueog.spleefog.arena;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import net.kyori.adventure.text.Component;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.model.BlockBounds;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.spigotmc.event.entity.EntityMountEvent;

// Keeps a player who is in a session inside their arena.
//
// This class exists because containment kept springing leaks in the same way: the decision was right and an event
// class had simply never been registered. Portals and vehicles were both missed that way. So every route by which a
// player's position can change is listed here, in one place. If a new route appears, it belongs in this file.
//
// decide() is the single rule, and every route asks it. What differs is only the mechanism each route has for
// acting on the answer: a movement can be redirected, a teleport can only be cancelled, a mount has to be left
// first. Adding a route means adding a handler that asks decide() and maps the Outcome onto whatever that event
// can actually do.
//
// Routes covered:
//   PlayerMoveEvent                     walking, being pushed by pistons or water
//   PlayerTeleportEvent                 commands, plugins, ender pearls, chorus fruit
//   PlayerPortalEvent                   nether and end portals; it declares its own handler list, so a
//                                       PlayerTeleportEvent listener never sees it
//   EntityPortalEvent                   a vehicle carrying the player through a portal
//   VehicleEnterEvent, EntityMountEvent mounting anything; passenger movement is not a PlayerMoveEvent
//   VehicleMoveEvent                    a mount that was boarded before the session started
//   PlayerStartSpectatingEntityEvent    spectator camera lock, which moves the view with no movement packet
//
// Deliberately not here: the plugin placing a player somewhere on purpose. Entering an arena, starting a match,
// showing the ending and choosing a respawn point are session lifecycle, and live in ArenaManager.
public final class Confinement implements Listener {

    // What to do about a player who is somewhere they should not be.
    private enum Outcome {

        // Where they are is fine.
        ALLOW,
        // Not in play, so put them back rather than kill them.
        RETURN,
        // In play and out of bounds or in the death region: that is how a spleef round
        // ends.
        ELIMINATE

    }

    private final ArenaManager manager;

    Confinement(ArenaManager manager) {

        this.manager = manager;

    }

    // The single containment rule. Session must not be null; callers check that
    // first.
    private Outcome decide(ArenaSession session, Player player, Location to) {

        boolean inside = this.manager.isInsideArena(session.arena(), to);
        if (!session.isAlive(player.getUniqueId())) {

            // Waiting players, spectators and the eliminated are invulnerable and
            // hunger-immune, so letting them
            // wander the world would hand out free invincibility and, for spectators, free
            // noclip. They are not in
            // play, so they are put back rather than killed.
            return inside ? Outcome.ALLOW : Outcome.RETURN;

        }

        BlockBounds deathRegion = session.arena().deathRegion();
        if (inside && (deathRegion == null || !deathRegion.contains(to))) {

            return Outcome.ALLOW;

        }

        return Outcome.ELIMINATE;

    }

    // Where a player who has strayed belongs.
    private Location returnPoint(ArenaSession session, Player player) {

        return session.hasSpectator(player.getUniqueId()) ? session.arena().spectatorSpawn()
                : session.arena().waitingSpawn();

    }

    private void eliminate(Player player) {

        this.manager.markForcedDeath(player.getUniqueId());
        player.setHealth(0.0D);

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event) {

        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {

            return;

        }

        Player player = event.getPlayer();
        ArenaSession session = this.manager.session(player);
        if (session == null) {

            return;

        }

        switch (this.decide(session, player, event.getTo())) {

            case ALLOW -> {

            }
            case ELIMINATE -> this.eliminate(player);
            case RETURN -> {

                Location back = this.returnPoint(session, player);
                if (back != null && back.getWorld() != null) {

                    event.setTo(back);

                }

            }

        }

    }

    // Anything landing inside the arena region is allowed, which covers the
    // plugin's own teleports without needing
    // to tag them: by the time a player is restored to their pre-Spleef location
    // they are no longer in a session.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();
        ArenaSession session = this.manager.session(player);
        if (!this.guardsTeleports(player, session) || this.decide(session, player, event.getTo()) == Outcome.ALLOW) {

            return;

        }

        event.setCancelled(true);
        Messages.send(player, Messages.body().append(Component.text("You cannot teleport out of Spleef. Use "))
                .append(Messages.value("/spleef leave")).append(Component.text(" first.")).build());

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {

        Player player = event.getPlayer();
        ArenaSession session = this.manager.session(player);
        if (this.guardsTeleports(player, session) && this.decide(session, player, event.getTo()) != Outcome.ALLOW) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {

        for (Entity passenger : event.getEntity().getPassengers()) {

            if (!(passenger instanceof Player player)) {

                continue;

            }

            ArenaSession session = this.manager.session(player);
            if (this.guardsTeleports(player, session) && this.decide(session, player, event.getTo()) != Outcome.ALLOW) {

                event.setCancelled(true);
                return;

            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {

        if (event.getEntered() instanceof Player player && this.manager.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {

        if (event.getEntity() instanceof Player player && this.manager.session(player) != null) {

            event.setCancelled(true);

        }

    }

    // Covers a mount boarded before the session began. Mounting is refused above,
    // so this is the second line.
    // VehicleMoveEvent is not cancellable, so the correction is to take the player
    // off the mount and then apply
    // whatever decide() asked for.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleMove(VehicleMoveEvent event) {

        for (Entity passenger : event.getVehicle().getPassengers()) {

            if (!(passenger instanceof Player player)) {

                continue;

            }

            ArenaSession session = this.manager.session(player);
            if (session == null) {

                continue;

            }

            Outcome outcome = this.decide(session, player, event.getTo());
            if (outcome == Outcome.ALLOW) {

                continue;

            }

            player.leaveVehicle();
            if (outcome == Outcome.ELIMINATE) {

                this.eliminate(player);
                continue;

            }

            Location back = this.returnPoint(session, player);
            if (back != null && back.getWorld() != null) {

                player.teleport(back);

            }

        }

    }

    // Locking the camera onto a player carries the view out of the arena with no
    // movement packet at all.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStartSpectating(PlayerStartSpectatingEntityEvent event) {

        ArenaSession session = this.manager.session(event.getPlayer());
        if (session == null) {

            return;

        }

        // Anything else can walk out of the arena and take the camera with it, and a
        // locked-on spectator sends no
        // movement packets, so nothing would ever correct it.
        Entity target = event.getNewSpectatorTarget();
        if (!(target instanceof Player watched) || this.manager.session(watched) != session) {

            event.setCancelled(true);

        }

    }

    private boolean guardsTeleports(Player player, ArenaSession session) {

        return session != null && this.manager.config().blockTeleports()
                && !player.hasPermission(ArenaManager.BYPASS_TELEPORT);

    }

    private static boolean sameBlock(Location first, Location second) {

        return first.getBlockX() == second.getBlockX() && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();

    }

}
