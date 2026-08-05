package net.trueog.spleefog.arena;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

// A sidebar that is built once per arena and then only mutated in place.
// Every line owns a permanent, invisible entry string, so the scores never move. The visible text lives in the line's
// team prefix, which means an update is a single team packet instead of a teardown and rebuild of the whole objective.
// That is what keeps the sidebar from flickering once per second.
final class ArenaScoreboard {

    private static final String ENTRY_ALPHABET = "0123456789abcdef";
    private static final String OBJECTIVE_NAME = "spleef";

    private final Scoreboard board;
    private final Objective objective;
    private final List<Team> lines = new ArrayList<>();

    ArenaScoreboard(Component title, int lineCount) {

        if (lineCount > ENTRY_ALPHABET.length()) {

            throw new IllegalArgumentException("A sidebar cannot have more than " + ENTRY_ALPHABET.length() + " lines");

        }

        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = this.board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int index = 0; index < lineCount; index++) {

            String entry = entry(index);
            Team team = this.board.registerNewTeam(OBJECTIVE_NAME + "-" + index);
            team.addEntry(entry);
            this.lines.add(team);
            this.objective.getScore(entry).setScore(lineCount - index);

        }

    }

    Scoreboard board() {

        return this.board;

    }

    void title(Component title) {

        this.objective.displayName(title);

    }

    void line(int index, Component text) {

        this.lines.get(index).prefix(text);

    }

    // Frees the objective and teams so the weakly held scoreboard can be collected
    // promptly.
    void unregister() {

        for (Team team : this.lines) {

            try {

                team.unregister();

            } catch (IllegalStateException ignored) {

                // Already gone; nothing to release.

            }

        }

        this.lines.clear();
        try {

            this.objective.unregister();

        } catch (IllegalStateException ignored) {

            // Already gone; nothing to release.

        }

    }

    private static String entry(int index) {

        // A bare colour code renders as nothing, which gives each line a unique but
        // invisible identity.
        return "§" + ENTRY_ALPHABET.charAt(index);

    }

}
