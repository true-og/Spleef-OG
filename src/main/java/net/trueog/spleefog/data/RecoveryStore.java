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
    // Set when the last write failed, so the file is known to be behind the
    // in-memory state.
    private boolean dirty;

    public RecoveryStore(SpleefPlugin plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "recovery.yml");
        this.load();

    }

    // Records a snapshot for a player about to enter an arena. Returns false, and
    // keeps nothing, when the
    // snapshot could not be written to disk: the caller must then refuse the entry,
    // because a player whose
    // inventory is only held in memory loses it for good if the server stops before
    // they leave.
    public boolean store(UUID playerId, PlayerSnapshot snapshot) {

        PlayerSnapshot previous = this.snapshots.put(playerId, snapshot);
        if (this.save()) {

            SpleefAPI.markRecoveryPending(playerId);
            return true;

        }

        if (previous == null) {

            this.snapshots.remove(playerId);

        } else {

            this.snapshots.put(playerId, previous);

        }

        return false;

    }

    // Writes the current snapshots back out after one of them was changed in place.
    public boolean persist() {

        return this.save();

    }

    // Retries a write that failed earlier. Called once a second so a stale file,
    // which would replay an obsolete
    // snapshot over a player's real inventory after a restart, is corrected as soon
    // as the disk allows it.
    public void flushIfDirty() {

        if (this.dirty) {

            this.save();

        }

    }

    public PlayerSnapshot get(UUID playerId) {

        return this.snapshots.get(playerId);

    }

    public PlayerSnapshot remove(UUID playerId) {

        PlayerSnapshot value = this.snapshots.remove(playerId);
        SpleefAPI.clearRecoveryPending(playerId);
        if (!this.save() && value != null) {

            // The player has their state back, so the in-memory entry stays gone; the write
            // is retried from
            // flushIfDirty until the obsolete copy on disk is replaced.
            this.plugin.getLogger().severe("The recovery entry for " + playerId
                    + " is still on disk after it was restored; it will be rewritten when recovery.yml can be saved.");

        }

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

    private boolean save() {

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

            this.dirty = false;
            return true;

        } catch (IOException ex) {

            this.dirty = true;
            this.plugin.getLogger().severe("Could not save recovery.yml: " + ex.getMessage());
            return false;

        }

    }

}
