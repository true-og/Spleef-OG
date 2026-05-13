package org.battleplugins.arena.spleef.hook;

import java.lang.reflect.Method;
import org.battleplugins.arena.spleef.api.SpleefAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class PlayerBountiesListener implements Listener {

    public static final String PLUGIN_NAME = "PlayerBounties-OG";

    private static final String CLAIM_EVENT_CLASS = "com.tcoded.playerbountiesplus.event.BountyClaimEvent";
    private static final String SET_EVENT_CLASS = "com.tcoded.playerbountiesplus.event.BountySetEvent";

    private PlayerBountiesListener() {

    }

    public static boolean register(Plugin owner) {

        Plugin bounties = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (bounties == null) {

            return false;

        }

        Class<? extends Event> claimEventClass;
        Class<? extends Event> setEventClass;
        Method getClaimant;
        Method getVictim;
        Method getTarget;

        try {

            claimEventClass = Class.forName(CLAIM_EVENT_CLASS).asSubclass(Event.class);
            setEventClass = Class.forName(SET_EVENT_CLASS).asSubclass(Event.class);
            getClaimant = claimEventClass.getMethod("getClaimant");
            getVictim = claimEventClass.getMethod("getVictim");
            getTarget = setEventClass.getMethod("getTarget");

        } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException ex) {

            owner.getSLF4JLogger().warn(
                    "PlayerBounties-OG is present but the expected API is missing; skipping bounty protection.", ex);
            return false;

        }

        if (!Cancellable.class.isAssignableFrom(claimEventClass)
                || !Cancellable.class.isAssignableFrom(setEventClass))
        {

            owner.getSLF4JLogger().warn("PlayerBounties-OG events are not Cancellable; skipping bounty protection.");
            return false;

        }

        PlayerBountiesListener listener = new PlayerBountiesListener();

        Bukkit.getPluginManager().registerEvent(claimEventClass, listener, EventPriority.NORMAL, new EventExecutor() {

            @Override
            public void execute(Listener listener, Event event) throws EventException {

                cancelIfInSpleef(event, getClaimant, getVictim);

            }

        }, owner, true);

        Bukkit.getPluginManager().registerEvent(setEventClass, listener, EventPriority.NORMAL, new EventExecutor() {

            @Override
            public void execute(Listener listener, Event event) throws EventException {

                cancelIfInSpleef(event, getTarget);

            }

        }, owner, true);

        return true;

    }

    private static void cancelIfInSpleef(Event event, Method... playerAccessors) throws EventException {

        for (Method accessor : playerAccessors) {

            Player player;

            try {

                player = (Player) accessor.invoke(event);

            } catch (ReflectiveOperationException | ClassCastException ex) {

                throw new EventException(ex);

            }

            if (SpleefAPI.isInSpleef(player)) {

                ((Cancellable) event).setCancelled(true);
                return;

            }

        }

    }

}
