package net.trueog.spleefog.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.SpleefPlugin;
import net.trueog.spleefog.arena.ArenaManager;
import net.trueog.spleefog.arena.ArenaSession;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.BlockBounds;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ArenaEditor implements Listener {

    private final SpleefPlugin plugin;
    private final ArenaManager arenaManager;
    private final Map<UUID, EditSession> edits = new HashMap<>();

    public ArenaEditor(SpleefPlugin plugin, ArenaManager arenaManager) {

        this.plugin = plugin;
        this.arenaManager = arenaManager;

    }

    public boolean beginLayer(Player player, SpleefArena arena) {

        if (!this.canEdit(player, arena)) {

            return false;

        }

        this.edits.put(player.getUniqueId(), new EditSession(arena, EditType.LAYER));
        Messages.send(player, "&7Click the first corner block of the layer, or type &bcancel&7.");
        return true;

    }

    public boolean beginDeathRegion(Player player, SpleefArena arena) {

        if (!this.canEdit(player, arena)) {

            return false;

        }

        this.edits.put(player.getUniqueId(), new EditSession(arena, EditType.DEATH_REGION));
        Messages.send(player, "&7Click the first corner block of the death region, or type &bcancel&7.");
        return true;

    }

    public boolean cancel(Player player) {

        return this.edits.remove(player.getUniqueId()) != null;

    }

    private boolean canEdit(Player player, SpleefArena arena) {

        ArenaSession session = this.arenaManager.get(arena.name());
        if (session == null || session.state() != ArenaState.WAITING || session.isActive()) {

            Messages.send(player, "&cThat arena cannot be edited while it is active.");
            return false;

        }

        return true;

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        EditSession edit = this.edits.get(event.getPlayer().getUniqueId());
        if (edit == null || edit.awaitingMaterial || event.getClickedBlock() == null
                || event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
        {

            return;

        }

        event.setCancelled(true);
        Location location = event.getClickedBlock().getLocation();
        if (!this.arenaManager.isSetupLocationValid(edit.arena, location)) {

            Messages.send(event.getPlayer(),
                    "&cBoth corners must be inside WorldGuard region &b" + edit.arena.regionId() + "&c.");
            return;

        }

        if (edit.first == null) {

            edit.first = location;
            Messages.send(event.getPlayer(), "&7First corner set. Click the second corner block.");
            return;

        }

        edit.second = location;
        if (edit.type == EditType.DEATH_REGION) {

            edit.arena.deathRegion(new BlockBounds(edit.first, edit.second));
            this.arenaManager.saveArenas();
            this.edits.remove(event.getPlayer().getUniqueId());
            Messages.send(event.getPlayer(), "&aDeath region saved.");

        } else {

            edit.awaitingMaterial = true;
            Messages.send(event.getPlayer(),
                    "&7Enter the Bukkit block data for this layer, for example &bminecraft:snow_block&7.");

        }

    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {

        EditSession edit = this.edits.get(event.getPlayer().getUniqueId());
        if (edit == null) {

            return;

        }

        event.setCancelled(true);
        String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(this.plugin, () -> this.handleChat(event.getPlayer(), edit, input));

    }

    private void handleChat(Player player, EditSession edit, String input) {

        if (input.equalsIgnoreCase("cancel")) {

            this.edits.remove(player.getUniqueId());
            Messages.send(player, "&7Arena edit cancelled.");
            return;

        }

        if (!edit.awaitingMaterial) {

            Messages.send(player, "&7Click the requested block, or type &bcancel&7.");
            return;

        }

        BlockData blockData;
        try {

            blockData = Bukkit.createBlockData(input);

        } catch (IllegalArgumentException ex) {

            Messages.send(player, "&cInvalid block data. Try again or type &bcancel&c.");
            return;

        }

        edit.arena.addLayer(new SpleefLayer(new BlockBounds(edit.first, edit.second), blockData));
        this.arenaManager.saveArenas();
        this.edits.remove(player.getUniqueId());
        Messages.send(player, "&aLayer added to &b" + edit.arena.name() + "&a.");

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        this.edits.remove(event.getPlayer().getUniqueId());

    }

    private enum EditType {
        LAYER, DEATH_REGION
    }

    private static final class EditSession {

        private final SpleefArena arena;
        private final EditType type;
        private Location first;
        private Location second;
        private boolean awaitingMaterial;

        private EditSession(SpleefArena arena, EditType type) {

            this.arena = arena;
            this.type = type;

        }

    }

}
