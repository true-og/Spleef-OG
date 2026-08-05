package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
import java.util.logging.Level;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Hands the sidebar back and forth with Scoreboard-OG.
//
// Scoreboard-OG injects its sidebar with packets and asserts the display slot exactly once, so Spleef taking the
// slot with setScoreboard is never undone by it. The problem is the other direction: when a player is handed back
// to the main scoreboard, the server sends a remove for the Spleef objective and puts nothing in its place, and
// Scoreboard-OG has no re-assert loop to notice. Its sidebar would stay blank until the player relogged. Asking it
// to reopen closes that hole; the call is also what stops its per-tick placeholder lookups running for players
// sitting in an arena.
public final class ScoreboardOGHook {

    private static final String PLUGIN_NAME = "Scoreboard-OG";
    private final SpleefPlugin owner;
    private Plugin cachedPlugin;
    private Method openBoard;
    private Method closeBoard;
    private boolean warned;

    public ScoreboardOGHook(SpleefPlugin owner) {

        this.owner = owner;

    }

    public void close(Player player) {

        this.invoke(player, true);

    }

    // Reopens on the following tick: Scoreboard-OG's updater runs every tick, and
    // reopening in the same tick races
    // the removal packets the scoreboard swap has just queued.
    public void reopenLater(Player player) {

        if (!this.owner.isEnabled()) {

            // Disabling: the scheduler is unavailable, so hand it back now rather than
            // leaving the sidebar closed
            // until the player next logs in.
            this.invoke(player, false);
            return;

        }

        Bukkit.getScheduler().runTask(this.owner, () -> {

            if (player.isOnline()) {

                this.invoke(player, false);

            }

        });

    }

    private void invoke(Player player, boolean close) {

        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {

            return;

        }

        try {

            this.resolve(plugin);
            Method method = close ? this.closeBoard : this.openBoard;
            if (method != null) {

                method.invoke(plugin, player);

            }

        } catch (ReflectiveOperationException | RuntimeException ex) {

            if (!this.warned) {

                this.warned = true;
                this.owner.getLogger().log(Level.WARNING,
                        "Could not hand the sidebar to Scoreboard-OG; its sidebar may stay blank after a match.", ex);

            }

        }

    }

    private void resolve(Plugin plugin) throws ReflectiveOperationException {

        if (plugin == this.cachedPlugin) {

            return;

        }

        this.openBoard = plugin.getClass().getMethod("openBoard", Player.class);
        this.closeBoard = plugin.getClass().getMethod("closeBoard", Player.class);
        this.cachedPlugin = plugin;
        this.warned = false;

    }

}
