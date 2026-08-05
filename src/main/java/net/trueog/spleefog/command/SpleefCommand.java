package net.trueog.spleefog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.arena.ArenaManager;
import net.trueog.spleefog.arena.ArenaSession;
import net.trueog.spleefog.data.StatsRepository.PlayerStats;
import net.trueog.spleefog.editor.ArenaEditor;
import net.trueog.spleefog.model.GameType;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpleefCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "spleef.admin";

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
            case "help" -> this.help(sender);
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

            Messages.send(sender,
                    Messages.body().append(Messages.bad("Arena not found. Use "))
                            .append(Messages.value("/spleef arenas")).append(Messages.bad(" to see what is available."))
                            .build());

        } else if (!session.join(player)) {

            Messages.send(sender, Messages.bad("That arena is unavailable, full, or already in progress."));

        }

    }

    private void leave(CommandSender sender) {

        Player player = this.player(sender);
        if (player == null) {

            return;

        }

        ArenaSession session = this.arenaManager.session(player);
        if (session == null) {

            Messages.send(sender, Messages.bad("You are not in Spleef."));
            return;

        }

        session.leave(player);
        Messages.send(sender, Messages.grey("You left Spleef."));

    }

    private void spectate(CommandSender sender, String[] args) {

        Player player = this.player(sender);
        if (player == null || !this.require(sender, "spleef.spectate")) {

            return;

        }

        if (args.length < 2) {

            this.usage(sender, "/spleef spectate <arena>");
            return;

        }

        ArenaSession session = this.arenaManager.get(args[1]);
        if (session == null || !session.spectate(player)) {

            Messages.send(sender, Messages.bad("That arena cannot be spectated right now."));

        }

    }

    private void showAvailable(CommandSender sender) {

        List<ArenaSession> available = this.arenaManager.availableSessions();
        if (available.isEmpty()) {

            Messages.send(sender, Messages.grey("No arenas are currently available."));
            return;

        }

        Messages.send(sender, Messages.grey("Available arenas:"));
        for (ArenaSession session : available) {

            String name = session.arena().name();
            Messages.send(sender,
                    Messages.body().append(Messages.value(name)).append(Component.text(" - "))
                            .append(Messages.name(session.arena().gameType().name())).append(Component.text(" - "))
                            .append(Messages.name(session.playerCount() + "/" + session.arena().capacity()))
                            .append(Component.text(" - "))
                            .append(Messages.good("/spleef join " + name)
                                    .clickEvent(ClickEvent.runCommand("/spleef join " + name))
                                    .hoverEvent(HoverEvent.showText(Messages.grey("Click to join " + name))))
                            .build());

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

                Messages.send(sender, Messages.bad("No Spleef statistics were found for that player."));
                return;

            }

        }

        Messages.send(sender,
                Messages.body().append(Messages.value(stats.name())).append(Component.text(": "))
                        .append(Messages.good(stats.wins() + " wins")).append(Component.text(", "))
                        .append(Messages.bad(stats.losses() + " losses")).append(Component.text(", "))
                        .append(Messages.warn(stats.ties() + " ties")).append(Component.text(", "))
                        .append(Messages.name(stats.games() + " games")).build());

    }

    private void create(CommandSender sender, String[] args) {

        Player player = this.adminPlayer(sender);
        if (player == null) {

            return;

        }

        if (args.length < 2) {

            this.usage(sender, "/spleef create <name> [classic|bow]");
            return;

        }

        GameType gameType;
        try {

            gameType = GameType.parse(args.length >= 3 ? args[2] : "classic");

        } catch (IllegalArgumentException ex) {

            Messages.send(sender, Messages.bad("Mode must be classic or bow."));
            return;

        }

        ArenaManager.CreateResult result = this.arenaManager.create(player, args[1], gameType);
        Messages.send(sender, result.success() ? Messages.good(result.message()) : Messages.bad(result.message()));

    }

    private void delete(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        if (this.arenaManager.delete(arena)) {

            Messages.send(sender, Messages.good("Deleted arena " + arena.name() + "."));

        } else {

            Messages.send(sender, Messages.bad("An active arena cannot be deleted."));

        }

    }

    private void setLocation(CommandSender sender, String[] args, LocationType type) {

        Player player = this.adminPlayer(sender);
        SpleefArena arena = this.adminArena(sender, args, 1);
        if (player == null || arena == null) {

            return;

        }

        if (!this.arenaManager.isSetupLocationValid(arena, player.getLocation())) {

            Messages.send(sender, Messages.bad("Stand inside arena region " + arena.regionId() + "."));
            return;

        }

        switch (type) {

            case WAITING -> arena.waitingSpawn(player.getLocation());
            case SPECTATOR -> arena.spectatorSpawn(player.getLocation());
            case PLAYER -> arena.addSpawn(player.getLocation());

        }

        this.arenaManager.saveArenas();
        Messages.send(sender, Messages.good("Saved the " + type.description + " for " + arena.name() + "."));

    }

    private void clearSpawns(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        ArenaSession session = this.arenaManager.get(arena.name());
        if (session != null && session.isActive()) {

            Messages.send(sender, Messages.bad("Wait for the active match to finish before clearing spawns."));
            return;

        }

        arena.clearSpawns();
        this.arenaManager.saveArenas();
        Messages.send(sender, Messages.good("Cleared player spawns for " + arena.name() + "."));

    }

    private void mode(CommandSender sender, String[] args) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        if (args.length < 3) {

            this.usage(sender, "/spleef mode <arena> <classic|bow>");
            return;

        }

        ArenaSession session = this.arenaManager.get(arena.name());
        if (session != null && session.isActive()) {

            // Switching mode mid-match flips which break path is legal and makes the
            // running game unwinnable.
            Messages.send(sender, Messages.bad("Wait for the active match to finish before changing the mode."));
            return;

        }

        try {

            arena.gameType(GameType.parse(args[2]));
            this.arenaManager.saveArenas();
            Messages.send(sender, Messages.good("Mode set to " + arena.gameType().name() + "."));

        } catch (IllegalArgumentException ex) {

            Messages.send(sender, Messages.bad("Mode must be classic or bow."));

        }

    }

    private void enabled(CommandSender sender, String[] args, boolean enabled) {

        SpleefArena arena = this.adminArena(sender, args, 1);
        if (arena == null) {

            return;

        }

        ArenaSession session = this.arenaManager.get(arena.name());
        if (!enabled && session != null && session.isActive()) {

            Messages.send(sender, Messages.bad("Wait for the active match to finish before disabling this arena."));
            return;

        }

        arena.enabled(enabled);
        this.arenaManager.saveArenas();
        Messages.send(sender,
                Messages.good("Arena " + arena.name() + " is now " + (enabled ? "enabled" : "disabled") + "."));

    }

    private void layer(CommandSender sender, String[] args) {

        if (!this.require(sender, ADMIN_PERMISSION)) {

            return;

        }

        if (args.length < 3) {

            this.usage(sender, "/spleef layer <add|material|remove|list|clear> <arena> [value]");
            return;

        }

        ArenaSession session = this.arenaManager.get(args[2]);
        if (session == null) {

            Messages.send(sender, Messages.bad("Arena not found."));
            return;

        }

        SpleefArena arena = session.arena();
        String action = args[1].toLowerCase(Locale.ROOT);
        if (session.isActive() && !action.equals("list")) {

            // Clearing layers mid-match leaves the dug-out floor with nothing to restore it
            // to, and changing them
            // under a running game silently breaks digging.
            Messages.send(sender, Messages.bad("Wait for the active match to finish before editing layers."));
            return;

        }

        switch (action) {

            case "add" -> {

                Player player = this.player(sender);
                if (player != null) {

                    this.editor.beginLayer(player, arena);

                }

            }
            case "material" -> {

                Player player = this.player(sender);
                if (player == null) {

                    return;

                }

                if (args.length < 4) {

                    this.usage(sender, "/spleef layer material <arena> <block data>");
                    return;

                }

                // Block data never contains spaces, but joining the tail keeps a stray space
                // from being fatal.
                this.editor.applyMaterial(player, arena, String.join("", List.of(args).subList(3, args.length)));

            }
            case "remove" -> {

                if (args.length < 4) {

                    this.usage(sender, "/spleef layer remove <arena> <index>");
                    return;

                }

                try {

                    int index = Integer.parseInt(args[3]) - 1;
                    if (index < 0 || index >= arena.layers().size()) {

                        throw new NumberFormatException();

                    }

                    arena.removeLayer(index);
                    this.arenaManager.saveArenas();
                    Messages.send(sender, Messages.good("Layer removed."));

                } catch (NumberFormatException ex) {

                    Messages.send(sender, Messages.bad("Invalid layer index."));

                }

            }
            case "list" -> {

                if (arena.layers().isEmpty()) {

                    Messages.send(sender, Messages.grey("This arena has no layers."));
                    return;

                }

                int index = 1;
                for (SpleefLayer layer : arena.layers()) {

                    Messages.send(sender,
                            Messages.body().append(Messages.value("#" + index++)).append(Component.text(" "))
                                    .append(Messages.name(layer.blockData().getAsString()))
                                    .append(Component.text(" - " + layer.bounds().minX() + "," + layer.bounds().minY()
                                            + "," + layer.bounds().minZ() + " to " + layer.bounds().maxX() + ","
                                            + layer.bounds().maxY() + "," + layer.bounds().maxZ()))
                                    .build());

                }

            }
            case "clear" -> {

                arena.clearLayers();
                this.arenaManager.saveArenas();
                Messages.send(sender, Messages.good("All layers cleared."));

            }
            default -> this.usage(sender, "/spleef layer <add|material|remove|list|clear> <arena> [value]");

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
        Messages.send(sender,
                Messages.body().append(Messages.value(arena.name())).append(Component.text(" - region "))
                        .append(Messages.name(arena.regionId())).append(Component.text(", world "))
                        .append(Messages.name(arena.worldName())).append(Component.text(", mode "))
                        .append(Messages.name(arena.gameType().name())).append(Component.text(", state "))
                        .append(Messages.name(session == null ? "unknown" : session.state().name())).build());
        Messages.send(sender, Messages.body().append(Component.text("Spawns: "))
                .append(Messages.name(Integer.toString(arena.spawns().size()))).append(Component.text(", layers: "))
                .append(Messages.name(Integer.toString(arena.layers().size()))).append(Component.text(", complete: "))
                .append(arena.isComplete() ? Messages.good("yes") : Messages.bad("no"))
                .append(Component.text(", enabled: "))
                .append(arena.enabled() ? Messages.good("yes") : Messages.bad("no")).build());
        if (!arena.isComplete()) {

            Messages.send(sender, Messages.warn("Still needed: " + missingSteps(arena)));

        }

    }

    private static String missingSteps(SpleefArena arena) {

        List<String> missing = new ArrayList<>();
        if (arena.waitingSpawn() == null) {

            missing.add("setwait");

        }

        if (arena.spectatorSpawn() == null) {

            missing.add("setspectator");

        }

        if (arena.spawns().size() < 2) {

            missing.add("addspawn (at least 2)");

        }

        if (arena.layers().isEmpty()) {

            missing.add("layer add");

        }

        if (arena.deathRegion() == null) {

            missing.add("deathregion");

        }

        return String.join(", ", missing);

    }

    private void cancel(CommandSender sender) {

        Player player = this.player(sender);
        if (player != null) {

            Messages.send(sender, this.editor.cancel(player) ? Messages.grey("Arena edit cancelled.")
                    : Messages.grey("You are not editing an arena."));

        }

    }

    private void reload(CommandSender sender) {

        if (!this.require(sender, ADMIN_PERMISSION)) {

            return;

        }

        this.plugin.reloadConfig();
        ArenaManager.ReloadResult result = this.arenaManager.reloadArenas();
        if (result.success()) {

            this.editor.cancelAll();

        }

        if (!result.success()) {

            Messages.send(sender, Messages.warn("Reloaded config.yml only. These arenas are still in use: "
                    + String.join(", ", result.busyArenas()) + "."));
            return;

        }

        Messages.send(sender,
                Messages.good("Reloaded config.yml and " + result.arenaCount() + " arena(s) from arenas.yml."));

    }

    private SpleefArena adminArena(CommandSender sender, String[] args, int index) {

        if (!this.require(sender, ADMIN_PERMISSION)) {

            return null;

        }

        if (args.length <= index) {

            Messages.send(sender, Messages.bad("An arena name is required."));
            return null;

        }

        ArenaSession session = this.arenaManager.get(args[index]);
        if (session == null) {

            Messages.send(sender, Messages.bad("Arena not found."));
            return null;

        }

        return session.arena();

    }

    private Player adminPlayer(CommandSender sender) {

        return this.require(sender, ADMIN_PERMISSION) ? this.player(sender) : null;

    }

    private Player player(CommandSender sender) {

        if (sender instanceof Player player) {

            return player;

        }

        Messages.send(sender, Messages.bad("That command can only be used by a player."));
        return null;

    }

    private boolean require(CommandSender sender, String permission) {

        if (sender.hasPermission(permission)) {

            return true;

        }

        Messages.send(sender, Messages.bad("You do not have permission to do that."));
        return false;

    }

    private void usage(CommandSender sender, String usage) {

        Messages.send(sender, Messages.body().append(Component.text("Usage: ")).append(Messages.value(usage)).build());

    }

    private void entry(CommandSender sender, String usage, String description) {

        Messages.send(sender,
                Messages.body().append(Messages.value(usage)).append(Component.text(" - " + description)).build());

    }

    private void help(CommandSender sender) {

        Messages.send(sender, Messages.value("Spleef commands"));
        this.entry(sender, "/spleef arenas", "show arenas you can join");
        this.entry(sender, "/spleef join <arena>", "join an arena");
        this.entry(sender, "/spleef leave", "leave your arena or spectator session");
        this.entry(sender, "/spleef spectate <arena>", "watch a match");
        this.entry(sender, "/spleef stats [player]", "show Spleef statistics");
        if (!sender.hasPermission(ADMIN_PERMISSION)) {

            return;

        }

        Messages.send(sender, Messages.value("Arena setup"));
        this.entry(sender, "/spleef create <name> [classic|bow]", "bind a new arena to your WorldGuard region");
        this.entry(sender, "/spleef setwait <arena>", "set the waiting spawn to where you stand");
        this.entry(sender, "/spleef setspectator <arena>", "set the spectator spawn to where you stand");
        this.entry(sender, "/spleef addspawn <arena>", "add a player spawn where you stand (at least 2)");
        this.entry(sender, "/spleef clearspawns <arena>", "remove every player spawn");
        this.entry(sender, "/spleef layer add <arena>", "click two corners to select a floor layer");
        this.entry(sender, "/spleef layer material <arena> <block data>", "finish the selected layer");
        this.entry(sender, "/spleef layer list|remove|clear <arena> [index]", "manage existing layers");
        this.entry(sender, "/spleef deathregion <arena>", "click two corners for the elimination volume");
        this.entry(sender, "/spleef cancel", "abandon the current selection");

        Messages.send(sender, Messages.value("Arena administration"));
        this.entry(sender, "/spleef info <arena>", "show setup progress and runtime state");
        this.entry(sender, "/spleef mode <arena> <classic|bow>", "change the game mode");
        this.entry(sender, "/spleef enable <arena>", "allow players to join");
        this.entry(sender, "/spleef disable <arena>", "stop players joining");
        this.entry(sender, "/spleef delete <arena>", "delete an inactive arena");
        this.entry(sender, "/spleef reload", "reload config.yml and arenas.yml");

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args)
    {

        if (args.length == 1) {

            List<String> commands = new ArrayList<>(List.of("arenas", "join", "leave", "spectate", "stats", "help"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {

                commands.addAll(List.of("create", "delete", "setwait", "setspectator", "addspawn", "clearspawns",
                        "mode", "enable", "disable", "layer", "deathregion", "info", "cancel", "reload"));

            }

            return matches(args[0], commands);

        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        boolean admin = sender.hasPermission(ADMIN_PERMISSION);
        if (args.length == 2 && List.of("join", "spectate", "spec").contains(subcommand)) {

            // Only the arenas a player could actually join, so completion does not
            // advertise disabled or
            // half-configured ones.
            return matches(args[1],
                    this.arenaManager.availableSessions().stream().map(session -> session.arena().name()).toList());

        }

        if (!admin) {

            return List.of();

        }

        if (args.length == 2 && List.of("delete", "setwait", "setspectator", "addspawn", "clearspawns", "mode",
                "enable", "disable", "deathregion", "info").contains(subcommand))
        {

            return matches(args[1], this.arenaNames());

        }

        if (args.length == 2 && subcommand.equals("layer")) {

            return matches(args[1], List.of("add", "material", "remove", "list", "clear"));

        }

        if (args.length == 3 && subcommand.equals("layer")) {

            return matches(args[2], this.arenaNames());

        }

        if (args.length == 4 && subcommand.equals("layer") && args[1].equalsIgnoreCase("material")) {

            return blockDataSuggestions(args[3]);

        }

        if (args.length == 3 && (subcommand.equals("create") || subcommand.equals("mode"))) {

            return matches(args[2], List.of("classic", "bow"));

        }

        return List.of();

    }

    private List<String> arenaNames() {

        return this.arenaManager.sessions().stream().map(session -> session.arena().name()).toList();

    }

    // Suggests placeable block types by namespaced key so a long block-data string
    // does not have to be typed blind.
    private static List<String> blockDataSuggestions(String input) {

        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();
        for (Material material : Registry.MATERIAL) {

            if (keys.size() >= 100) {

                break;

            }

            if (!material.isBlock() || material.isAir()) {

                continue;

            }

            String key = material.getKey().toString();
            if (key.startsWith(prefix) || key.substring(key.indexOf(':') + 1).startsWith(prefix)) {

                keys.add(key);

            }

        }

        return keys;

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
