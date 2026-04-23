package org.battleplugins.arena.spleef;

import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.config.ArenaConfigParser;
import org.battleplugins.arena.config.ParseException;
import org.battleplugins.arena.event.action.EventActionType;
import org.battleplugins.arena.spleef.action.GiveShovelAction;
import org.battleplugins.arena.spleef.action.PasteLayersAction;
import org.battleplugins.arena.spleef.api.SpleefApiListener;
import org.battleplugins.arena.spleef.arena.SpleefArena;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArenaSpleef extends JavaPlugin {

    public static EventActionType<GiveShovelAction> GIVE_SHOVEL = EventActionType.create("give-shovel",
            GiveShovelAction.class, GiveShovelAction::new);
    public static EventActionType<PasteLayersAction> PASTE_LAYERS = EventActionType.create("paste-layers",
            PasteLayersAction.class, PasteLayersAction::new);

    private static ArenaSpleef instance;

    private SpleefConfig config;

    private final NamespacedKey spleefItemKey = new NamespacedKey(this, "spleef_item");

    @Override
    public void onEnable() {

        instance = this;

        this.saveDefaultConfig();

        File configFile = new File(this.getDataFolder(), "config.yml");
        Configuration config = YamlConfiguration.loadConfiguration(configFile);
        try {

            this.config = ArenaConfigParser.newInstance(configFile.toPath(), SpleefConfig.class, config);

        } catch (ParseException e) {

            ParseException.handle(e);

            this.getSLF4JLogger().error("Failed to load Spleef configuration! Disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;

        }

        Path dataFolder = this.getDataFolder().toPath();
        Path arenasPath = dataFolder.resolve("arenas");
        if (Files.notExists(arenasPath)) {

            this.saveResource("arenas/spleef.yml", false);

        }

        BattleArena.getInstance().registerArena(this, "Spleef", SpleefArena.class, SpleefArena::new);

        this.getServer().getPluginManager().registerEvents(new SpleefApiListener(), this);

    }

    @Override
    public void onDisable() {

        Bukkit.getScheduler().cancelTasks(this);

    }

    public SpleefConfig getMainConfig() {

        return this.config;

    }

    public NamespacedKey getSpleefItemKey() {

        return this.spleefItemKey;

    }

    public static ArenaSpleef getInstance() {

        return instance;

    }

}
