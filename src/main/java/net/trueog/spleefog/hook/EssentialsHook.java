package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
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
// disables itself whenever the plugin, the class or the methods are not what this expects.
public final class EssentialsHook {

    private static final String[] PLUGIN_NAMES = { "Essentials-OG", "Essentials" };

    private final Plugin essentials;
    private final Method getUser;
    private final Method setLastLocation;

    public EssentialsHook() {

        Plugin found = null;
        for (String name : PLUGIN_NAMES) {

            Plugin candidate = Bukkit.getPluginManager().getPlugin(name);
            if (candidate != null && candidate.isEnabled()) {

                found = candidate;
                break;

            }

        }

        Method userMethod = null;
        Method locationMethod = null;
        if (found != null) {

            try {

                userMethod = found.getClass().getMethod("getUser", Player.class);
                locationMethod = userMethod.getReturnType().getMethod("setLastLocation", Location.class);

            } catch (ReflectiveOperationException | RuntimeException ignored) {

                userMethod = null;
                locationMethod = null;

            }

        }

        this.essentials = userMethod == null ? null : found;
        this.getUser = userMethod;
        this.setLastLocation = locationMethod;

    }

    public boolean isEnabled() {

        return this.essentials != null;

    }

    public void setBackLocation(Player player, Location location) {

        if (this.essentials == null || location == null || location.getWorld() == null) {

            return;

        }

        try {

            Object user = this.getUser.invoke(this.essentials, player);
            if (user != null) {

                this.setLastLocation.invoke(user, location);

            }

        } catch (ReflectiveOperationException | RuntimeException ignored) {

            // Essentials changed shape or refused the call; /back simply keeps whatever it
            // had, which Confinement still refuses. Never let this break a restore.

        }

    }

}
