package org.battleplugins.arena.spleef;

import org.battleplugins.arena.command.ArenaCommand;
import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.command.Argument;
import org.battleplugins.arena.competition.map.CompetitionMap;
import org.battleplugins.arena.competition.map.options.Bounds;
import org.battleplugins.arena.spleef.arena.SpleefArena;
import org.battleplugins.arena.spleef.arena.SpleefMap;
import org.battleplugins.arena.spleef.editor.SpleefEditorWizards;
import org.bukkit.entity.Player;

public class SpleefExecutor extends ArenaCommandExecutor {

    public SpleefExecutor(SpleefArena arena) {

        super(arena);

    }

    @ArenaCommand(commands = "layer", subCommands = "add", description = "Adds a layer to a spleef arena.", permissionNode = "layer.add")
    public void layer(Player player, CompetitionMap map) {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        SpleefEditorWizards.ADD_LAYER.openWizard(player, this.arena, ctx -> ctx.setMap(spleefMap));

    }

    @ArenaCommand(commands = "layer", subCommands = "remove", description = "Removes a layer from a spleef arena.", permissionNode = "layer.remove")
    public void removeLayer(Player player, CompetitionMap map, @Argument(name = "index") int index) {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        index--;

        if (index < 0 || index >= spleefMap.getLayers().size()) {

            SpleefMessages.INVALID_LAYER.send(player, Integer.toString(spleefMap.getLayers().size()));
            return;

        }

        spleefMap.removeLayer(spleefMap.getLayers().get(index));
        SpleefMessages.LAYER_REMOVED.send(player);

    }

    @ArenaCommand(commands = "layer", subCommands = "clear", description = "Clears all layers from a spleef arena.", permissionNode = "layer.clear")
    public void clearLayer(Player player, CompetitionMap map) {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        spleefMap.clearLayers();
        SpleefMessages.LAYER_REMOVED_ALL.send(player);

    }

    @ArenaCommand(commands = "layer", subCommands = "index", description = "Changes the index of a layer.", permissionNode = "layer.index")
    public void indexLayer(Player player, CompetitionMap map, @Argument(name = "from") int from,
            @Argument(name = "to") int to)
    {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        from--;
        to--;

        if (spleefMap.getLayers().isEmpty()) {

            SpleefMessages.NO_LAYERS.send(player);
            return;

        }

        if (from < 0 || from >= spleefMap.getLayers().size() || to < 0 || to >= spleefMap.getLayers().size()) {

            SpleefMessages.INVALID_LAYER.send(player, Integer.toString(spleefMap.getLayers().size()));
            return;

        }

        SpleefMap.Layer layer = spleefMap.getLayers().get(from);
        spleefMap.removeLayer(layer);
        spleefMap.addLayer(to, layer);

        SpleefMessages.LAYER_INDEX_CHANGED.send(player);

    }

    @ArenaCommand(commands = "layer", subCommands = "list", description = "Lists all layers in a spleef arena.", permissionNode = "layer.list")
    public void listLayers(Player player, CompetitionMap map) {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        if (spleefMap.getLayers().isEmpty()) {

            SpleefMessages.NO_LAYERS.send(player);
            return;

        }

        this.sendHeader(player);
        for (int i = 0; i < spleefMap.getLayers().size(); i++) {

            SpleefMap.Layer layer = spleefMap.getLayers().get(i);
            Bounds bounds = layer.getBounds();
            String minBounds = bounds.getMinX() + ", " + bounds.getMinY() + ", " + bounds.getMinZ();
            String maxBounds = bounds.getMaxX() + ", " + bounds.getMaxY() + ", " + bounds.getMaxZ();

            SpleefMessages.LAYER_INFO.send(player, Integer.toString(i + 1), minBounds, maxBounds,
                    layer.getBlockData().getAsString());

        }

    }

    @ArenaCommand(commands = "deathregion", description = "Sets the death region for a spleef arena.", permissionNode = "deathregion")
    public void setDeathRegion(Player player, CompetitionMap map) {

        if (!(map instanceof SpleefMap spleefMap)) {

            return; // Should not happen but just incase

        }

        SpleefEditorWizards.DEATH_REGION.openWizard(player, this.arena, ctx -> ctx.setMap(spleefMap));

    }

}
