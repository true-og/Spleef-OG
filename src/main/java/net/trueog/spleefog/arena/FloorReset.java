package net.trueog.spleefog.arena;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.List;
import net.trueog.spleefog.model.SpleefArena;
import net.trueog.spleefog.model.SpleefLayer;
import org.bukkit.World;
import org.bukkit.block.Block;

// Restores an arena floor a bounded number of blocks at a time.
//
// The whole job is resolved up front: the region is looked up once instead of once per block, bounds are clamped to
// the world's build limits, and the cursor walks a chunk at a time so consecutive writes stay in the same chunk.
// Chunks that are not currently loaded are loaded deliberately rather than incidentally, and only a few per pass,
// because a floor left with holes in an unloaded chunk would still be broken the next time a player walked into it.
final class FloorReset {

    private static final int MAX_CHUNK_LOADS_PER_PASS = 4;

    private final World world;
    private final ProtectedRegion region;
    private final List<Section> sections = new ArrayList<>();
    private int sectionIndex;
    private int cursorX;
    private int cursorY;
    private int cursorZ;
    private boolean started;

    FloorReset(World world, ProtectedRegion region, SpleefArena arena) {

        this.world = world;
        this.region = region;
        int floor = world.getMinHeight();
        int ceiling = world.getMaxHeight() - 1;
        for (SpleefLayer layer : arena.layers()) {

            int minY = Math.max(floor, layer.bounds().minY());
            int maxY = Math.min(ceiling, layer.bounds().maxY());
            if (minY > maxY) {

                // Entirely outside this world's build range, so there is nothing that could be
                // written.
                continue;

            }

            for (int chunkX = layer.bounds().minX() >> 4; chunkX <= layer.bounds().maxX() >> 4; chunkX++) {

                for (int chunkZ = layer.bounds().minZ() >> 4; chunkZ <= layer.bounds().maxZ() >> 4; chunkZ++) {

                    this.sections.add(new Section(layer, chunkX, chunkZ, Math.max(layer.bounds().minX(), chunkX << 4),
                            Math.min(layer.bounds().maxX(), (chunkX << 4) + 15), minY, maxY,
                            Math.max(layer.bounds().minZ(), chunkZ << 4),
                            Math.min(layer.bounds().maxZ(), (chunkZ << 4) + 15)));

                }

            }

        }

    }

    boolean isDone() {

        return this.sectionIndex >= this.sections.size();

    }

    // Writes up to budget blocks and reports whether the job finished.
    boolean run(int budget) {

        int written = 0;
        int loads = 0;
        while (this.sectionIndex < this.sections.size() && written < budget) {

            Section section = this.sections.get(this.sectionIndex);
            if (!this.started) {

                if (!this.world.isChunkLoaded(section.chunkX, section.chunkZ)) {

                    // Loading a chunk is far more expensive than writing blocks, so only a few are
                    // pulled in per
                    // pass. The rest of the floor continues on the following ticks.
                    if (loads >= MAX_CHUNK_LOADS_PER_PASS) {

                        return false;

                    }

                    loads++;

                }

                this.cursorX = section.minX;
                this.cursorY = section.minY;
                this.cursorZ = section.minZ;
                this.started = true;
                this.world.getChunkAt(section.chunkX, section.chunkZ);

            }

            written += this.fill(section, budget - written);
            if (this.cursorX > section.maxX) {

                this.sectionIndex++;
                this.started = false;

            }

        }

        return this.isDone();

    }

    private int fill(Section section, int budget) {

        int written = 0;
        while (this.cursorX <= section.maxX && written < budget) {

            if (this.region.contains(this.cursorX, this.cursorY, this.cursorZ)) {

                Block block = this.world.getBlockAt(this.cursorX, this.cursorY, this.cursorZ);
                if (!block.getBlockData().matches(section.layer.blockData())) {

                    // Skipping blocks that already match keeps an untouched floor from re-sending
                    // the whole volume.
                    block.setBlockData(section.layer.blockData(), false);

                }

            }

            written++;
            this.cursorZ++;
            if (this.cursorZ > section.maxZ) {

                this.cursorZ = section.minZ;
                this.cursorY++;

            }

            if (this.cursorY > section.maxY) {

                this.cursorY = section.minY;
                this.cursorX++;

            }

        }

        return written;

    }

    private record Section(SpleefLayer layer, int chunkX, int chunkZ, int minX, int maxX, int minY, int maxY, int minZ,
            int maxZ)
    {
    }

}
