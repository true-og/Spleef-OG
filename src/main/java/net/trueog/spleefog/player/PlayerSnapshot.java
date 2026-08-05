package net.trueog.spleefog.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;

public final class PlayerSnapshot {

    private final Location location;
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final ItemStack cursor;
    private final List<PotionEffect> potionEffects;
    private final GameMode gameMode;
    private final float experience;
    private final int level;
    private final int totalExperience;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final boolean allowFlight;
    private final boolean flying;
    private final int fireTicks;
    private final float fallDistance;
    private final transient Scoreboard scoreboard;

    private PlayerSnapshot(Location location, ItemStack[] storage, ItemStack[] armor, ItemStack[] extra,
            ItemStack cursor, List<PotionEffect> potionEffects, GameMode gameMode, float experience, int level,
            int totalExperience, double health, int foodLevel, float saturation, float exhaustion, boolean allowFlight,
            boolean flying, int fireTicks, float fallDistance, Scoreboard scoreboard)
    {

        this.location = location;
        this.storage = storage;
        this.armor = armor;
        this.extra = extra;
        this.cursor = cursor;
        this.potionEffects = potionEffects;
        this.gameMode = gameMode;
        this.experience = experience;
        this.level = level;
        this.totalExperience = totalExperience;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.fireTicks = fireTicks;
        this.fallDistance = fallDistance;
        this.scoreboard = scoreboard;

    }

    public static PlayerSnapshot capture(Player player) {

        return new PlayerSnapshot(player.getLocation().clone(), clone(player.getInventory().getStorageContents()),
                clone(player.getInventory().getArmorContents()), clone(player.getInventory().getExtraContents()),
                player.getItemOnCursor().clone(), new ArrayList<>(player.getActivePotionEffects()),
                player.getGameMode(), player.getExp(), player.getLevel(), player.getTotalExperience(),
                player.getHealth(), player.getFoodLevel(), player.getSaturation(), player.getExhaustion(),
                player.getAllowFlight(), player.isFlying(), player.getFireTicks(), player.getFallDistance(),
                player.getScoreboard());

    }

    public Location location() {

        return this.location == null ? null : this.location.clone();

    }

    public void restore(Player player) {

        player.getInventory().setStorageContents(clone(this.storage));
        player.getInventory().setArmorContents(clone(this.armor));
        player.getInventory().setExtraContents(clone(this.extra));
        // Restored explicitly: closing the inventory on entry would otherwise have
        // thrown the carried stack on the
        // ground back on the SMP.
        player.setItemOnCursor(this.cursor == null ? null : this.cursor.clone());
        player.updateInventory();

        for (PotionEffect effect : player.getActivePotionEffects()) {

            player.removePotionEffect(effect.getType());

        }

        player.addPotionEffects(this.potionEffects);
        player.setGameMode(this.gameMode);
        player.setExp(this.experience);
        player.setLevel(this.level);
        player.setTotalExperience(this.totalExperience);
        double maximumHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.max(0.5D, Math.min(this.health, maximumHealth)));
        player.setFoodLevel(this.foodLevel);
        player.setSaturation(this.saturation);
        player.setExhaustion(this.exhaustion);
        player.setAllowFlight(this.allowFlight);
        player.setFlying(this.allowFlight && this.flying);
        player.setFireTicks(this.fireTicks);
        player.setFallDistance(this.fallDistance);
        player.setScoreboard(
                this.scoreboard == null ? Bukkit.getScoreboardManager().getMainScoreboard() : this.scoreboard);
        Location destination = safeDestination(this.location);
        if (destination != null) {

            player.teleport(destination);

        }

    }

    // Falls back to a world spawn when the recorded world is gone. Skipping the
    // teleport instead would leave the
    // player standing in the arena with their own inventory back, which is worse
    // than landing at spawn.
    private static Location safeDestination(Location location) {

        if (location != null && location.getWorld() != null) {

            return location;

        }

        World fallback = Bukkit.getWorld("world");
        if (fallback == null) {

            fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        }

        return fallback == null ? null : fallback.getSpawnLocation();

    }

    public void write(ConfigurationSection yaml, String path) {

        yaml.set(path + ".location", this.location);
        yaml.set(path + ".storage", Arrays.asList(this.storage));
        yaml.set(path + ".armor", Arrays.asList(this.armor));
        yaml.set(path + ".extra", Arrays.asList(this.extra));
        yaml.set(path + ".cursor", this.cursor);
        yaml.set(path + ".game-mode", this.gameMode.name());
        yaml.set(path + ".experience", this.experience);
        yaml.set(path + ".level", this.level);
        yaml.set(path + ".total-experience", this.totalExperience);
        yaml.set(path + ".health", this.health);
        yaml.set(path + ".food-level", this.foodLevel);
        yaml.set(path + ".saturation", this.saturation);
        yaml.set(path + ".exhaustion", this.exhaustion);
        yaml.set(path + ".allow-flight", this.allowFlight);
        yaml.set(path + ".flying", this.flying);
        yaml.set(path + ".fire-ticks", this.fireTicks);
        yaml.set(path + ".fall-distance", this.fallDistance);

        List<Map<String, Object>> effects = new ArrayList<>();
        for (PotionEffect effect : this.potionEffects) {

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", effect.getType().getName());
            value.put("duration", effect.getDuration());
            value.put("amplifier", effect.getAmplifier());
            value.put("ambient", effect.isAmbient());
            value.put("particles", effect.hasParticles());
            value.put("icon", effect.hasIcon());
            effects.add(value);

        }

        yaml.set(path + ".potion-effects", effects);

    }

    public static PlayerSnapshot read(ConfigurationSection yaml, String path) {

        Location location = yaml.getLocation(path + ".location");
        ItemStack[] storage = readItems(yaml.getList(path + ".storage", List.of()), 36);
        ItemStack[] armor = readItems(yaml.getList(path + ".armor", List.of()), 4);
        ItemStack[] extra = readItems(yaml.getList(path + ".extra", List.of()), 1);
        List<PotionEffect> effects = readEffects(yaml.getMapList(path + ".potion-effects"));
        GameMode gameMode;
        try {

            gameMode = GameMode.valueOf(yaml.getString(path + ".game-mode", "SURVIVAL"));

        } catch (IllegalArgumentException ignored) {

            gameMode = GameMode.SURVIVAL;

        }

        ItemStack cursor = yaml.getItemStack(path + ".cursor");
        return new PlayerSnapshot(location, storage, armor, extra, cursor, effects, gameMode,
                (float) yaml.getDouble(path + ".experience"), yaml.getInt(path + ".level"),
                yaml.getInt(path + ".total-experience"), yaml.getDouble(path + ".health", 20.0D),
                yaml.getInt(path + ".food-level", 20), (float) yaml.getDouble(path + ".saturation", 5.0D),
                (float) yaml.getDouble(path + ".exhaustion"), yaml.getBoolean(path + ".allow-flight"),
                yaml.getBoolean(path + ".flying"), yaml.getInt(path + ".fire-ticks"),
                (float) yaml.getDouble(path + ".fall-distance"), null);

    }

    private static ItemStack[] clone(ItemStack[] contents) {

        ItemStack[] result = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {

            result[i] = contents[i] == null ? null : contents[i].clone();

        }

        return result;

    }

    private static ItemStack[] readItems(List<?> values, int defaultSize) {

        ItemStack[] result = new ItemStack[Math.max(defaultSize, values.size())];
        for (int i = 0; i < values.size(); i++) {

            if (values.get(i) instanceof ItemStack item) {

                result[i] = item;

            }

        }

        return result;

    }

    private static List<PotionEffect> readEffects(List<Map<?, ?>> values) {

        List<PotionEffect> result = new ArrayList<>();
        for (Map<?, ?> value : values) {

            PotionEffectType type = PotionEffectType.getByName(String.valueOf(value.get("type")));
            if (type == null) {

                continue;

            }

            result.add(new PotionEffect(type, number(value.get("duration")), number(value.get("amplifier")),
                    bool(value.get("ambient")), bool(value.get("particles")), bool(value.get("icon"))));

        }

        return result;

    }

    private static int number(Object value) {

        return value instanceof Number number ? number.intValue() : 0;

    }

    private static boolean bool(Object value) {

        return value instanceof Boolean bool && bool;

    }

}
