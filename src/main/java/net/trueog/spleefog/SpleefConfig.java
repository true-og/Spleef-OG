package net.trueog.spleefog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SpleefConfig {

    private final SpleefPlugin plugin;

    public SpleefConfig(SpleefPlugin plugin) {

        this.plugin = plugin;

    }

    public List<String> worldWhitelist() {

        return this.plugin.getConfig().getStringList("world-whitelist");

    }

    public boolean isWorldAllowed(String worldName) {

        if (worldName == null) {

            return false;

        }

        List<String> worlds = this.worldWhitelist();
        if (worlds.isEmpty()) {

            return true;

        }

        return worlds.stream().filter(name -> name != null).anyMatch(name -> name.equalsIgnoreCase(worldName));

    }

    public int minimumPlayers() {

        return Math.max(2, this.plugin.getConfig().getInt("match.minimum-players", 2));

    }

    public int waitingSeconds() {

        return Math.max(1, this.plugin.getConfig().getInt("match.waiting-seconds", 20));

    }

    public int timeLimitSeconds() {

        return Math.max(30, this.plugin.getConfig().getInt("match.time-limit-seconds", 300));

    }

    public int victorySeconds() {

        return Math.max(1, this.plugin.getConfig().getInt("match.victory-seconds", 5));

    }

    public boolean blockTeleports() {

        return this.plugin.getConfig().getBoolean("protection.block-teleports", true);

    }

    public boolean blockAllCommands() {

        return this.plugin.getConfig().getBoolean("protection.block-all-commands", false);

    }

    public Set<String> whitelistedCommands() {

        return this.commandSet("protection.whitelisted-commands");

    }

    public Set<String> blacklistedCommands() {

        return this.commandSet("protection.blacklisted-commands");

    }

    // Reads a command list into a lower-case set. Entries keep their spaces so
    // multi-word entries such as union home
    // can be matched against the front of what the player typed.
    private Set<String> commandSet(String path) {

        Set<String> result = new LinkedHashSet<>();
        for (String value : this.plugin.getConfig().getStringList(path)) {

            if (value == null || value.isBlank()) {

                continue;

            }

            String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("^/+", "").replaceAll("\\s+", " ");
            if (!normalized.isEmpty()) {

                result.add(normalized);

            }

        }

        return result;

    }

    // How many blocks of an arena floor may be restored in a single tick.
    public int resetBlocksPerTick() {

        return Math.max(256, this.plugin.getConfig().getInt("performance.reset-blocks-per-tick", 8192));

    }

    public boolean scoreboardEnabled() {

        return this.plugin.getConfig().getBoolean("scoreboard.enabled", true);

    }

    public Component scoreboardTitle() {

        return Messages.parse(this.plugin.getConfig().getString("scoreboard.title", "<aqua><bold>SPLEEF"));

    }

    public ItemStack classicTool() {

        return this.readItem("items.classic", Material.DIAMOND_SHOVEL);

    }

    public ItemStack bowTool() {

        return this.readItem("items.bow", Material.BOW);

    }

    private ItemStack readItem(String path, Material fallback) {

        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection(path);
        if (section == null) {

            return new ItemStack(fallback);

        }

        Material material = Material.matchMaterial(section.getString("material", fallback.name()));
        if (material == null || !material.isItem()) {

            this.plugin.getLogger().warning("Config value " + path + ".material is not a usable item; falling back to "
                    + fallback.name() + ".");
            material = fallback;

        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {

            return item;

        }

        String name = section.getString("name");
        if (name != null) {

            meta.displayName(Messages.parseItemName(name));

        }

        ConfigurationSection enchantments = section.getConfigurationSection("enchantments");
        if (enchantments != null) {

            for (String key : enchantments.getKeys(false)) {

                this.applyEnchantment(meta, path, key, enchantments.getInt(key));

            }

        }

        item.setItemMeta(meta);
        return item;

    }

    @SuppressWarnings("deprecation")
    private void applyEnchantment(ItemMeta meta, String path, String key, int level) {

        String name = key.toUpperCase(Locale.ROOT);
        Enchantment enchantment = Enchantment.getByName(name);
        if (enchantment == null) {

            enchantment = Enchantment
                    .getByKey(org.bukkit.NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT).replace(' ', '_')));

        }

        if (enchantment == null) {

            this.plugin.getLogger().warning("Unknown enchantment '" + key + "' in " + path + "; skipping it.");
            return;

        }

        meta.addEnchant(enchantment, Math.max(1, level), true);

    }

}
