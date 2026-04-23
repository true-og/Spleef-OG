package org.battleplugins.arena.spleef.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SpleefLeaveEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public SpleefLeaveEvent(Player player) {

        super(player);

    }

    @NotNull
    @Override
    public HandlerList getHandlers() {

        return HANDLERS;

    }

    public static HandlerList getHandlerList() {

        return HANDLERS;

    }

}
