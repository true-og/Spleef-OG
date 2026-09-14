package net.trueog.spleefog.model;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

// A location held by world name rather than by a live World reference.
//
// A Bukkit Location keeps a weak reference to its world and throws from getWorld() and serialize() once that world
// has been unloaded, and its YAML form cannot be read back at all while the world is not loaded yet. Arenas and
// recovery snapshots outlive both situations: a world manager may load the arena world after this plugin enables,
// and an origin world may be unloaded while its player is in a match. Storing the name and resolving it on demand
// keeps the definition intact through both and lets it come back the moment the world is available again.
public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    private static final String WORLD = "world";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    // Captures a live location. Returns null for a null location or one whose world
    // can no longer be named.
    public static StoredLocation of(Location location) {

        if (location == null || !location.isWorldLoaded()) {

            return null;

        }

        return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());

    }

    // Resolves against the currently loaded worlds. Returns null while the world is
    // not loaded, so callers can
    // treat an unavailable world exactly like a missing location.
    public Location toLocation() {

        World resolved = this.world == null ? null : Bukkit.getWorld(this.world);
        return resolved == null ? null : new Location(resolved, this.x, this.y, this.z, this.yaw, this.pitch);

    }

    public Map<String, Object> toMap() {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(WORLD, this.world);
        map.put(X, this.x);
        map.put(Y, this.y);
        map.put(Z, this.z);
        map.put(YAW, this.yaw);
        map.put(PITCH, this.pitch);
        return map;

    }

    // Reads either this plugin's map form or a Location that Bukkit serialised in
    // an earlier version. A serialised
    // Location whose world is not loaded deserialises to null, in which case there
    // is nothing to recover from it.
    public static StoredLocation from(Object value) {

        if (value instanceof StoredLocation stored) {

            return stored;

        }

        if (value instanceof Location location) {

            return of(location);

        }

        if (value instanceof ConfigurationSection section) {

            return from(section.getValues(false));

        }

        if (value instanceof Map<?, ?> map && map.get(WORLD) instanceof String world) {

            return new StoredLocation(world, number(map.get(X)), number(map.get(Y)), number(map.get(Z)),
                    (float) number(map.get(YAW)), (float) number(map.get(PITCH)));

        }

        return null;

    }

    public static StoredLocation read(ConfigurationSection section, String path) {

        return section == null ? null : from(section.get(path));

    }

    private static double number(Object value) {

        return value instanceof Number number ? number.doubleValue() : 0.0D;

    }

}
