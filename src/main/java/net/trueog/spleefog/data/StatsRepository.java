package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class StatsRepository {

    private final SpleefPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    public StatsRepository(SpleefPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.load();

    }

    public PlayerStats get(UUID playerId, String name) {

        PlayerStats value = this.stats.computeIfAbsent(playerId, ignored -> new PlayerStats(name));
        if (name != null && !name.isBlank()) {

            value.name = name;

        }

        return value;

    }

    public PlayerStats findByName(String name) {

        return this.stats.values().stream().filter(value -> value.name.equalsIgnoreCase(name)).findFirst().orElse(null);

    }

    public void recordWin(UUID playerId, String name) {

        PlayerStats value = this.get(playerId, name);
        value.wins++;
        value.games++;

    }

    public void recordLoss(UUID playerId, String name) {

        PlayerStats value = this.get(playerId, name);
        value.losses++;
        value.games++;

    }

    public void recordTie(UUID playerId, String name) {

        PlayerStats value = this.get(playerId, name);
        value.ties++;
        value.games++;

    }

    public void save() {

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : this.stats.entrySet()) {

            String path = "players." + entry.getKey();
            PlayerStats value = entry.getValue();
            yaml.set(path + ".name", value.name);
            yaml.set(path + ".wins", value.wins);
            yaml.set(path + ".losses", value.losses);
            yaml.set(path + ".ties", value.ties);
            yaml.set(path + ".games", value.games);

        }

        try {

            yaml.save(this.file);

        } catch (IOException ex) {

            this.plugin.getLogger().severe("Could not save stats.yml: " + ex.getMessage());

        }

    }

    private void load() {

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {

            return;

        }

        for (String key : players.getKeys(false)) {

            try {

                UUID playerId = UUID.fromString(key);
                String path = "players." + key;
                PlayerStats value = new PlayerStats(yaml.getString(path + ".name", "Unknown"));
                value.wins = yaml.getInt(path + ".wins");
                value.losses = yaml.getInt(path + ".losses");
                value.ties = yaml.getInt(path + ".ties");
                value.games = yaml.getInt(path + ".games");
                this.stats.put(playerId, value);

            } catch (IllegalArgumentException ignored) {

                this.plugin.getLogger().warning("Ignoring invalid player UUID in stats.yml: " + key);

            }

        }

    }

    public static final class PlayerStats {

        private String name;
        private int wins;
        private int losses;
        private int ties;
        private int games;

        private PlayerStats(String name) {

            this.name = name == null ? "Unknown" : name;

        }

        public String name() {

            return this.name;

        }

        public int wins() {

            return this.wins;

        }

        public int losses() {

            return this.losses;

        }

        public int ties() {

            return this.ties;

        }

        public int games() {

            return this.games;

        }

    }

}
