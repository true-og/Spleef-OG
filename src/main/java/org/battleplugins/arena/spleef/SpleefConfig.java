package org.battleplugins.arena.spleef;

import org.battleplugins.arena.config.ArenaOption;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SpleefConfig {

    @ArenaOption(name = "projectiles-break-blocks", description = "Whether snowballs should break blocks.")
    private boolean projectilesBreakBlocks = false;

    @ArenaOption(name = "shovels", description = "The shovels for this spleef game.", required = true)
    private Map<String, ItemStack> shovels;

    public boolean shouldProjectilesBreakBlocks() {

        return this.projectilesBreakBlocks;

    }

    @Nullable
    public ItemStack getShovel(String name) {

        return this.shovels == null ? null : this.shovels.get(name).clone();

    }

}
