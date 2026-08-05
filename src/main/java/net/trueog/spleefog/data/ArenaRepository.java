package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.model.BlockBounds;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ArenaRepository {

    private final SpleefPlugin plugin;
    private final File file;
    // Names this plugin successfully read and is therefore allowed to rewrite.
    private final Set<String> managedKeys = new LinkedHashSet<>();

    public ArenaRepository(SpleefPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");

    }

    public List<SpleefArena> load() {

        List<SpleefArena> arenas = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        this.managedKeys.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) {

            return arenas;

        }

        for (String key : root.getKeys(false)) {

            ConfigurationSection section = root.getConfigurationSection(key);
            try {

                if (!seen.add(key.toLowerCase(Locale.ROOT))) {

                    // Two keys differing only in case would collide in memory and the survivor's
                    // save would delete
                    // both from disk. Leave the duplicate alone rather than silently destroying a
                    // real arena.
                    this.plugin.getLogger().warning("Ignoring Spleef arena '" + key
                            + "': another arena already uses that name. Rename one of them in arenas.yml.");
                    continue;

                }

                SpleefArena arena = this.readArena(key, section);
                if (arena != null) {

                    arenas.add(arena);
                    this.managedKeys.add(key);

                }

            } catch (RuntimeException ex) {

                // Deliberately left out of managedKeys: an arena this plugin could not read is
                // one it must not
                // rewrite, or the next save would erase a definition the operator still wants.
                this.plugin.getLogger().warning("Could not load Spleef arena '" + key + "': " + ex.getMessage()
                        + " Its definition is being kept in arenas.yml untouched.");

            }

        }

        return arenas;

    }

    public void save(Collection<SpleefArena> arenas) {

        // Start from what is on disk so entries this plugin does not manage survive
        // verbatim.
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        for (String key : this.managedKeys) {

            yaml.set("arenas." + key, null);

        }

        this.managedKeys.clear();
        for (SpleefArena arena : arenas) {

            this.managedKeys.add(arena.name());

            String path = "arenas." + arena.name();
            yaml.set(path + ".world", arena.worldName());
            yaml.set(path + ".region", arena.regionId());
            yaml.set(path + ".region-bounds", writeBounds(arena.regionBounds()));
            yaml.set(path + ".game", arena.gameType().name());
            yaml.set(path + ".enabled", arena.enabled());
            yaml.set(path + ".waiting-spawn", arena.waitingSpawn());
            yaml.set(path + ".spectator-spawn", arena.spectatorSpawn());
            yaml.set(path + ".spawns", arena.spawns());
            yaml.set(path + ".death-region", writeBounds(arena.deathRegion()));

            int index = 0;
            for (SpleefLayer layer : arena.layers()) {

                String layerPath = path + ".layers." + index++;
                yaml.set(layerPath + ".bounds", writeBounds(layer.bounds()));
                yaml.set(layerPath + ".block-data", layer.blockData().getAsString());

            }

        }

        try {

            yaml.save(this.file);

        } catch (IOException ex) {

            this.plugin.getLogger().severe("Could not save arenas.yml: " + ex.getMessage());

        }

    }

    private SpleefArena readArena(String name, ConfigurationSection section) {

        if (section == null) {

            return null;

        }

        String worldName = section.getString("world");
        String region = section.getString("region");
        BlockBounds regionBounds = readBounds(section.getConfigurationSection("region-bounds"));
        if (worldName == null || region == null || regionBounds == null || Bukkit.getWorld(worldName) == null) {

            throw new IllegalArgumentException("world, region, or region bounds are unavailable");

        }

        SpleefArena arena = new SpleefArena(name, worldName, region, regionBounds,
                GameType.parse(section.getString("game", "CLASSIC")));
        arena.enabled(section.getBoolean("enabled", true));
        arena.waitingSpawn(section.getLocation("waiting-spawn"));
        arena.spectatorSpawn(section.getLocation("spectator-spawn"));
        for (Object value : section.getList("spawns", List.of())) {

            if (value instanceof Location location) {

                arena.addSpawn(location);

            }

        }

        arena.deathRegion(readBounds(section.getConfigurationSection("death-region")));

        ConfigurationSection layers = section.getConfigurationSection("layers");
        if (layers != null) {

            for (String key : layers.getKeys(false)) {

                ConfigurationSection layer = layers.getConfigurationSection(key);
                BlockBounds bounds = readBounds(layer == null ? null : layer.getConfigurationSection("bounds"));
                String blockData = layer == null ? null : layer.getString("block-data");
                if (bounds != null && blockData != null) {

                    arena.addLayer(new SpleefLayer(bounds, Bukkit.createBlockData(blockData)));

                }

            }

        }

        return arena;

    }

    private static Map<String, Object> writeBounds(BlockBounds bounds) {

        if (bounds == null) {

            return null;

        }

        return Map.of("min-x", bounds.minX(), "min-y", bounds.minY(), "min-z", bounds.minZ(), "max-x", bounds.maxX(),
                "max-y", bounds.maxY(), "max-z", bounds.maxZ());

    }

    private static BlockBounds readBounds(ConfigurationSection section) {

        if (section == null) {

            return null;

        }

        return new BlockBounds(section.getInt("min-x"), section.getInt("min-y"), section.getInt("min-z"),
                section.getInt("max-x"), section.getInt("max-y"), section.getInt("max-z"));

    }

}
