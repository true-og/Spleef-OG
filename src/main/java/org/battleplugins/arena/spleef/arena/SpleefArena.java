package org.battleplugins.arena.spleef.arena;

import io.papermc.paper.math.BlockPosition;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.competition.map.MapFactory;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.battleplugins.arena.config.ArenaOption;
import org.battleplugins.arena.event.ArenaEventHandler;
import org.battleplugins.arena.event.arena.ArenaPhaseStartEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;
import org.battleplugins.arena.options.ArenaOptionType;
import org.battleplugins.arena.options.types.BooleanArenaOption;
import org.battleplugins.arena.spleef.ArenaSpleef;
import org.battleplugins.arena.spleef.SpleefEventResolvers;
import org.battleplugins.arena.spleef.SpleefExecutor;
import org.battleplugins.arena.spleef.SpleefMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpleefArena extends Arena {

    @ArenaOption(name = "game", description = "The spleef game.")
    private SpleefGame game = SpleefGame.CLASSIC;

    public SpleefArena() {

        super();

        this.getEventManager().registerArenaResolver(ProjectileHitEvent.class, SpleefEventResolvers.PROJECTILE_HIT);
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

            org.bukkit.World mapWorld = spleefCompetition.getMap().getWorld();
            String worldName = mapWorld == null ? null : mapWorld.getName();
            if (!ArenaSpleef.getInstance().getMainConfig().isWorldAllowed(worldName)) {

                ArenaSpleef.getInstance().getSLF4JLogger().warn(
                        "Refusing to start Spleef competition '{}' — world '{}' is not in the Spleef world whitelist.",
                        spleefCompetition.getMap().getName(), worldName);
                this.leaveWorldBlockedCompetition(spleefCompetition, worldName);
                return;

            }

            if (!spleefCompetition.isMatchRegionAllowed()) {

                ArenaSpleef.getInstance().getSLF4JLogger().warn(
                        "Refusing to start Spleef competition '{}' — map is not inside a whitelisted WorldGuard region.",
                        spleefCompetition.getMap().getName());
                this.leaveRegionBlockedCompetition(spleefCompetition);
                return;

            }

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

            if (!(player.getCompetition() instanceof SpleefCompetition competition)
                    || !competition.isLocationAllowed(event.getBlock().getLocation()))
            {

                return;

            }

            event.setCancelled(false);
            this.giveSnowballsFromFloor(event);

        }

    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event, SpleefCompetition competition) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ())
        {

            return; // Same block, skip hot-path work

        }

        SpleefMap spleefMap = (SpleefMap) competition.getMap();
        if (spleefMap.getDeathRegion() != null && spleefMap.getDeathRegion().isInside(event.getTo())) {

            event.getPlayer().damage(10000.0D); // Kill the player

        }

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

        if (!competition.isLocationAllowed(block.getLocation())) {

            return;

        }

        block.setType(Material.AIR);

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

    private void giveSnowballsFromFloor(BlockBreakEvent event) {

        int amount = this.getSnowballDropCount(event.getBlock());
        if (amount <= 0) {

            return;

        }

        event.setDropItems(false);
        event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOWBALL, amount));

    }

    private int getSnowballDropCount(Block block) {

        if (block.getType() == Material.SNOW_BLOCK) {

            return 4;

        }

        if (block.getType() != Material.SNOW) {

            return 0;

        }

        BlockData blockData = block.getBlockData();
        if (blockData instanceof Snow snow) {

            return snow.getLayers();

        }

        return 1;

    }

    private void leaveWorldBlockedCompetition(SpleefCompetition competition, String worldName) {

        List<ArenaPlayer> players = new ArrayList<>(competition.getPlayers());
        players.addAll(competition.getSpectators());
        for (ArenaPlayer player : players) {

            SpleefMessages.WORLD_BLOCKED_AT_RUNTIME.send(player.getPlayer(), worldName);
            competition.leave(player, ArenaLeaveEvent.Cause.PLUGIN);

        }

    }

    private void leaveRegionBlockedCompetition(SpleefCompetition competition) {

        List<ArenaPlayer> players = new ArrayList<>(competition.getPlayers());
        players.addAll(competition.getSpectators());
        for (ArenaPlayer player : players) {

            SpleefMessages.MATCH_REGION_NOT_WHITELISTED.send(player.getPlayer());
            competition.leave(player, ArenaLeaveEvent.Cause.PLUGIN);

        }

    }

    public SpleefGame getGame() {

        return this.game;

    }

}
