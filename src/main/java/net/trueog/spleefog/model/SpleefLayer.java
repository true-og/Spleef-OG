package net.trueog.spleefog.model;

import org.bukkit.block.data.BlockData;

public record SpleefLayer(BlockBounds bounds, BlockData blockData) {
}
