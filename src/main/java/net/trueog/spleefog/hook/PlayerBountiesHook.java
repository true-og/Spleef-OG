package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.api.SpleefAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class PlayerBountiesHook implements Listener {

    private static final String PLUGIN_NAME = "PlayerBounties-OG";
    private static final String CLAIM_EVENT = "com.tcoded.playerbountiesplus.event.BountyClaimEvent";
    private static final String SET_EVENT = "com.tcoded.playerbountiesplus.event.BountySetEvent";

    private PlayerBountiesHook() {

    }

    public static boolean register(SpleefPlugin owner) {

        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {

            return false;

        }

        try {

            Class<? extends Event> claimClass = Class.forName(CLAIM_EVENT).asSubclass(Event.class);
            Class<? extends Event> setClass = Class.forName(SET_EVENT).asSubclass(Event.class);
            Method claimant = claimClass.getMethod("getClaimant");
            Method victim = claimClass.getMethod("getVictim");
            Method target = setClass.getMethod("getTarget");
            if (!Cancellable.class.isAssignableFrom(claimClass) || !Cancellable.class.isAssignableFrom(setClass)) {

                return false;

            }

            PlayerBountiesHook listener = new PlayerBountiesHook();
            Bukkit.getPluginManager().registerEvent(claimClass, listener, EventPriority.NORMAL,
                    executor(claimant, victim), owner, true);
            Bukkit.getPluginManager().registerEvent(setClass, listener, EventPriority.NORMAL, executor(target), owner,
                    true);
            return true;

        } catch (ReflectiveOperationException | ClassCastException ex) {

            owner.getLogger().warning("PlayerBounties-OG API is incompatible; bounty protection is disabled.");
            return false;

        }

    }

    private static EventExecutor executor(Method... accessors) {

        return (listener, event) -> {

            try {

                for (Method accessor : accessors) {

                    Object value = accessor.invoke(event);
                    if (value instanceof Player player && SpleefAPI.isInSpleef(player)) {

                        ((Cancellable) event).setCancelled(true);
                        return;

                    }

                }

            } catch (ReflectiveOperationException | ClassCastException ex) {

                throw new EventException(ex);

            }

        };

    }

}
