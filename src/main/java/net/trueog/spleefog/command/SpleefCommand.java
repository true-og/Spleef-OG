package net.trueog.spleefog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.arena.ArenaManager;
import net.trueog.spleefog.arena.ArenaSession;
import net.trueog.spleefog.data.StatsRepository.PlayerStats;
import net.trueog.spleefog.editor.ArenaEditor;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpleefCommand implements CommandExecutor, TabCompleter {

    private final SpleefPlugin plugin;
    private final ArenaManager arenaManager;
    private final ArenaEditor editor;

    public SpleefCommand(SpleefPlugin plugin, ArenaManager arenaManager, ArenaEditor editor) {

        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.editor = editor;

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args)
    {

        if (args.length == 0) {

            this.help(sender);
            return true;

        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {

            case "arenas" -> this.showAvailable(sender);
            case "join" -> this.join(sender, args);
            case "leave" -> this.leave(sender);
            case "spectate", "spec" -> this.spectate(sender, args);
            case "stats" -> this.stats(sender, args);
            case "create" -> this.create(sender, args);
            case "delete" -> this.delete(sender, args);
            case "setwait" -> this.setLocation(sender, args, LocationType.WAITING);
            case "setspectator" -> this.setLocation(sender, args, LocationType.SPECTATOR);
            case "addspawn" -> this.setLocation(sender, args, LocationType.PLAYER);
            case "clearspawns" -> this.clearSpawns(sender, args);
            case "mode" -> this.mode(sender, args);
            case "enable", "disable" -> this.enabled(sender, args, subcommand.equals("enable"));
            case "layer" -> this.layer(sender, args);
            case "deathregion" -> this.deathRegion(sender, args);
            case "info" -> this.info(sender, args);
            case "cancel" -> this.cancel(sender);
            case "reload" -> this.reload(sender);
            default -> this.help(sender);

        }

        return true;

    }

    private void join(CommandSender sender, String[] args) {

        Player player = this.player(sender);
        if (player == null || !this.require(sender, "spleef.play")) {

            return;

        }

        if (args.length < 2) {

            this.showAvailable(sender);
            return;

        }

        ArenaSession session = this.arenaManager.get(args[1]);
        if (session == null) {

            Messages.send(sender, "&cArena not found. Use &b/spleef arenas &cto see available arenas.");

        } else if (!session.join(player)) {

            Messages.send(sender, "&cThat arena is unavailable, full, or already in progress.");

        }

    }

    private void leave(CommandSender sender) {

        Player player = this.player(sender);
        if (player == null) {

            return;

        }

        ArenaSession session = this.arenaManager.session(player);
        if (session == null) {

            Messages.send(sender, "&cYou are not in Spleef.");
            return;

        }

        session.leave(player);
        Messages.send(sender, "&7You left Spleef.");

    }

    private void spectate(CommandSender sender, String[] args) {

        Player player = this.player(sender);
        if (player == null || !this.require(sender, "spleef.spectate")) {

            return;

        }

        if (args.length < 2) {

            Messages.send(sender, "&7Usage: &b/spleef spectate <arena>");
            return;

        }

        ArenaSession session = this.arenaManager.get(args[1]);
        if (session == null || !session.spectate(player)) {

            Messages.send(sender, "&cThat arena cannot be spectated right now.");

        }

    }

    private void showAvailable(CommandSender sender) {

        List<ArenaSession> available = this.arenaManager.availableSessions();
        if (available.isEmpty()) {

            Messages.send(sender, "&7No arenas are currently available.");
            return;

        }

        Messages.send(sender, "&7Available arenas:");
        for (ArenaSession session : available) {

            Messages.send(sender,
                    "&b" + session.arena().name() + " &8- &7" + session.arena().gameType().name() + " &8- &7"
                            + session.playerCount() + "/" + session.arena().capacity() + " &8- &f/spleef join "
                            + session.arena().name());

        }

    }

    private void stats(CommandSender sender, String[] args) {

        if (!this.require(sender, "spleef.stats")) {

            return;

        }

        PlayerStats stats;
        if (args.length < 2) {

            Player player = this.player(sender);
            if (player == null) {

                return;

            }

            stats = this.arenaManager.stats().get(player.getUniqueId(), player.getName());

        } else {

            stats = this.arenaManager.stats().findByName(args[1]);
            if (stats == null) {

                Messages.send(sender, "&cNo Spleef statistics were found for that player.");
                return;

            }

        }

        Messages.send(sender, "&b" + stats.name() + "&7: &a" + stats.wins() + " wins&7, &c" + stats.losses()
                + " losses&7, &e" + stats.ties() + " ties&7, &f" + stats.games() + " games");

    }

    private void create(CommandSender sender, String[] args) {

        Player player = this.adminPlayer(sender);
        if (player == null || args.length < 2) {

            Messages.send(sender, "&7Usage: &b/spleef create <name> [classic|bow]");
            return;

        }

        GameType gameType;
        try {

            gameType = GameType.parse(args.length >= 3 ? args[2] : "classic");

        } catch (IllegalArgumentException ex) {

            Messages.send(sender, "&cMode must be &bclassic &cor &bbow&c.");
            return;

        }

        ArenaManager.CreateResult result = this.arenaManager.create(player, args[1], gameType);
        Messages.send(sender, (result.success() ? "&a" : "&c") + result.message());

    }

    private void delete(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        if (this.arenaManager.delete(arena)) {

            Messages.send(sender, "&aDeleted arena &b" + arena.name() + "&a.");

        } else {

            Messages.send(sender, "&cAn active arena cannot be deleted.");

        }

    }

    private void setLocation(CommandSender sender, String[] args, LocationType type) {

        Player player = this.adminPlayer(sender);
        SpleefArena arena = this.adminArena(sender, args, 1);
        if (player == null || arena == null) {

            return;

        }

        if (!this.arenaManager.isSetupLocationValid(arena, player.getLocation())) {

            Messages.send(sender, "&cStand inside arena region &b" + arena.regionId() + "&c.");
            return;

        }

        switch (type) {

            case WAITING -> arena.waitingSpawn(player.getLocation());
            case SPECTATOR -> arena.spectatorSpawn(player.getLocation());
            case PLAYER -> arena.addSpawn(player.getLocation());

        }

        this.arenaManager.saveArenas();
        Messages.send(sender, "&aSaved the " + type.description + " for &b" + arena.name() + "&a.");

    }

    private void clearSpawns(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        arena.clearSpawns();
        this.arenaManager.saveArenas();
        Messages.send(sender, "&aCleared player spawns for &b" + arena.name() + "&a.");

    }

    private void mode(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null || args.length < 3) {

            Messages.send(sender, "&7Usage: &b/spleef mode <arena> <classic|bow>");
            return;

        }

        try {

            arena.gameType(GameType.parse(args[2]));
            this.arenaManager.saveArenas();
            Messages.send(sender, "&aMode set to &b" + arena.gameType().name() + "&a.");

        } catch (IllegalArgumentException ex) {

            Messages.send(sender, "&cMode must be &bclassic &cor &bbow&c.");

        }

    }

    private void enabled(CommandSender sender, String[] args, boolean enabled) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        ArenaSession session = this.arenaManager.get(arena.name());
        if (!enabled && session.isActive()) {

            Messages.send(sender, "&cWait for the active match to finish before disabling this arena.");
            return;

        }

        arena.enabled(enabled);
        this.arenaManager.saveArenas();
        Messages.send(sender, "&aArena &b" + arena.name() + " &ais now " + (enabled ? "enabled" : "disabled") + ".");

    }

    private void layer(CommandSender sender, String[] args) {

        if (!this.require(sender, "spleef.admin") || args.length < 3) {

            Messages.send(sender, "&7Usage: &b/spleef layer <add|remove|list|clear> <arena> [index]");
            return;

        }

        SpleefArena arena = this.arenaManager.get(args[2]) == null ? null : this.arenaManager.get(args[2]).arena();
        if (arena == null) {

            Messages.send(sender, "&cArena not found.");
            return;

        }

        switch (args[1].toLowerCase(Locale.ROOT)) {

            case "add" -> {

                Player player = this.player(sender);
                if (player != null) {

                    this.editor.beginLayer(player, arena);

                }

            }
            case "remove" -> {

                if (args.length < 4) {

                    Messages.send(sender, "&7Usage: &b/spleef layer remove <arena> <index>");
                    return;

                }

                try {

                    int index = Integer.parseInt(args[3]) - 1;
                    if (index < 0 || index >= arena.layers().size()) {

                        throw new NumberFormatException();

                    }

                    arena.removeLayer(index);
                    this.arenaManager.saveArenas();
                    Messages.send(sender, "&aLayer removed.");

                } catch (NumberFormatException ex) {

                    Messages.send(sender, "&cInvalid layer index.");

                }

            }
            case "list" -> {

                if (arena.layers().isEmpty()) {

                    Messages.send(sender, "&7This arena has no layers.");

                }

                int index = 1;
                for (SpleefLayer layer : arena.layers()) {

                    Messages.send(sender, "&b#" + index++ + " &7" + layer.blockData().getAsString() + " &8- &7"
                            + layer.bounds().minX() + "," + layer.bounds().minY() + "," + layer.bounds().minZ() + " to "
                            + layer.bounds().maxX() + "," + layer.bounds().maxY() + "," + layer.bounds().maxZ());

                }

            }
            case "clear" -> {

                arena.clearLayers();
                this.arenaManager.saveArenas();
                Messages.send(sender, "&aAll layers cleared.");

            }
            default -> Messages.send(sender, "&7Usage: &b/spleef layer <add|remove|list|clear> <arena> [index]");

        }

    }

    private void deathRegion(CommandSender sender, String[] args) {

        Player player = this.adminPlayer(sender);
        SpleefArena arena = this.adminArena(sender, args, 1);
        if (player != null && arena != null) {

            this.editor.beginDeathRegion(player, arena);

        }

    }

    private void info(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        ArenaSession session = this.arenaManager.get(arena.name());
        Messages.send(sender, "&b" + arena.name() + " &8- &7region &f" + arena.regionId() + "&7, world &f"
                + arena.worldName() + "&7, mode &f" + arena.gameType() + "&7, state &f" + session.state());
        Messages.send(sender,
                "&7Spawns: &f" + arena.spawns().size() + "&7, layers: &f" + arena.layers().size() + "&7, complete: "
                        + (arena.isComplete() ? "&aYes" : "&cNo") + "&7, enabled: "
                        + (arena.enabled() ? "&aYes" : "&cNo"));

    }

    private void cancel(CommandSender sender) {

        Player player = this.player(sender);
        if (player != null) {

            Messages.send(sender,
                    this.editor.cancel(player) ? "&7Arena edit cancelled." : "&7You are not editing an arena.");

        }

    }

    private void reload(CommandSender sender) {

        if (!this.require(sender, "spleef.admin")) {

            return;

        }

        this.plugin.reloadConfig();
        Messages.send(sender, "&aConfiguration reloaded.");

    }

    private SpleefArena adminArena(CommandSender sender, String[] args, int index) {

        if (!this.require(sender, "spleef.admin")) {

            return null;

        }

        if (args.length <= index) {

            Messages.send(sender, "&cAn arena name is required.");
            return null;

        }

        ArenaSession session = this.arenaManager.get(args[index]);
        if (session == null) {

            Messages.send(sender, "&cArena not found.");
            return null;

        }

        return session.arena();

    }

    private Player adminPlayer(CommandSender sender) {

        return this.require(sender, "spleef.admin") ? this.player(sender) : null;

    }

    private Player player(CommandSender sender) {

        if (sender instanceof Player player) {

            return player;

        }

        Messages.send(sender, "&cThat command can only be used by a player.");
        return null;

    }

    private boolean require(CommandSender sender, String permission) {

        if (sender.hasPermission(permission)) {

            return true;

        }

        Messages.send(sender, "&cYou do not have permission to do that.");
        return false;

    }

    private void help(CommandSender sender) {

        Messages.send(sender, "&b/spleef arenas &8- &7show available arenas");
        Messages.send(sender, "&b/spleef join <arena> &8- &7join an arena");
        Messages.send(sender, "&b/spleef leave &8- &7leave Spleef");
        Messages.send(sender, "&b/spleef spectate <arena> &8- &7spectate an arena");
        Messages.send(sender, "&b/spleef stats [player] &8- &7show Spleef statistics");
        if (sender.hasPermission("spleef.admin")) {

            Messages.send(sender,
                    "&b/spleef create <name> [classic|bow] &8- &7create an arena in your WorldGuard region");
            Messages.send(sender, "&b/spleef info <arena> &8- &7show setup status");

        }

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args)
    {

        if (args.length == 1) {

            List<String> commands = new ArrayList<>(List.of("arenas", "join", "leave", "spectate", "stats"));
            if (sender.hasPermission("spleef.admin")) {

                commands.addAll(List.of("create", "delete", "setwait", "setspectator", "addspawn", "clearspawns",
                        "mode", "enable", "disable", "layer", "deathregion", "info", "cancel", "reload"));

            }

            return matches(args[0], commands);

        }

        if (args.length == 2
                && List.of("join", "spectate", "delete", "setwait", "setspectator", "addspawn", "clearspawns", "mode",
                        "enable", "disable", "deathregion", "info").contains(args[0].toLowerCase(Locale.ROOT)))
        {

            return matches(args[1], this.arenaManager.sessions().stream().map(value -> value.arena().name()).toList());

        }

        if (args.length == 2 && args[0].equalsIgnoreCase("layer")) {

            return matches(args[1], List.of("add", "remove", "list", "clear"));

        }

        if (args.length == 3 && args[0].equalsIgnoreCase("layer")) {

            return matches(args[2], this.arenaManager.sessions().stream().map(value -> value.arena().name()).toList());

        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("mode"))) {

            return matches(args[2], List.of("classic", "bow"));

        }

        return List.of();

    }

    private static List<String> matches(String input, List<String> values) {

        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();

    }

    private enum LocationType {

        WAITING("waiting spawn"), SPECTATOR("spectator spawn"), PLAYER("player spawn");

        private final String description;

        LocationType(String description) {

            this.description = description;

        }

    }

}
