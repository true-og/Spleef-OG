package org.battleplugins.arena.spleef.api;

import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.spleef.arena.SpleefCompetition;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SpleefAPI {

    private static final Set<UUID> PLAYERS_IN_SPLEEF = Collections.synchronizedSet(new HashSet<>());

    private SpleefAPI() {

    }

    public static boolean isInSpleef(Player player) {

        if (player == null) {

            return false;

        }

        return PLAYERS_IN_SPLEEF.contains(player.getUniqueId());

    }

    static boolean markJoined(Player player) {

        return PLAYERS_IN_SPLEEF.add(player.getUniqueId());

    }

    static boolean markLeft(Player player) {

        return PLAYERS_IN_SPLEEF.remove(player.getUniqueId());

    }

    static boolean isSpleefPlayer(ArenaPlayer arenaPlayer) {

        return arenaPlayer != null && arenaPlayer.getCompetition() instanceof SpleefCompetition;

    }

}
