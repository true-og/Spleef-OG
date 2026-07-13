package net.trueog.spleefog.hook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.trueog.spleefog.SpleefPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

public final class GameModeInventoriesHook {

    private static final String PLUGIN_NAME = "GameModeInventories-OG";
    private static final String USE_PERMISSION = "gamemodeinventories.use";
    private final SpleefPlugin owner;
    private final boolean enabled;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public GameModeInventoriesHook(SpleefPlugin owner) {

        this.owner = owner;
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        this.enabled = plugin != null && plugin.isEnabled();

    }

    public void suspend(Player player) {

        if (this.enabled) {

            this.attachments.computeIfAbsent(player.getUniqueId(),
                    ignored -> player.addAttachment(this.owner, USE_PERMISSION, false));

        }

    }

    public void release(Player player) {

        PermissionAttachment attachment = this.attachments.remove(player.getUniqueId());
        if (attachment == null) {

            return;

        }

        try {

            attachment.remove();

        } catch (IllegalArgumentException ignored) {

        }

    }

    public void releaseAll() {

        for (PermissionAttachment attachment : this.attachments.values()) {

            try {

                attachment.remove();

            } catch (IllegalArgumentException ignored) {

            }

        }

        this.attachments.clear();

    }

}
