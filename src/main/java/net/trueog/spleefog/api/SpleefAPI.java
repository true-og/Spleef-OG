package net.trueog.spleefog.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class SpleefAPI {

    private static final Set<UUID> ACTIVE_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private static final Set<UUID> PENDING_RECOVERY = Collections.synchronizedSet(new HashSet<>());

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

    // True while Spleef still holds a saved copy of this player's pre-match state.
    //
    // Unlike isInSpleef, this survives a restart or a crash, because it is backed
    // by recovery.yml. It lets another
    // plugin tell "this player was left in an odd gamemode by a minigame" apart
    // from "this player was left in an odd
    // gamemode on the SMP", which matters on a login where Spleef's in-memory
    // session is long gone.
    public static boolean hasPendingRecovery(UUID playerId) {

        return playerId != null && PENDING_RECOVERY.contains(playerId);

    }

    public static void markRecoveryPending(UUID playerId) {

        if (playerId != null) {

            PENDING_RECOVERY.add(playerId);

        }

    }

    public static void clearRecoveryPending(UUID playerId) {

        if (playerId != null) {

            PENDING_RECOVERY.remove(playerId);

        }

    }

}
