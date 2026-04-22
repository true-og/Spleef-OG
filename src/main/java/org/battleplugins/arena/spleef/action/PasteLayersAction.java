package org.battleplugins.arena.spleef.action;

import org.battleplugins.arena.Arena;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.competition.Competition;
import org.battleplugins.arena.event.action.EventAction;
import org.battleplugins.arena.resolver.Resolvable;
import org.battleplugins.arena.spleef.arena.SpleefCompetition;

import java.util.Map;

public class PasteLayersAction extends EventAction {

    public PasteLayersAction(Map<String, String> params) {

        super(params);

    }

    @Override
    public void call(ArenaPlayer arenaPlayer, Resolvable resolvable) {

    }

    @Override
    public void postProcess(Arena arena, Competition<?> competition, Resolvable resolvable) {

        if (!(competition instanceof SpleefCompetition spleefCompetition)) {

            return;

        }

        spleefCompetition.pasteLayers();

    }

}
