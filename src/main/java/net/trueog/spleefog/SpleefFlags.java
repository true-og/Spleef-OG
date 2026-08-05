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
            return;

        } catch (FlagConflictException conflict) {

            adopt(plugin, registry, "another plugin already owns it");

        } catch (IllegalStateException locked) {

            // WorldGuard locks its registry once it enables, so a mid-session reload cannot
            // register.
            adopt(plugin, registry, "the WorldGuard flag registry is locked");

        }

    }

    private static void adopt(SpleefPlugin plugin, FlagRegistry registry, String reason) {

        Flag<?> existing = registry.get(ALLOW_SPLEEF);
        if (existing instanceof StateFlag stateFlag) {

            allowSpleefFlag = stateFlag;
            return;

        }

        allowSpleefFlag = null;
        if (existing == null) {

            plugin.getLogger().severe("Could not register WorldGuard flag '" + ALLOW_SPLEEF + "' because " + reason
                    + ". Restart the server instead of reloading Spleef-OG; arena creation is disabled until then.");
            return;

        }

        plugin.getLogger().severe("WorldGuard flag '" + ALLOW_SPLEEF + "' has an incompatible type.");

    }

    public static StateFlag allowSpleef() {

        return allowSpleefFlag;

    }

}
