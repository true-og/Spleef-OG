package org.battleplugins.arena.spleef.arena;

import io.papermc.paper.math.Position;
import org.battleplugins.arena.competition.CompetitionType;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.spleef.ArenaSpleef;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpleefCompetition extends LiveCompetition<SpleefCompetition> {

    private final SpleefArena arena;
    private final SpleefMap map;

    private final List<BukkitTask> decayTasks = new ArrayList<>();
    private final List<Position> pendingDecays = new ArrayList<>();

    public SpleefCompetition(SpleefArena arena, CompetitionType type, SpleefMap map) {

        super(arena, type, map);

        this.arena = arena;
        this.map = map;

    }

    public void beginLayerDecay() {

        List<SpleefMap.Layer> layers = new ArrayList<>(this.map.getLayers());
        layers.sort((layer1, layer2) -> layer2.getBounds().getMaxY() - layer1.getBounds().getMaxY());

        for (int i = 0; i < layers.size(); i++) {

            SpleefMap.Layer layer = layers.get(i);

            long delay = (i + 1) * this.arena.getLayerDecayDelay().toSeconds() * 20;
            BukkitTask decayTask = Bukkit.getScheduler().runTaskLater(ArenaSpleef.getInstance(), () -> {

                Duration layerDecayTime = this.arena.getLayerDecayTime();
                List<Block> blocks = new ArrayList<>(layer.getBounds().getVolume());

                for (int x = layer.getBounds().getMinX(); x <= layer.getBounds().getMaxX(); x++) {

                    for (int y = layer.getBounds().getMinY(); y <= layer.getBounds().getMaxY(); y++) {

                        for (int z = layer.getBounds().getMinZ(); z <= layer.getBounds().getMaxZ(); z++) {

                            blocks.add(this.map.getWorld().getBlockAt(x, y, z));

                        }

                    }

                }

                if (blocks.isEmpty()) {

                    return;

                }

                Collections.shuffle(blocks);

                long totalDecayTimeTicks = layerDecayTime.getSeconds() * 20;
                long intervalBetweenDecayTicks = Math.max(1L, totalDecayTimeTicks / blocks.size());

                this.decayTasks.add(new BukkitRunnable() {

                    private int index = 0;

                    @Override
                    public void run() {

                        if (index >= blocks.size()) {

                            this.cancel();
                            return;

                        }

                        Block block = blocks.get(index);
                        block.setType(Material.AIR);

                        this.index++;

                    }

                }.runTaskTimer(ArenaSpleef.getInstance(), 0, intervalBetweenDecayTicks));

            }, delay);

            this.decayTasks.add(decayTask);

        }

    }

    public void stopLayerDecay() {

        for (BukkitTask decayTask : this.decayTasks) {

            decayTask.cancel();

        }

        this.decayTasks.clear();

    }

    public void pasteLayers() {

        List<SpleefMap.Layer> layers = this.map.getLayers();
        for (SpleefMap.Layer layer : layers) {

            for (int x = layer.getBounds().getMinX(); x <= layer.getBounds().getMaxX(); x++) {

                for (int y = layer.getBounds().getMinY(); y <= layer.getBounds().getMaxY(); y++) {

                    for (int z = layer.getBounds().getMinZ(); z <= layer.getBounds().getMaxZ(); z++) {

                        Block block = this.map.getWorld().getBlockAt(x, y, z);
                        block.setBlockData(layer.getBlockData());

                    }

                }

            }

        }

    }

    public void decayBlock(Position position) {

        if (this.pendingDecays.contains(position)) {

            return;

        }

        this.pendingDecays.add(position);

        Bukkit.getScheduler().runTaskLater(ArenaSpleef.getInstance(), () -> {

            Block block = this.map.getWorld().getBlockAt(position.blockX(), position.blockY(), position.blockZ());
            block.setType(Material.AIR);

            this.pendingDecays.remove(position);

        }, 5);

    }

}
