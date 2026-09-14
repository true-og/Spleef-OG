package net.trueog.spleefog.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;

// An arena definition. Locations are kept by world name (see StoredLocation) so a definition survives its world
// being loaded after this plugin, or unloaded while the plugin runs; the Location accessors resolve on demand and
// return null while the world is unavailable.
public final class SpleefArena {

    private final String name;
    private final String worldName;
    private final String regionId;
    private final BlockBounds regionBounds;
    private final List<StoredLocation> spawns = new ArrayList<>();
    private final List<SpleefLayer> layers = new ArrayList<>();
    private GameType gameType;
    private boolean enabled = true;
    private StoredLocation waitingSpawn;
    private StoredLocation spectatorSpawn;
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

        return resolve(this.waitingSpawn);

    }

    public StoredLocation storedWaitingSpawn() {

        return this.waitingSpawn;

    }

    public void waitingSpawn(Location location) {

        this.waitingSpawn = StoredLocation.of(location);

    }

    public void waitingSpawn(StoredLocation location) {

        this.waitingSpawn = location;

    }

    public Location spectatorSpawn() {

        return resolve(this.spectatorSpawn);

    }

    public StoredLocation storedSpectatorSpawn() {

        return this.spectatorSpawn;

    }

    public void spectatorSpawn(Location location) {

        this.spectatorSpawn = StoredLocation.of(location);

    }

    public void spectatorSpawn(StoredLocation location) {

        this.spectatorSpawn = location;

    }

    // Resolved spawns, in order. Any spawn whose world is not loaded is left out,
    // which is why callers must treat
    // the size of this list, not capacity(), as the number of usable spawns at that
    // moment.
    public List<Location> spawns() {

        return this.spawns.stream().map(SpleefArena::resolve).filter(Objects::nonNull).toList();

    }

    public List<StoredLocation> storedSpawns() {

        return List.copyOf(this.spawns);

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

        StoredLocation stored = StoredLocation.of(location);
        if (stored != null) {

            this.spawns.add(stored);

        }

    }

    public void addSpawn(StoredLocation location) {

        if (location != null) {

            this.spawns.add(location);

        }

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

    private static Location resolve(StoredLocation location) {

        return location == null ? null : location.toLocation();

    }

}
