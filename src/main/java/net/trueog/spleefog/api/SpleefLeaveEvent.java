package net.trueog.spleefog.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class SpleefLeaveEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String arenaName;

    public SpleefLeaveEvent(Player player, String arenaName) {

        super(player);
        this.arenaName = arenaName;

    }

    public String getArenaName() {

        return this.arenaName;

    }

    @Override
    public @NotNull HandlerList getHandlers() {

        return HANDLERS;

    }

    public static HandlerList getHandlerList() {

        return HANDLERS;

    }

}
