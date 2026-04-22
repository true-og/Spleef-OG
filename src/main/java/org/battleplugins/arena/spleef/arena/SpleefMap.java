package org.battleplugins.arena.spleef.arena;

import io.papermc.paper.math.Position;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.map.LiveCompetitionMap;
import org.battleplugins.arena.competition.map.MapFactory;
import org.battleplugins.arena.competition.map.MapType;
import org.battleplugins.arena.competition.map.options.Bounds;
import org.battleplugins.arena.competition.map.options.Spawns;
import org.battleplugins.arena.config.ArenaOption;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpleefMap extends LiveCompetitionMap {

    static final MapFactory FACTORY = MapFactory.create(SpleefMap.class, SpleefMap::new);

    @ArenaOption(name = "layers", description = "The layers for this spleef map.")
    private List<Layer> layers;

    @ArenaOption(name = "death-region", description = "The region where players will die if they fall into.")
    private Bounds deathRegion;

    private final Map<Position, Layer> positionToLayers = new HashMap<>();

    public SpleefMap() {

    }

    public SpleefMap(String name, Arena arena, MapType type, String world, @Nullable Bounds bounds,
            @Nullable Spawns spawns)
    {

        super(name, arena, type, world, bounds, spawns);

    }

    @Override
    public void postProcess() {

        super.postProcess();

        if (this.layers != null) {

            for (Layer layer : this.layers) {

                for (int x = layer.getBounds().getMinX(); x <= layer.getBounds().getMaxX(); x++) {

                    for (int y = layer.getBounds().getMinY(); y <= layer.getBounds().getMaxY(); y++) {

                        for (int z = layer.getBounds().getMinZ(); z <= layer.getBounds().getMaxZ(); z++) {

                            this.positionToLayers.put(Position.block(x, y, z), layer);

                        }

                    }

                }

            }

        }

    }

    @Override
    public LiveCompetition<SpleefCompetition> createCompetition(Arena arena) {

        if (!(arena instanceof SpleefArena spleefArena)) {

            throw new IllegalArgumentException("Arena must be a spleef arena!");

        }

        return new SpleefCompetition(spleefArena, arena.getType(), this);

    }

    public List<Layer> getLayers() {

        return this.layers == null ? List.of() : List.copyOf(this.layers);

    }

    public void addLayer(Layer layer) {

        if (this.layers == null) {

            this.layers = new ArrayList<>();

        }

        this.layers.add(layer);

    }

    public void addLayer(int index, Layer layer) {

        if (this.layers == null) {

            this.layers = new ArrayList<>();

        }

        this.layers.add(index, layer);

    }

    public void removeLayer(Layer layer) {

        this.layers.remove(layer);

    }

    public void clearLayers() {

        this.layers.clear();

    }

    @Nullable
    public Layer getLayer(Position position) {

        return this.positionToLayers.get(position);

    }

    @Nullable
    public Bounds getDeathRegion() {

        return this.deathRegion;

    }

    public void setDeathRegion(Bounds deathRegion) {

        this.deathRegion = deathRegion;

    }

    public static class Layer {

        @ArenaOption(name = "bounds", description = "The bounds of this layer.", required = true)
        private Bounds bounds;

        @ArenaOption(name = "block-data", description = "The block data of this layer.", required = true)
        private BlockData blockData;

        public Layer() {

        }

        public Layer(Bounds bounds, BlockData blockData) {

            this.bounds = bounds;
            this.blockData = blockData;

        }

        public Bounds getBounds() {

            return this.bounds;

        }

        public BlockData getBlockData() {

            return this.blockData;

        }

    }

}
