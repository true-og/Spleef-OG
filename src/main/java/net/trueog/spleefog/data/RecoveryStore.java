package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.api.SpleefAPI;
import net.trueog.spleefog.player.PlayerSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RecoveryStore {

    private final SpleefPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();

    public RecoveryStore(SpleefPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "recovery.yml");
        this.load();

    }

    public PlayerSnapshot capture(org.bukkit.entity.Player player) {

        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        this.snapshots.put(player.getUniqueId(), snapshot);
        SpleefAPI.markRecoveryPending(player.getUniqueId());
        this.save();
        return snapshot;

    }

    public PlayerSnapshot get(UUID playerId) {

        return this.snapshots.get(playerId);

    }

    public PlayerSnapshot remove(UUID playerId) {

        PlayerSnapshot value = this.snapshots.remove(playerId);
        SpleefAPI.clearRecoveryPending(playerId);
        this.save();
        return value;

    }

    public boolean contains(UUID playerId) {

        return this.snapshots.containsKey(playerId);

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
                this.snapshots.put(playerId, PlayerSnapshot.read(yaml, "players." + key));
                SpleefAPI.markRecoveryPending(playerId);

            } catch (RuntimeException ex) {

                this.plugin.getLogger().warning("Ignoring invalid recovery entry '" + key + "'.");

            }

        }

    }

    private void save() {

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerSnapshot> entry : this.snapshots.entrySet()) {

            entry.getValue().write(yaml, "players." + entry.getKey());

        }

        // Saved through a temporary file and moved into place. A truncating in-place
        // write leaves a window where
        // a crash produces an unparseable file, and an unparseable file loads as empty,
        // discarding the stored
        // inventory of every player currently in an arena.
        File temporary = new File(this.file.getParentFile(), this.file.getName() + ".tmp");
        try {

            yaml.save(temporary);
            try {

                Files.move(temporary.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

            } catch (AtomicMoveNotSupportedException ex) {

                Files.move(temporary.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            }

        } catch (IOException ex) {

            this.plugin.getLogger().severe("Could not save recovery.yml: " + ex.getMessage());

        }

    }

}
