package net.trueog.spleefog.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

// How every data file this plugin owns is read and written.
//
// Writes go through a temporary file that is moved into place, so a crash mid-write leaves the previous complete
// file rather than a truncated one. Reads are strict: Bukkit's loadConfiguration() logs a parse failure and hands
// back an empty configuration, and an empty configuration saved back over the original is how a single bad byte
// turns into every arena, statistic, or pending recovery being gone for good. A file that cannot be parsed is moved
// aside under a timestamped name instead, so it survives for an operator to repair.
final class YamlFiles {

    private YamlFiles() {

    }

    static void save(YamlConfiguration yaml, File file) throws IOException {

        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {

            throw new IOException("Could not create " + parent);

        }

        File temporary = new File(parent, file.getName() + ".tmp");
        yaml.save(temporary);
        try {

            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (AtomicMoveNotSupportedException ex) {

            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        }

    }

    // Returns the parsed file, or an empty configuration when the file does not
    // exist or could not be read. In the
    // latter case the original is renamed out of the way first so that nothing this
    // plugin later saves can replace
    // it, and the logger is told where it went.
    static YamlConfiguration load(File file, Logger logger) {

        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.isFile()) {

            return yaml;

        }

        try {

            yaml.load(file);
            return yaml;

        } catch (IOException | InvalidConfigurationException ex) {

            File aside = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
            String moved;
            try {

                Files.move(file.toPath(), aside.toPath(), StandardCopyOption.ATOMIC_MOVE);
                moved = "It was moved to " + aside.getName() + " so it can be repaired";

            } catch (IOException moveFailure) {

                moved = "It could not be moved aside (" + moveFailure.getMessage()
                        + "), so it will be overwritten by the next save";

            }

            logger.severe("Could not read " + file.getName() + ": " + ex.getMessage() + ". " + moved
                    + "; the plugin is continuing without its contents.");
            return new YamlConfiguration();

        }

    }

}
