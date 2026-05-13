package org.battleplugins.arena.spleef;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.plugin.Plugin;

public final class SpleefFlags {

    public static final String ALLOW_SPLEEF_FLAG_NAME = "allow-spleef";

    private static StateFlag allowSpleefFlag;

    private SpleefFlags() {

    }

    public static void register(Plugin plugin) {

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {

            StateFlag flag = new StateFlag(ALLOW_SPLEEF_FLAG_NAME, false);
            registry.register(flag);
            allowSpleefFlag = flag;

        } catch (FlagConflictException conflict) {

            Flag<?> existing = registry.get(ALLOW_SPLEEF_FLAG_NAME);
            if (existing instanceof StateFlag stateFlag) {

                allowSpleefFlag = stateFlag;
                plugin.getSLF4JLogger().info("Reusing existing WorldGuard flag '{}'.", ALLOW_SPLEEF_FLAG_NAME);

            } else {

                plugin.getSLF4JLogger().error(
                        "WorldGuard flag '{}' is already registered with an incompatible type; spleef will not enforce it.",
                        ALLOW_SPLEEF_FLAG_NAME);

            }

        } catch (IllegalStateException late) {

            plugin.getSLF4JLogger().error(
                    "Failed to register WorldGuard flag '{}' — registry already locked. Ensure Spleef-OG loads before WorldGuard enables.",
                    ALLOW_SPLEEF_FLAG_NAME, late);

        }

    }

    public static StateFlag getAllowSpleefFlag() {

        return allowSpleefFlag;

    }

}
