package org.battleplugins.arena.spleef.arena;

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import io.papermc.paper.math.BlockPosition;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.competition.map.MapFactory;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.battleplugins.arena.config.ArenaOption;
import org.battleplugins.arena.event.ArenaEventHandler;
import org.battleplugins.arena.event.arena.ArenaPhaseCompleteEvent;
import org.battleplugins.arena.event.arena.ArenaPhaseStartEvent;
import org.battleplugins.arena.options.ArenaOptionType;
import org.battleplugins.arena.options.types.BooleanArenaOption;
import org.battleplugins.arena.spleef.ArenaSpleef;
import org.battleplugins.arena.spleef.SpleefEventResolvers;
import org.battleplugins.arena.spleef.SpleefExecutor;
import org.battleplugins.arena.spleef.SpleefUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;

public class SpleefArena extends Arena {

    @ArenaOption(name = "layer-decay-delay", description = "The delay before each layer decays.")
    private Duration layerDecayDelay = Duration.ofMinutes(2);

    @ArenaOption(name = "layer-decay-time", description = "The time it takes to decay and entire layer.")
    private Duration layerDecayTime = Duration.ofMinutes(1);

    @ArenaOption(name = "game", description = "The spleef game.")
    private SpleefGame game = SpleefGame.CLASSIC;

    public SpleefArena() {

        super();

        this.getEventManager().registerArenaResolver(ProjectileHitEvent.class, SpleefEventResolvers.PROJECTILE_HIT);
        this.getEventManager().registerArenaResolver(ThrownEggHatchEvent.class, SpleefEventResolvers.THROWN_EGG_HATCH);
        this.getEventManager().registerArenaResolver(TNTPrimeEvent.class, SpleefEventResolvers.TNT_PRIME.apply(this));

    }

    @Override
    public ArenaCommandExecutor createCommandExecutor() {

        return new SpleefExecutor(this);

    }

    @Override
    public MapFactory getMapFactory() {

        return SpleefMap.FACTORY;

    }

    @ArenaEventHandler
    public void onPhaseStart(ArenaPhaseStartEvent event) {

        if (!CompetitionPhaseType.INGAME.equals(event.getPhase().getType())) {

            return;

        }

        if (event.getCompetition() instanceof SpleefCompetition spleefCompetition) {

            spleefCompetition.beginLayerDecay();

        }

    }

    @ArenaEventHandler
    public void onPhaseComplete(ArenaPhaseCompleteEvent event) {

        if (!CompetitionPhaseType.INGAME.equals(event.getPhase().getType())) {

            return;

        }

        if (event.getCompetition() instanceof SpleefCompetition spleefCompetition) {

            spleefCompetition.stopLayerDecay();

        }

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event, ArenaPlayer player) {

        // Ensure that the game is the classic spleef game
        if (this.game != SpleefGame.CLASSIC) {

            return;

        }

        if (player.getCompetition().option(ArenaOptionType.BLOCK_BREAK).map(BooleanArenaOption::isEnabled)
                .orElse(true))
        {

            return; // Game already allows block breaking

        }

        SpleefMap spleefMap = (SpleefMap) player.getCompetition().getMap();
        BlockPosition pos = event.getBlock().getLocation().toBlock();
        SpleefMap.Layer layer = spleefMap.getLayer(pos);
        if (layer != null) {

            event.setCancelled(false);

        }

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event, SpleefCompetition competition) {

        SpleefMap spleefMap = (SpleefMap) competition.getMap();
        if (spleefMap.getDeathRegion() != null && spleefMap.getDeathRegion().isInside(event.getTo())) {

            event.getPlayer().damage(10000.0D); // Kill the player
            return;

        }

        // Ensure that the game is the decay spleef game
        if (this.game != SpleefGame.DECAY) {

            return;

        }

        // If a player changes blocks and the block below is of a
        // spleef layer, then decay the layer
        Block block = SpleefUtil.getBlockUnderPlayer(event.getTo().getBlockY() - 1, event.getTo());
        if (block == null || block.getType().isAir()) {

            return;

        }

        BlockPosition pos = block.getLocation().toBlock();
        SpleefMap.Layer layer = spleefMap.getLayer(pos);
        if (layer == null) {

            return;

        }

        competition.decayBlock(pos);

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) {

            return;

        }

        if (this.game != SpleefGame.SPLEGG) {

            return;

        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {

            return;

        }

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(ArenaSpleef.getInstance().getSpleefItemKey(),
                PersistentDataType.BOOLEAN))
        {

            event.setCancelled(true);

        }

        // Shoot an egg
        event.getPlayer().launchProjectile(Egg.class, null, egg -> {

            egg.setMetadata("splegg", new FixedMetadataValue(ArenaSpleef.getInstance(), true));
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);

        });

    }

    // For the events below:
    // We'd only ever capture this event if the conditions in the event resolvers
    // are met, so we can safely run the below code without having to check for a
    // lot of conditions that would otherwise be necessary.

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onProjectileHit(ProjectileHitEvent event, SpleefCompetition competition) {

        Block block = event.getHitBlock();
        SpleefMap spleefMap = (SpleefMap) competition.getMap();

        BlockPosition pos = block.getLocation().toBlock();
        SpleefMap.Layer layer = spleefMap.getLayer(pos);
        if (layer == null) {

            return;

        }

        block.setType(Material.AIR);

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onEggHatch(ThrownEggHatchEvent event) {

        Egg egg = event.getEgg();
        if (!egg.hasMetadata("splegg")) {

            return;

        }

        event.setHatching(false);

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onTntPrime(TNTPrimeEvent event) {

        event.setCancelled(true);
        if (event.getCause() == TNTPrimeEvent.PrimeCause.PROJECTILE) {

            if (event.getPrimingEntity() != null && !(event.getPrimingEntity() instanceof Player)) {

                event.getPrimingEntity().remove();

            }

            event.getBlock().setType(Material.AIR);
            TNTPrimed tntEntity = event.getBlock().getWorld().spawn(event.getBlock().getLocation().toCenterLocation(),
                    TNTPrimed.class, tnt ->
                    {

                        tnt.setFuseTicks(Integer.MAX_VALUE);

                    });
            Bukkit.getScheduler().runTaskLater(ArenaSpleef.getInstance(), tntEntity::remove, 20);

        }

    }

    public Duration getLayerDecayDelay() {

        return this.layerDecayDelay;

    }

    public Duration getLayerDecayTime() {

        return this.layerDecayTime;

    }

    public SpleefGame getGame() {

        return this.game;

    }

}
