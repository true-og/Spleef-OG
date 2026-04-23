package org.battleplugins.arena.spleef.api;

import org.battleplugins.arena.event.player.ArenaJoinEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class SpleefApiListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaJoin(ArenaJoinEvent event) {

        if (!SpleefAPI.isSpleefPlayer(event.getArenaPlayer())) {

            return;

        }

        Player player = event.getPlayer();
        if (SpleefAPI.markJoined(player)) {

            Bukkit.getPluginManager().callEvent(new SpleefJoinEvent(player));

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaLeave(ArenaLeaveEvent event) {

        if (!SpleefAPI.isSpleefPlayer(event.getArenaPlayer())) {

            return;

        }

        Player player = event.getPlayer();
        if (SpleefAPI.markLeft(player)) {

            Bukkit.getPluginManager().callEvent(new SpleefLeaveEvent(player));

        }

    }

}
