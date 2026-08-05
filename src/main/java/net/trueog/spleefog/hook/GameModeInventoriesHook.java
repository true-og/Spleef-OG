package net.trueog.spleefog.hook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

// Holds GameModeInventories-OG off a player for as long as Spleef owns their inventory and gamemode.
// Three separate permissions matter. gamemodeinventories.use drives the per-gamemode inventory swap, and leaving it on
// would file a Spleef kit away as the player's real adventure-mode inventory. gamemodeinventories.death drives a
// restore one tick after respawn that would overwrite the kit outright. gamemodeinventories.spectator is the supported
// bypass for the spectator restriction; without it an eliminated player's switch into spectator is cancelled before it
// happens.
public final class GameModeInventoriesHook {

    private static final String PLUGIN_NAME = "GameModeInventories-OG";
    private static final String USE_PERMISSION = "gamemodeinventories.use";
    private static final String DEATH_PERMISSION = "gamemodeinventories.death";
    private static final String SPECTATOR_PERMISSION = "gamemodeinventories.spectator";
    private final SpleefPlugin owner;
    private final boolean enabled;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public GameModeInventoriesHook(SpleefPlugin owner) {

        this.owner = owner;
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        this.enabled = plugin != null && plugin.isEnabled();

    }

    public boolean isEnabled() {

        return this.enabled;

    }

    public void suspend(Player player) {

        if (!this.enabled || this.attachments.containsKey(player.getUniqueId())) {

            return;

        }

        PermissionAttachment attachment = player.addAttachment(this.owner);
        attachment.setPermission(USE_PERMISSION, false);
        attachment.setPermission(DEATH_PERMISSION, false);
        attachment.setPermission(SPECTATOR_PERMISSION, true);
        this.attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();

    }

    public void release(Player player) {

        PermissionAttachment attachment = this.attachments.remove(player.getUniqueId());
        if (attachment == null) {

            return;

        }

        try {

            attachment.remove();

        } catch (IllegalArgumentException ignored) {

            // The attachment was already dropped, most likely with the player's
            // permissible.

        }

    }

    public void releaseAll() {

        for (PermissionAttachment attachment : this.attachments.values()) {

            try {

                attachment.remove();

            } catch (IllegalArgumentException ignored) {

                // The attachment was already dropped, most likely with the player's
                // permissible.

            }

        }

        this.attachments.clear();

    }

}
