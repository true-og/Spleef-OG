package net.trueog.spleefog.hook;

import java.lang.reflect.Method;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Keeps combat-tagged SMP players from escaping into a Spleef match. */
public final class BattleTrackerHook {

    private final SpleefPlugin owner;
    private Plugin cachedPlugin;
    private Method getCombatLog;
    private Method isInCombat;
    private boolean warned;

    public BattleTrackerHook(SpleefPlugin owner) {

        this.owner = owner;

    }

    public boolean isInCombat(Player player) {

        Plugin plugin = Bukkit.getPluginManager().getPlugin("BattleTracker");
        if (plugin == null || !plugin.isEnabled()) {

            return false;

        }

        try {

            this.resolve(plugin);
            Object combatLog = this.getCombatLog.invoke(plugin);
            return combatLog != null && Boolean.TRUE.equals(this.isInCombat.invoke(combatLog, player));

        } catch (ReflectiveOperationException ex) {

            if (!this.warned) {

                this.warned = true;
                this.owner.getLogger()
                        .warning("Could not query BattleTracker combat state; Spleef join protection is disabled.");

            }

            return false;

        }

    }

    private void resolve(Plugin plugin) throws ReflectiveOperationException {

        if (plugin == this.cachedPlugin && this.getCombatLog != null && this.isInCombat != null) {

            return;

        }

        this.getCombatLog = plugin.getClass().getMethod("getCombatLog");
        Object combatLog = this.getCombatLog.invoke(plugin);
        if (combatLog == null) {

            throw new NoSuchMethodException("BattleTracker combat log is unavailable");

        }

        this.isInCombat = combatLog.getClass().getMethod("isInCombat", Player.class);
        this.cachedPlugin = plugin;
        this.warned = false;

    }

}
