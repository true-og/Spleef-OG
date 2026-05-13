package org.battleplugins.arena.spleef;

import org.battleplugins.arena.config.ArenaOption;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpleefConfig {

    @ArenaOption(name = "projectiles-break-blocks", description = "Whether snowballs should break blocks.")
    private boolean projectilesBreakBlocks = false;

    @ArenaOption(name = "shovels", description = "The shovels for this spleef game.", required = true)
    private Map<String, ItemStack> shovels;

    @ArenaOption(name = "world-whitelist", description = "Worlds where Spleef arenas may be created and run. Empty list = all worlds allowed.")
    private List<String> worldWhitelist;

    public boolean shouldProjectilesBreakBlocks() {

        return this.projectilesBreakBlocks;

    }

    @Nullable
    public ItemStack getShovel(String name) {

        if (this.shovels == null) {

            return null;

        }

        ItemStack shovel = this.shovels.get(name);
        return shovel == null ? null : shovel.clone();

    }

    public boolean isWorldAllowed(String worldName) {

        if (this.worldWhitelist == null || this.worldWhitelist.isEmpty()) {

            return true;

        }

        if (worldName == null) {

            return false;

        }

        String target = worldName.toLowerCase(Locale.ROOT);
        for (String allowed : this.worldWhitelist) {

            if (allowed != null && allowed.toLowerCase(Locale.ROOT).equals(target)) {

                return true;

            }

        }

        return false;

    }

    public List<String> getWorldWhitelist() {

        return this.worldWhitelist == null ? List.of() : List.copyOf(this.worldWhitelist);

    }

}
