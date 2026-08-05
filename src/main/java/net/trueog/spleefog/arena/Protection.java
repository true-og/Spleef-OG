package net.trueog.spleefog.arena;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.model.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

// Rules that apply to a player because Spleef currently owns them.
//
// Two directions, and both matter. Inward: nothing may damage, starve, or take items from someone in a match.
// Outward: because they are invulnerable, they must not be able to hurt anything that is not in the same match, or
// an arena becomes a safe firing position onto the SMP.
//
// The preventive guards run at LOWEST on purpose. At HIGHEST they fire after a NORMAL-priority GUI plugin has
// already acted on the same click, which is too late to prevent anything.
public final class Protection implements Listener {

    private final ArenaManager manager;

    Protection(ArenaManager manager) {

        this.manager = manager;

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player) || this.manager.session(player) == null) {

            return;

        }

        // Kept as a safety net rather than a live path: the plugin ends a round with
        // setHealth(0), which calls
        // die() straight away without raising a damage event, so this exemption is only
        // reached if elimination is
        // ever changed to work through damage.
        if (!this.manager.isForcedDeath(player.getUniqueId())) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {

        if (this.crossesArena(event.getDamager(), event.getEntity())) {

            event.setCancelled(true);

        }

    }

    // An arrow sets its target alight before it deals damage, and the burn then
    // arrives as a damager-less event, so
    // cancelling the impact alone still leaves a session player able to set fire to
    // anything outside the arena.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {

        if (this.crossesArena(event.getCombuster(), event.getEntity())) {

            event.setCancelled(true);

        }

    }

    // True when a player Spleef has made invulnerable is acting on something that
    // is not in the same match.
    private boolean crossesArena(Entity source, Entity target) {

        ArenaSession sourceSession = this.manager.session(resolveAttacker(source));
        if (sourceSession == null) {

            return false;

        }

        return (target instanceof Player victim ? this.manager.session(victim) : null) != sourceSession;

    }

    private static Player resolveAttacker(Entity damager) {

        if (damager instanceof Player player) {

            return player;

        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {

            return shooter;

        }

        return null;

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFood(FoodLevelChangeEvent event) {

        if (event.getEntity() instanceof Player player && this.manager.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        ArenaSession session = this.manager.session(event.getPlayer());
        if (session != null && session.state() != ArenaState.IN_GAME) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {

        if (this.manager.session(event.getPlayer()) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPickup(EntityPickupItemEvent event) {

        if (event.getEntity() instanceof Player player && this.manager.session(player) != null) {

            event.setCancelled(true);

        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player player && this.manager.session(player) != null) {

            event.setCancelled(true);

        }

    }

    // Restores gamemode changes that a world-level policy plugin has vetoed for a
    // player Spleef is managing.
    //
    // GameModeInventories-OG cancels adventure mode in worlds on its
    // restrict_adventure_worlds list, and spectator
    // mode when restrict_spectator is set, both at LOWEST. Those rules exist to
    // stop players parking themselves in
    // those modes on the SMP; inside an arena the switch is the plugin's own doing
    // and has to go through, or waiting
    // players never leave survival and eliminated players never become spectators.
    // Bukkit only honours a
    // cancellation that survives every handler, so clearing it here is what takes
    // effect.
    //
    // Creative is never reinstated. Spleef never sets it, and un-cancelling it
    // would hand anyone with /gmic a
    // bypass of the server's creative-region policy for as long as they stood in an
    // arena.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {

        if (!event.isCancelled() || event.getNewGameMode() == GameMode.CREATIVE) {

            return;

        }

        if (this.manager.ownsGameModeOf(event.getPlayer().getUniqueId())) {

            event.setCancelled(false);

        }

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (this.manager.session(player) == null || player.hasPermission(ArenaManager.BYPASS_COMMANDS)) {

            return;

        }

        String typed = normalizeCommand(event.getMessage());
        if (typed.isEmpty() || !this.isCommandBlocked(typed)) {

            return;

        }

        event.setCancelled(true);
        Messages.send(player, Messages.body().append(Component.text("You cannot use "))
                .append(Messages.value("/" + typed)).append(Component.text(" while in Spleef.")).build());

    }

    // Strips the slash, lower-cases, and collapses runs of whitespace so config
    // entries can match predictably.
    private static String normalizeCommand(String message) {

        return message == null ? ""
                : message.replaceAll("^/+", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

    }

    private boolean isCommandBlocked(String typed) {

        Set<String> candidates = commandCandidates(typed);
        if (candidates.stream().anyMatch(candidate -> candidate.equals("spleef") || candidate.startsWith("spleef "))) {

            // Never trap a player inside a match; /spleef leave has to stay reachable.
            return false;

        }

        if (this.manager.config().blockAllCommands()) {

            Set<String> whitelist = this.manager.config().whitelistedCommands();
            return candidates.stream().noneMatch(candidate -> matchesAny(whitelist, candidate));

        }

        Set<String> blacklist = this.manager.config().blacklistedCommands();
        return candidates.stream().anyMatch(candidate -> matchesAny(blacklist, candidate));

    }

    // Expands what the player typed into every spelling that means the same
    // command, so an entry of home also
    // covers /essentials:home and any registered alias.
    private static Set<String> commandCandidates(String typed) {

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(typed);
        int split = typed.indexOf(' ');
        String label = split < 0 ? typed : typed.substring(0, split);
        String remainder = split < 0 ? "" : typed.substring(split);

        int namespace = label.indexOf(':');
        if (namespace >= 0 && namespace + 1 < label.length()) {

            candidates.add(label.substring(namespace + 1) + remainder);

        }

        try {

            Command command = Bukkit.getCommandMap().getCommand(label);
            if (command != null) {

                candidates.add(command.getName().toLowerCase(Locale.ROOT) + remainder);

            }

        } catch (RuntimeException ignored) {

            // No command map available; the literal spellings above still apply.

        }

        return candidates;

    }

    private static boolean matchesAny(Set<String> entries, String candidate) {

        for (String entry : entries) {

            if (candidate.equals(entry) || candidate.startsWith(entry + " ")) {

                return true;

            }

        }

        return false;

    }

}
