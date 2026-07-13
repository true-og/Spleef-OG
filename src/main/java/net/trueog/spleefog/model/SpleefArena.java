package net.trueog.spleefog.model;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;

public final class SpleefArena {

    private final String name;
    private final String worldName;
    private final String regionId;
    private final BlockBounds regionBounds;
    private final List<Location> spawns = new ArrayList<>();
    private final List<SpleefLayer> layers = new ArrayList<>();
    private GameType gameType;
    private boolean enabled = true;
    private Location waitingSpawn;
    private Location spectatorSpawn;
    private BlockBounds deathRegion;

    public SpleefArena(String name, String worldName, String regionId, BlockBounds regionBounds, GameType gameType) {

        this.name = name;
        this.worldName = worldName;
        this.regionId = regionId;
        this.regionBounds = regionBounds;
        this.gameType = gameType;

    }

    public String name() {

        return this.name;

    }

    public String worldName() {

        return this.worldName;

    }

    public String regionId() {

        return this.regionId;

    }

    public BlockBounds regionBounds() {

        return this.regionBounds;

    }

    public GameType gameType() {

        return this.gameType;

    }

    public void gameType(GameType gameType) {

        this.gameType = gameType;

    }

    public boolean enabled() {

        return this.enabled;

    }

    public void enabled(boolean enabled) {

        this.enabled = enabled;

    }

    public Location waitingSpawn() {

        return clone(this.waitingSpawn);

    }

    public void waitingSpawn(Location location) {

        this.waitingSpawn = clone(location);

    }

    public Location spectatorSpawn() {

        return clone(this.spectatorSpawn);

    }

    public void spectatorSpawn(Location location) {

        this.spectatorSpawn = clone(location);

    }

    public List<Location> spawns() {

        return this.spawns.stream().map(SpleefArena::clone).toList();

    }

    public List<SpleefLayer> layers() {

        return List.copyOf(this.layers);

    }

    public BlockBounds deathRegion() {

        return this.deathRegion;

    }

    public void deathRegion(BlockBounds bounds) {

        this.deathRegion = bounds;

    }

    public void addSpawn(Location location) {

        this.spawns.add(clone(location));

    }

    public void clearSpawns() {

        this.spawns.clear();

    }

    public void addLayer(SpleefLayer layer) {

        this.layers.add(layer);

    }

    public void removeLayer(int index) {

        this.layers.remove(index);

    }

    public void clearLayers() {

        this.layers.clear();

    }

    public boolean contains(Location location) {

        World world = location == null ? null : location.getWorld();
        return world != null && world.getName().equalsIgnoreCase(this.worldName)
                && this.regionBounds.contains(location);

    }

    public boolean isComplete() {

        return this.waitingSpawn != null && this.spectatorSpawn != null && this.spawns.size() >= 2
                && !this.layers.isEmpty() && this.deathRegion != null;

    }

    public int capacity() {

        return this.spawns.size();

    }

    private static Location clone(Location location) {

        return location == null ? null : location.clone();

    }

}
