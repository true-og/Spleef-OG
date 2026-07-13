package net.trueog.spleefog.model;

import org.bukkit.Location;

public record BlockBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public BlockBounds(Location first, Location second) {

        this(Math.min(first.getBlockX(), second.getBlockX()), Math.min(first.getBlockY(), second.getBlockY()),
                Math.min(first.getBlockZ(), second.getBlockZ()), Math.max(first.getBlockX(), second.getBlockX()),
                Math.max(first.getBlockY(), second.getBlockY()), Math.max(first.getBlockZ(), second.getBlockZ()));

    }

    public boolean contains(Location location) {

        return location != null && location.getBlockX() >= this.minX && location.getBlockX() <= this.maxX
                && location.getBlockY() >= this.minY && location.getBlockY() <= this.maxY
                && location.getBlockZ() >= this.minZ && location.getBlockZ() <= this.maxZ;

    }

    public long volume() {

        return (long) (this.maxX - this.minX + 1) * (this.maxY - this.minY + 1) * (this.maxZ - this.minZ + 1);

    }

}
