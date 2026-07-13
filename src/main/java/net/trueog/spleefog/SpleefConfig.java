package net.trueog.spleefog;

import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
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

    public boolean scoreboardEnabled() {

        return this.plugin.getConfig().getBoolean("scoreboard.enabled", true);

    }

    public String scoreboardTitle() {

        return color(this.plugin.getConfig().getString("scoreboard.title", "&b&lSPLEEF"));

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
        ItemStack item = new ItemStack(material == null ? fallback : material);
        ItemMeta meta = item.getItemMeta();
        String name = section.getString("name");
        if (name != null) {

            meta.setDisplayName(color(name));

        }

        ConfigurationSection enchantments = section.getConfigurationSection("enchantments");
        if (enchantments != null) {

            for (String key : enchantments.getKeys(false)) {

                Enchantment enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
                if (enchantment != null) {

                    meta.addEnchant(enchantment, enchantments.getInt(key), true);

                }

            }

        }

        item.setItemMeta(meta);
        return item;

    }

    @SuppressWarnings("deprecation")
    public static String color(String message) {

        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);

    }

}
