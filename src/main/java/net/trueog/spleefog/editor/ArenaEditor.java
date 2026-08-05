package net.trueog.spleefog.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.trueog.spleefog.Messages;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Two-click region selection for arena layers and death regions.
//
// A layer's block data is taken as a command argument rather than a chat prompt. Registering a listener for the
// legacy chat event makes the server route every player's chat down its legacy path, so a setup wizard used a
// handful of times must not cost the whole server chat fidelity. A command also arrives on the main thread already
// and can be tab-completed.
public final class ArenaEditor implements Listener {

    private final ArenaManager arenaManager;
    private final Map<UUID, EditSession> edits = new HashMap<>();

    public ArenaEditor(ArenaManager arenaManager) {

        this.arenaManager = arenaManager;

    }

    public boolean beginLayer(Player player, SpleefArena arena) {

        if (!this.canEdit(player, arena)) {

            return false;

        }

        this.edits.put(player.getUniqueId(), new EditSession(arena, EditType.LAYER));
        Messages.send(player, Messages.body().append(Component.text("Click the first corner block of the layer, or "))
                .append(Messages.value("/spleef cancel")).append(Component.text(".")).build());
        return true;

    }

    public boolean beginDeathRegion(Player player, SpleefArena arena) {

        if (!this.canEdit(player, arena)) {

            return false;

        }

        this.edits.put(player.getUniqueId(), new EditSession(arena, EditType.DEATH_REGION));
        Messages.send(player,
                Messages.body().append(Component.text("Click the first corner block of the death region, or "))
                        .append(Messages.value("/spleef cancel")).append(Component.text(".")).build());
        return true;

    }

    public boolean cancel(Player player) {

        return this.edits.remove(player.getUniqueId()) != null;

    }

    // Dropped whenever arenas are re-read, because a pending selection points at an
    // arena object that no longer
    // exists and writing to it would report success while saving nothing.
    public void cancelAll() {

        for (UUID playerId : this.edits.keySet()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                Messages.send(player, Messages.warn("Your arena selection was cancelled because arenas reloaded."));

            }

        }

        this.edits.clear();

    }

    // Completes a layer selection with the block data the admin supplied on the
    // command line.
    public void applyMaterial(Player player, SpleefArena arena, String blockDataText) {

        EditSession edit = this.edits.get(player.getUniqueId());
        if (edit == null || edit.type != EditType.LAYER || edit.first == null || edit.second == null) {

            Messages.send(player, Messages.body().append(Component.text("Select both corners first with "))
                    .append(Messages.value("/spleef layer add " + arena.name())).append(Component.text(".")).build());
            return;

        }

        if (edit.arena != arena) {

            Messages.send(player, Messages.bad("Your pending selection belongs to arena " + edit.arena.name() + "."));
            return;

        }

        BlockData blockData;
        try {

            blockData = Bukkit.createBlockData(blockDataText);

        } catch (IllegalArgumentException ex) {

            Messages.send(player, Messages.bad("'" + blockDataText + "' is not valid block data."));
            return;

        }

        arena.addLayer(new SpleefLayer(new BlockBounds(edit.first, edit.second), blockData));
        this.arenaManager.saveArenas();
        this.edits.remove(player.getUniqueId());
        Messages.send(player, Messages.body().append(Component.text("Layer added to "))
                .append(Messages.value(arena.name())).append(Component.text(".")).build());

    }

    private boolean canEdit(Player player, SpleefArena arena) {

        ArenaSession session = this.arenaManager.get(arena.name());
        if (session == null || session.state() != ArenaState.WAITING || session.isActive()) {

            Messages.send(player, Messages.bad("That arena cannot be edited while it is active."));
            return false;

        }

        return true;

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        EditSession edit = this.edits.get(event.getPlayer().getUniqueId());
        if (edit == null || event.getClickedBlock() == null
                || event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
        {

            return;

        }

        event.setCancelled(true);
        Location location = event.getClickedBlock().getLocation();
        if (!this.arenaManager.isSetupLocationValid(edit.arena, location)) {

            Messages.send(event.getPlayer(),
                    Messages.bad("Both corners must be inside WorldGuard region " + edit.arena.regionId() + "."));
            return;

        }

        if (edit.first == null || edit.second != null) {

            // A further click restarts the selection, which is friendlier than silently
            // ignoring it.
            edit.first = location;
            edit.second = null;
            Messages.send(event.getPlayer(), Messages.grey("First corner set. Click the second corner block."));
            return;

        }

        edit.second = location;
        if (edit.type == EditType.DEATH_REGION) {

            edit.arena.deathRegion(new BlockBounds(edit.first, edit.second));
            this.arenaManager.saveArenas();
            this.edits.remove(event.getPlayer().getUniqueId());
            Messages.send(event.getPlayer(), Messages.good("Death region saved."));
            return;

        }

        String command = "/spleef layer material " + edit.arena.name() + " ";
        Messages.send(event.getPlayer(), Messages.body().append(Component.text("Second corner set. Now run "))
                .append(Messages.value(command.trim() + " <block data>").clickEvent(ClickEvent.suggestCommand(command))
                        .hoverEvent(HoverEvent.showText(Messages.grey("Click to fill in the command"))))
                .append(Component.text(", for example ")).append(Messages.name("minecraft:snow_block"))
                .append(Component.text(".")).build());

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

        private EditSession(SpleefArena arena, EditType type) {

            this.arena = arena;
            this.type = type;

        }

    }

}
