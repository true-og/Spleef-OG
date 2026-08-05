package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
import java.util.logging.Level;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Answers whether a player is combat tagged, so a match cannot be used to escape a fight.
//
// Two providers are consulted because a server may run either. EternalCombat-OG is asked first: where both are
// installed it is normally the authoritative one, and BattleTracker's combat-log feature is often switched off
// precisely so the two do not fight. Asking only BattleTracker in that setup silently answers "not in combat" for
// everyone, which turns joining an arena into a free escape with no item loss.
public final class CombatHook {

    private static final String ETERNAL_COMBAT = "EternalCombat-OG";
    private static final String BATTLE_TRACKER = "BattleTracker";

    private final SpleefPlugin owner;
    private Provider eternalCombat;
    private Provider battleTracker;
    private boolean warned;
    private boolean reportedNoProvider;

    public CombatHook(SpleefPlugin owner) {

        this.owner = owner;

    }

    public boolean isInCombat(Player player) {

        Boolean eternal = this.query(ETERNAL_COMBAT, player);
        if (eternal != null) {

            return eternal;

        }

        Boolean tracker = this.query(BATTLE_TRACKER, player);
        if (tracker != null) {

            return tracker;

        }

        if (!this.reportedNoProvider) {

            this.reportedNoProvider = true;
            this.owner.getLogger().warning("No combat-tag plugin answered, so Spleef cannot stop a tagged player "
                    + "joining a match. Install EternalCombat-OG, or enable BattleTracker's combat-log feature.");

        }

        return false;

    }

    // Returns null when this provider cannot answer at all, so the caller can fall
    // through to the next one.
    private Boolean query(String pluginName, Player player) {

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) {

            return null;

        }

        try {

            Provider provider = ETERNAL_COMBAT.equals(pluginName) ? this.eternalCombat(plugin)
                    : this.battleTracker(plugin);
            return provider == null ? null : provider.isInCombat(player);

        } catch (ReflectiveOperationException | RuntimeException ex) {

            if (!this.warned) {

                this.warned = true;
                this.owner.getLogger().log(Level.WARNING,
                        "Could not read combat state from " + pluginName + "; trying the next provider.", ex);

            }

            return null;

        }

    }

    private Provider eternalCombat(Plugin plugin) throws ReflectiveOperationException {

        if (this.eternalCombat == null) {

            Method accessor = plugin.getClass().getMethod("getFightManager");
            Object manager = accessor.invoke(plugin);
            if (manager == null) {

                return null;

            }

            Method query = manager.getClass().getMethod("isInCombat", java.util.UUID.class);
            this.eternalCombat = player -> Boolean.TRUE.equals(query.invoke(manager, player.getUniqueId()));

        }

        return this.eternalCombat;

    }

    private Provider battleTracker(Plugin plugin) throws ReflectiveOperationException {

        if (this.battleTracker == null) {

            Method accessor = plugin.getClass().getMethod("getCombatLog");
            Object combatLog = accessor.invoke(plugin);
            if (combatLog == null) {

                return null;

            }

            Method query = combatLog.getClass().getMethod("isInCombat", Player.class);
            this.battleTracker = player -> Boolean.TRUE.equals(query.invoke(combatLog, player));

        }

        return this.battleTracker;

    }

    @FunctionalInterface
    private interface Provider {

        boolean isInCombat(Player player) throws ReflectiveOperationException;

    }

}
