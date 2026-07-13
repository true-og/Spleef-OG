package net.trueog.spleefog;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

public final class SpleefFlags {

    public static final String ALLOW_SPLEEF = "allow-spleef";
    private static StateFlag allowSpleefFlag;

    private SpleefFlags() {

    }

    public static void register(SpleefPlugin plugin) {

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {

            StateFlag flag = new StateFlag(ALLOW_SPLEEF, false);
            registry.register(flag);
            allowSpleefFlag = flag;

        } catch (FlagConflictException conflict) {

            Flag<?> existing = registry.get(ALLOW_SPLEEF);
            if (existing instanceof StateFlag stateFlag) {

                allowSpleefFlag = stateFlag;

            } else {

                plugin.getLogger().severe("WorldGuard flag '" + ALLOW_SPLEEF + "' has an incompatible type.");

            }

        }

    }

    public static StateFlag allowSpleef() {

        return allowSpleefFlag;

    }

}
