package org.battleplugins.arena.spleef;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;

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

}
