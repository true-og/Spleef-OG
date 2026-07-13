package net.trueog.spleefog.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class SpleefJoinEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String arenaName;
    private final boolean spectator;

    public SpleefJoinEvent(Player player, String arenaName, boolean spectator) {

        super(player);
        this.arenaName = arenaName;
        this.spectator = spectator;

    }

    public String getArenaName() {

        return this.arenaName;

    }

    public boolean isSpectator() {

        return this.spectator;

    }

    @Override
    public @NotNull HandlerList getHandlers() {

        return HANDLERS;

    }

    public static HandlerList getHandlerList() {

        return HANDLERS;

    }

}
