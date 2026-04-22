package org.battleplugins.arena.spleef.action;

import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.event.action.EventAction;
import org.battleplugins.arena.resolver.Resolvable;
import org.battleplugins.arena.spleef.ArenaSpleef;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class GiveShovelAction extends EventAction {

    private static final String SHOVEL_KEY = "shovel";

    public GiveShovelAction(Map<String, String> params) {

        super(params, SHOVEL_KEY);

    }

    @Override
    public void call(ArenaPlayer arenaPlayer, Resolvable resolvable) {

        ItemStack shovel = ArenaSpleef.getInstance().getMainConfig().getShovel(this.get(SHOVEL_KEY));
        if (shovel == null) {

            ArenaSpleef.getInstance().getSLF4JLogger()
                    .warn("Invalid shovel " + this.get(SHOVEL_KEY) + ". Not giving shovel to player.");
            return;

        }

        shovel.editMeta(meta -> meta.getPersistentDataContainer().set(ArenaSpleef.getInstance().getSpleefItemKey(),
                PersistentDataType.BOOLEAN, true));
        arenaPlayer.getPlayer().getInventory().addItem(shovel);

    }

}
