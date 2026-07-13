package net.trueog.spleefog.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class SpleefAPI {

    private static final Set<UUID> ACTIVE_PLAYERS = Collections.synchronizedSet(new HashSet<>());

    private SpleefAPI() {

    }

    public static boolean isInSpleef(Player player) {

        return player != null && ACTIVE_PLAYERS.contains(player.getUniqueId());

    }

    public static boolean markJoined(Player player) {

        return player != null && ACTIVE_PLAYERS.add(player.getUniqueId());

    }

    public static boolean markLeft(Player player) {

        return player != null && ACTIVE_PLAYERS.remove(player.getUniqueId());

    }

    public static void clear() {

        ACTIVE_PLAYERS.clear();

    }

}
