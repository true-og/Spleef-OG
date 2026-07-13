package net.trueog.spleefog;

import net.trueog.spleefog.api.SpleefAPI;
import net.trueog.spleefog.arena.ArenaManager;
import net.trueog.spleefog.command.SpleefCommand;
import net.trueog.spleefog.data.ArenaRepository;
import net.trueog.spleefog.data.RecoveryStore;
import net.trueog.spleefog.data.StatsRepository;
import net.trueog.spleefog.editor.ArenaEditor;
import net.trueog.spleefog.hook.GameModeInventoriesHook;
import net.trueog.spleefog.hook.PlayerBountiesHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpleefPlugin extends JavaPlugin {

    private ArenaManager arenaManager;

    @Override
    public void onLoad() {

        SpleefFlags.register(this);

    }

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        SpleefConfig config = new SpleefConfig(this);
        WorldGuardSupport worldGuard = new WorldGuardSupport();
        ArenaRepository arenaRepository = new ArenaRepository(this);
        StatsRepository stats = new StatsRepository(this);
        RecoveryStore recovery = new RecoveryStore(this);
        GameModeInventoriesHook gameModeInventories = new GameModeInventoriesHook(this);
        this.arenaManager = new ArenaManager(this, config, worldGuard, arenaRepository, stats, recovery,
                gameModeInventories);

        ArenaEditor editor = new ArenaEditor(this, this.arenaManager);
        this.getServer().getPluginManager().registerEvents(editor, this);
        SpleefCommand commandHandler = new SpleefCommand(this, this.arenaManager, editor);
        PluginCommand command = this.getCommand("spleef");
        if (command == null) {

            throw new IllegalStateException("The spleef command is missing from plugin.yml");

        }

        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        this.arenaManager.start();
        PlayerBountiesHook.register(this);
        this.getLogger().info("Loaded " + this.arenaManager.sessions().size() + " standalone Spleef arena(s).");

    }

    @Override
    public void onDisable() {

        if (this.arenaManager != null) {

            this.arenaManager.shutdown();

        }

        SpleefAPI.clear();

    }

    public ArenaManager getArenaManager() {

        return this.arenaManager;

    }

}
