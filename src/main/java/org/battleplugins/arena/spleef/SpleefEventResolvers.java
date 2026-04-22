package org.battleplugins.arena.spleef;

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.competition.Competition;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.event.ArenaEventHandler;
import org.battleplugins.arena.spleef.arena.SpleefArena;
import org.battleplugins.arena.spleef.arena.SpleefCompetition;
import org.battleplugins.arena.spleef.arena.SpleefGame;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.function.Function;

/**
 * Contains custom event resolvers for Spleef events.
 * <p>
 * The events below would normally not be captured by the
 * {@link ArenaEventHandler} annotation since there is no default logic for them
 * to link back to a {@link Competition}. However, these custom resolvers allow
 * for these events to be listened on, along with segmenting the logic to the
 * appropriate arena and spleef game mode.
 */
public final class SpleefEventResolvers {

    public static final Function<ProjectileHitEvent, LiveCompetition<?>> PROJECTILE_HIT = event -> {

        if (!(event.getEntity() instanceof Egg egg)) {

            return null;

        }

        if (event.getHitBlock() == null) {

            return null;

        }

        if (!egg.hasMetadata("splegg") && !ArenaSpleef.getInstance().getMainConfig().shouldProjectilesBreakBlocks()) {

            return null;

        }

        ProjectileSource shooter = egg.getShooter();
        if (!(shooter instanceof Player player)) {

            return null;

        }

        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null || !(arenaPlayer.getArena() instanceof SpleefArena)) {

            return null;

        }

        return arenaPlayer.getCompetition();

    };

    public static final Function<ThrownEggHatchEvent, LiveCompetition<?>> THROWN_EGG_HATCH = event -> {

        if (!event.getEgg().hasMetadata("splegg")) {

            return null;

        }

        ProjectileSource shooter = event.getEgg().getShooter();
        if (!(shooter instanceof Player player)) {

            return null;

        }

        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null || !(arenaPlayer.getArena() instanceof SpleefArena)) {

            return null;

        }

        return arenaPlayer.getCompetition();

    };

    public static final Function<SpleefArena, Function<TNTPrimeEvent, LiveCompetition<?>>> TNT_PRIME = arena -> event -> {

        if (arena.getGame() != SpleefGame.BOW_SPLEEF) {

            return null;

        }

        List<Competition<?>> competitions = BattleArena.getInstance().getCompetitions(arena);
        for (Competition<?> competition : competitions) {

            if (!(competition instanceof SpleefCompetition spleefCompetition)) {

                continue;

            }

            if (!spleefCompetition.getMap().getWorld().equals(event.getBlock().getWorld())) {

                continue;

            }

            if (spleefCompetition.getMap().getBounds() != null
                    && spleefCompetition.getMap().getBounds().isInside(event.getBlock().getLocation()))
            {

                return spleefCompetition;

            }

        }

        return null;

    };

}
