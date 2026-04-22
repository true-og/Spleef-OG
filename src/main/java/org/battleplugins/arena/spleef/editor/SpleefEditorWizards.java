package org.battleplugins.arena.spleef.editor;

import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.competition.map.options.Bounds;
import org.battleplugins.arena.config.ParseException;
import org.battleplugins.arena.editor.ArenaEditorWizard;
import org.battleplugins.arena.editor.ArenaEditorWizards;
import org.battleplugins.arena.editor.stage.PositionInputStage;
import org.battleplugins.arena.editor.stage.TextInputStage;
import org.battleplugins.arena.editor.type.MapOption;
import org.battleplugins.arena.messages.Messages;
import org.battleplugins.arena.spleef.SpleefMessages;
import org.battleplugins.arena.spleef.arena.SpleefMap;
import org.bukkit.Bukkit;

import java.io.IOException;

public final class SpleefEditorWizards {

    public static final ArenaEditorWizard<LayerContext> ADD_LAYER = ArenaEditorWizards.createWizard(LayerContext::new)
            .addStage(MapOption.MIN_POS,
                    new PositionInputStage<>(SpleefMessages.LAYER_SET_MIN_POSITION, ctx -> ctx::setMin))
            .addStage(MapOption.MAX_POS,
                    new PositionInputStage<>(SpleefMessages.LAYER_SET_MAX_POSITION, ctx -> ctx::setMax))
            .addStage(LayerOption.BLOCK_DATA, new TextInputStage<>(SpleefMessages.LAYER_SET_BLOCK_DATA,
                    SpleefMessages.LAYER_SET_BLOCK_DATA_INVALID, (ctx, str) ->
                    {

                        try {

                            Bukkit.createBlockData(str);

                        } catch (IllegalArgumentException e) {

                            return false;

                        }

                        return true;

                    }, ctx -> str -> ctx.setBlockData(Bukkit.createBlockData(str)))).onCreationComplete(ctx -> {

                        Bounds bounds = new Bounds(ctx.getMin(), ctx.getMax());

                        SpleefMap map = ctx.getMap();
                        SpleefMap.Layer layer = new SpleefMap.Layer(bounds, ctx.getBlockData());
                        map.addLayer(layer);

                        try {

                            map.save();

                        } catch (ParseException | IOException e) {

                            BattleArena.getInstance().error("Failed to save map file for arena {}",
                                    ctx.getArena().getName(), e);
                            Messages.MAP_FAILED_TO_SAVE.send(ctx.getPlayer(), map.getName());
                            return;

                        }

                        SpleefMessages.LAYER_ADDED.send(ctx.getPlayer());

                    });

    public static final ArenaEditorWizard<DeathRegionContext> DEATH_REGION = ArenaEditorWizards
            .createWizard(DeathRegionContext::new)
            .addStage(MapOption.MIN_POS,
                    new PositionInputStage<>(SpleefMessages.DEATH_REGION_SET_MIN_POSITION, ctx -> ctx::setMin))
            .addStage(MapOption.MAX_POS,
                    new PositionInputStage<>(SpleefMessages.DEATH_REGION_SET_MAX_POSITION, ctx -> ctx::setMax))
            .onCreationComplete(ctx ->
            {

                Bounds bounds = new Bounds(ctx.getMin(), ctx.getMax());

                SpleefMap map = ctx.getMap();
                map.setDeathRegion(bounds);

                try {

                    map.save();

                } catch (ParseException | IOException e) {

                    BattleArena.getInstance().error("Failed to save map file for arena {}", ctx.getArena().getName(),
                            e);
                    Messages.MAP_FAILED_TO_SAVE.send(ctx.getPlayer(), map.getName());
                    return;

                }

                SpleefMessages.DEATH_REGION_SET.send(ctx.getPlayer());

            });

}
