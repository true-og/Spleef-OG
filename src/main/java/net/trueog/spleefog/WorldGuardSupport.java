package net.trueog.spleefog;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Comparator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.trueog.spleefog.model.BlockBounds;

public final class WorldGuardSupport {

    public RegionBinding findArenaRegion(Player player) {

        Location location = player.getLocation();
        RegionManager manager = this.manager(location.getWorld());
        if (manager == null || SpleefFlags.allowSpleef() == null) {

            return null;

        }

        BlockVector3 point = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ApplicableRegionSet applicable = manager.getApplicableRegions(point);
        return applicable.getRegions().stream()
                .filter(region -> !region.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION))
                .filter(region -> region.getFlag(SpleefFlags.allowSpleef()) == StateFlag.State.ALLOW).map(this::binding)
                .min(Comparator.comparingLong(binding -> binding.bounds().volume())).orElse(null);

    }

    public boolean regionExists(World world, String regionId) {

        return this.region(world, regionId) != null;

    }

    // Resolves the region once so bulk work can test containment without a manager
    // lookup per block.
    public ProtectedRegion region(World world, String regionId) {

        RegionManager manager = this.manager(world);
        return manager == null || regionId == null ? null : manager.getRegion(regionId);

    }

    public boolean isInside(Location location, String regionId) {

        if (location == null || location.getWorld() == null) {

            return false;

        }

        RegionManager manager = this.manager(location.getWorld());
        ProtectedRegion region = manager == null ? null : manager.getRegion(regionId);
        return region != null && region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    }

    public BlockBounds regionBounds(World world, String regionId) {

        RegionManager manager = this.manager(world);
        ProtectedRegion region = manager == null ? null : manager.getRegion(regionId);
        return region == null ? null : this.binding(region).bounds();

    }

    // True when the region's own flags let anyone build, meaning it is not
    // providing the protection Spleef assumes.
    public boolean allowsNonMemberBuilding(World world, String regionId) {

        ProtectedRegion region = this.region(world, regionId);
        return region != null && (region.getFlag(Flags.BUILD) == StateFlag.State.ALLOW
                || region.getFlag(Flags.PASSTHROUGH) == StateFlag.State.ALLOW);

    }

    private RegionManager manager(World world) {

        return world == null ? null
                : WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));

    }

    private RegionBinding binding(ProtectedRegion region) {

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new RegionBinding(region.getId(),
                new BlockBounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));

    }

    public record RegionBinding(String id, BlockBounds bounds) {
    }

}
