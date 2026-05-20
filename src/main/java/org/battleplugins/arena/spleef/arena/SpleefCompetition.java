package org.battleplugins.arena.spleef.arena;

import org.battleplugins.arena.competition.CompetitionType;
import org.battleplugins.arena.competition.JoinResult;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.PlayerRole;
import org.battleplugins.arena.competition.map.options.Bounds;
import org.battleplugins.arena.spleef.ArenaSpleef;
import org.battleplugins.arena.spleef.SpleefConfig;
import org.battleplugins.arena.spleef.SpleefMessages;
import org.battleplugins.arena.spleef.WorldGuardSupport;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpleefCompetition extends LiveCompetition<SpleefCompetition> {

    private static final JoinResult REGION_NOT_JOINABLE = new JoinResult(false,
            SpleefMessages.MATCH_REGION_NOT_WHITELISTED);

    private final SpleefMap map;

    public SpleefCompetition(SpleefArena arena, CompetitionType type, SpleefMap map) {

        super(arena, type, map);

        this.map = map;

    }

    @Override
    public CompletableFuture<JoinResult> canJoin(Collection<Player> players, PlayerRole role) {

        if (!this.isMatchRegionAllowed()) {

            return CompletableFuture.completedFuture(REGION_NOT_JOINABLE);

        }

        return super.canJoin(players, role);

    }

    public boolean isMatchRegionAllowed() {

        SpleefConfig config = ArenaSpleef.getInstance().getMainConfig();
        if (config == null || !config.hasRegionWhitelist()) {

            return true;

        }

        World world = this.map.getWorld();
        if (world == null) {

            return false;

        }

        String worldGuardRegion = this.map.getWorldGuardRegion();
        if (config.isRegionAllowed(worldGuardRegion) && WorldGuardSupport.regionExists(world, worldGuardRegion)) {

            return true;

        }

        List<SpleefMap.Layer> layers = this.map.getLayers();
        if (!layers.isEmpty()) {

            return layers.stream().allMatch(layer -> this.isBoundsInAllowedRegion(layer.getBounds()));

        }

        return this.map.bounds().map(this::isBoundsInAllowedRegion).orElse(false);

    }

    public boolean isLocationAllowed(Location location) {

        SpleefConfig config = ArenaSpleef.getInstance().getMainConfig();
        return config == null || config.isRegionAllowedAt(location);

    }

    public void pasteLayers() {

        List<SpleefMap.Layer> layers = this.map.getLayers();
        for (SpleefMap.Layer layer : layers) {

            for (int x = layer.getBounds().getMinX(); x <= layer.getBounds().getMaxX(); x++) {

                for (int y = layer.getBounds().getMinY(); y <= layer.getBounds().getMaxY(); y++) {

                    for (int z = layer.getBounds().getMinZ(); z <= layer.getBounds().getMaxZ(); z++) {

                        Block block = this.map.getWorld().getBlockAt(x, y, z);
                        if (!this.isLocationAllowed(block.getLocation())) {

                            continue;

                        }

                        block.setBlockData(layer.getBlockData());

                    }

                }

            }

        }

    }

    private boolean isBoundsInAllowedRegion(Bounds bounds) {

        World world = this.map.getWorld();
        if (world == null) {

            return false;

        }

        return this.isLocationAllowed(new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMinZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMaxZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMinX(), bounds.getMaxY(), bounds.getMinZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMinX(), bounds.getMaxY(), bounds.getMaxZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMaxX(), bounds.getMinY(), bounds.getMinZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMaxX(), bounds.getMinY(), bounds.getMaxZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMinZ()))
                && this.isLocationAllowed(new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ()));

    }

}
