package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Repoints Essentials' /back away from the arena.
//
// Essentials records a back location on every teleport, so the teleport that puts a player back
// where they were before Spleef leaves /back aimed at the arena they just left. Confinement refuses
// that teleport, which is correct but reads as a broken /back. Overwriting the recorded location
// with the pre-Spleef spot instead makes /back land where the player expects.
//
// Essentials is not a compile dependency, so the two calls are made reflectively and the hook
// disables itself whenever the plugin, the class or the methods are not what this expects. The
// plugin instance is looked up on every call, and the methods are re-resolved whenever it changes,
// so a reloaded Essentials is picked up instead of being called through a dead instance.
public final class EssentialsHook {

    private static final String[] PLUGIN_NAMES = { "Essentials-OG", "Essentials" };

    private final Logger logger;
    private Plugin cachedPlugin;
    private Method getUser;
    private Method setLastLocation;
    private boolean warned;

    public EssentialsHook(Logger logger) {

        this.logger = logger;

    }

    public boolean isEnabled() {

        return this.findPlugin() != null;

    }

    public void setBackLocation(Player player, Location location) {

        Plugin essentials = this.findPlugin();
        if (essentials == null || location == null || !location.isWorldLoaded()) {

            return;

        }

        try {

            this.resolve(essentials);
            Object user = this.getUser.invoke(essentials, player);
            if (user != null) {

                this.setLastLocation.invoke(user, location);

            }

        } catch (ReflectiveOperationException | RuntimeException ex) {

            // Essentials changed shape or refused the call; /back simply keeps whatever it
            // had, which Confinement still refuses. Never let this break a restore, but say
            // so once so the missing behaviour is not a mystery.
            if (!this.warned) {

                this.warned = true;
                this.logger.log(Level.WARNING,
                        "Could not repoint Essentials /back after Spleef; /back may aim at the arena.", ex);

            }

        }

    }

    private Plugin findPlugin() {

        for (String name : PLUGIN_NAMES) {

            Plugin candidate = Bukkit.getPluginManager().getPlugin(name);
            if (candidate != null && candidate.isEnabled()) {

                return candidate;

            }

        }

        return null;

    }

    private void resolve(Plugin plugin) throws ReflectiveOperationException {

        if (plugin == this.cachedPlugin) {

            return;

        }

        Method userMethod = plugin.getClass().getMethod("getUser", Player.class);
        this.setLastLocation = userMethod.getReturnType().getMethod("setLastLocation", Location.class);
        this.getUser = userMethod;
        this.cachedPlugin = plugin;
        this.warned = false;

    }

}
