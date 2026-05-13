package org.battleplugins.arena.spleef.hook;

import org.battleplugins.arena.event.player.ArenaJoinEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;
import org.battleplugins.arena.event.player.ArenaSpectateEvent;
import org.battleplugins.arena.spleef.api.SpleefAPI;
import org.battleplugins.arena.spleef.arena.SpleefCompetition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GameModeInventoriesListener implements Listener {

    public static final String PLUGIN_NAME = "GameModeInventories-OG";

    private static final String USE_PERMISSION = "gamemodeinventories.use";

    private final Plugin owner;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    private GameModeInventoriesListener(Plugin owner) {

        this.owner = owner;

    }

    public static GameModeInventoriesListener register(Plugin owner) {

        final Plugin gameModeInventories = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (gameModeInventories == null || !gameModeInventories.isEnabled()) {

            return null;

        }

        final GameModeInventoriesListener listener = new GameModeInventoriesListener(owner);
        Bukkit.getPluginManager().registerEvents(listener, owner);
        return listener;

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArenaJoin(ArenaJoinEvent event) {

        if (event.getArenaPlayer().getCompetition() instanceof SpleefCompetition) {

            this.suspend(event.getPlayer());

        }

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArenaSpectate(ArenaSpectateEvent event) {

        if (event.getArenaPlayer().getCompetition() instanceof SpleefCompetition) {

            this.suspend(event.getPlayer());

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaLeave(ArenaLeaveEvent event) {

        if (!(event.getArenaPlayer().getCompetition() instanceof SpleefCompetition)) {

            return;

        }

        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this.owner, () -> {

            if (!SpleefAPI.isInSpleef(player)) {

                this.release(player);

            }

        });

    }

    public void releaseAll() {

        for (PermissionAttachment attachment : this.attachments.values()) {

            try {

                attachment.remove();

            } catch (IllegalArgumentException ignored) {

            }

        }

        this.attachments.clear();

    }

    private void suspend(Player player) {

        // Keep GMI from saving arena-cleared contents over real inventories.
        this.attachments.computeIfAbsent(player.getUniqueId(),
                key -> player.addAttachment(this.owner, USE_PERMISSION, false));

    }

    private void release(Player player) {

        final PermissionAttachment attachment = this.attachments.remove(player.getUniqueId());
        if (attachment == null) {

            return;

        }

        try {

            attachment.remove();

        } catch (IllegalArgumentException ignored) {

        }

    }

}
