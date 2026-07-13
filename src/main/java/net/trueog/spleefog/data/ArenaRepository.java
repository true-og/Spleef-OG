package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    public ArenaRepository(SpleefPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");

    }

    public List<SpleefArena> load() {

        List<SpleefArena> arenas = new ArrayList<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) {

            return arenas;

        }

        for (String key : root.getKeys(false)) {

            ConfigurationSection section = root.getConfigurationSection(key);
            try {

                SpleefArena arena = this.readArena(key, section);
                if (arena != null) {

                    arenas.add(arena);

                }

            } catch (RuntimeException ex) {

                this.plugin.getLogger().warning("Could not load Spleef arena '" + key + "': " + ex.getMessage());

            }

        }

        return arenas;

    }

    public void save(Collection<SpleefArena> arenas) {

        YamlConfiguration yaml = new YamlConfiguration();
        for (SpleefArena arena : arenas) {

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
