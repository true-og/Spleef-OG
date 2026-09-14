package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
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
    // Raw sections this plugin could not read, written back verbatim on every save.
    private final Map<String, Object> unreadable = new java.util.LinkedHashMap<>();
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

        // Strict: a recovery.yml that cannot be parsed is moved aside rather than
        // treated as empty, because the
        // next save would otherwise replace it and every pending recovery in it would
        // be gone.
        YamlConfiguration yaml = YamlFiles.load(this.file, this.plugin.getLogger());
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

                // Kept in the raw section below so the next save writes it back verbatim
                // instead of deleting the
                // only copy of somebody's inventory.
                this.unreadable.put(key, players.get(key));
                this.plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Could not read recovery entry '" + key + "'; it is being kept in recovery.yml untouched.", ex);

            }

        }

    }

    private boolean save() {

        // Serialisation failures are treated like disk failures. One snapshot that
        // cannot be written must not stop
        // every other player's entry from reaching the disk, and it must not escape
        // into the caller, where an
        // exception halfway through an entry or a restore leaves the player in neither
        // state.
        try {

            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : this.unreadable.entrySet()) {

                yaml.set("players." + entry.getKey(), entry.getValue());

            }

            for (Map.Entry<UUID, PlayerSnapshot> entry : this.snapshots.entrySet()) {

                entry.getValue().write(yaml, "players." + entry.getKey());

            }

            YamlFiles.save(yaml, this.file);
            this.dirty = false;
            return true;

        } catch (IOException | RuntimeException ex) {

            this.dirty = true;
            this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save recovery.yml.", ex);
            return false;

        }

    }

}
