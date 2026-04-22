package org.battleplugins.arena.spleef;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;

public final class SpleefUtil {

    public static Block getBlockUnderPlayer(int y, Location location) {

        Location loc = location.clone();
        loc.setY(y);

        double boxWidth = 0.3;
        Block block1 = getBlock(loc, +boxWidth, -boxWidth);
        if (!block1.getType().isAir()) {

            return block1;

        }

        Block block2 = getBlock(loc, -boxWidth, +boxWidth);
        if (!block2.getType().isAir()) {

            return block2;

        }

        Block block3 = getBlock(loc, +boxWidth, +boxWidth);
        if (!block3.getType().isAir()) {

            return block3;

        }

        Block block4 = getBlock(loc, -boxWidth, -boxWidth);
        if (!block4.getType().isAir()) {

            return block4;

        }

        return null;

    }

    private static Block getBlock(Location loc, double x, double z) {

        return loc.getWorld().getBlockAt(NumberConversions.floor(loc.getX() + x), loc.getBlockY(),
                NumberConversions.floor(loc.getZ() + z));

    }

}
