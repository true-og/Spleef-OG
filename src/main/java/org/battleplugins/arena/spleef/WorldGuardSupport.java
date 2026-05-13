package org.battleplugins.arena.spleef;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WorldGuardSupport {

    private WorldGuardSupport() {

    }

    public static boolean isEnabled() {

        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");

    }

    public static boolean isInsideRegion(Location location, String regionId) {

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {

            return false;

        }

        BlockVector3 position = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ApplicableRegionSet regions = regionManager.getApplicableRegions(position);
        return regions.getRegions().stream().anyMatch(region -> region.getId().equalsIgnoreCase(regionId));

    }

    public static boolean isSpleefAllowedAt(Player player) {

        StateFlag flag = SpleefFlags.getAllowSpleefFlag();
        if (flag == null) {

            return false;

        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        Location location = player.getLocation();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {

            return false;

        }

        BlockVector3 position = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ApplicableRegionSet regions = regionManager.getApplicableRegions(position);
        return regions.testState(localPlayer, flag);

    }

}
