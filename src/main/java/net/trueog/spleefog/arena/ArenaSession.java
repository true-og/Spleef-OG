package net.trueog.spleefog.arena;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.SpleefArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public final class ArenaSession {

    private static final int SCOREBOARD_LINES = 5;

    private final ArenaManager manager;
    private final SpleefArena arena;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> matchPlayers = new LinkedHashSet<>();
    private final Set<UUID> eliminated = new LinkedHashSet<>();
    private final Map<UUID, String> playerNames = new LinkedHashMap<>();
    private ArenaState state = ArenaState.WAITING;
    private int secondsRemaining;
    private ArenaScoreboard scoreboard;
    // Set once the players have been told the start is held for the floor, so the
    // message is not repeated every second of the wait.
    private boolean announcedFloorWait;

    ArenaSession(ArenaManager manager, SpleefArena arena) {

        this.manager = manager;
        this.arena = arena;

    }

    public SpleefArena arena() {

        return this.arena;

    }

    public ArenaState state() {

        return this.state;

    }

    public int secondsRemaining() {

        return this.secondsRemaining;

    }

    public int playerCount() {

        return this.players.size();

    }

    public int spectatorCount() {

        return this.spectators.size();

    }

    public int aliveCount() {

        return this.alive.size();

    }

    public boolean hasPlayer(UUID playerId) {

        return this.players.contains(playerId);

    }

    public boolean hasSpectator(UUID playerId) {

        return this.spectators.contains(playerId);

    }

    public boolean isAlive(UUID playerId) {

        return this.state == ArenaState.IN_GAME && this.alive.contains(playerId);

    }

    public boolean isActive() {

        return !this.players.isEmpty() || !this.spectators.isEmpty();

    }

    public boolean canJoin() {

        return this.arena.enabled() && this.arena.isComplete()
                && (this.state == ArenaState.WAITING || this.state == ArenaState.COUNTDOWN)
                && this.players.size() < this.arena.capacity() && this.manager.isArenaRuntimeValid(this.arena);

    }

    // Every refusal is explained to the player here or in ArenaManager.enter, so a
    // caller only needs the boolean.
    public boolean join(Player player) {

        if (this.manager.session(player) != null) {

            Messages.send(player, Messages.bad("You are already in a Spleef arena. Use /spleef leave first."));
            return false;

        }

        if (!this.canJoin()) {

            Messages.send(player, Messages.bad("That arena is unavailable, full, or already in progress."));
            return false;

        }

        if (this.manager.isInCombat(player)) {

            Messages.send(player, Messages.bad("You cannot join Spleef while combat tagged."));
            return false;

        }

        if (!this.manager.enter(player, this, false, this.arena.waitingSpawn())) {

            return false;

        }

        this.players.add(player.getUniqueId());
        this.playerNames.put(player.getUniqueId(), player.getName());
        this.broadcastCount(player.getName(), "joined");
        // Applied now rather than on the next arena tick, so the player is not left
        // with no sidebar for a second
        // after the other one has already been closed for them.
        this.manager.updateScoreboards(this);
        // Fired once the session already lists the player, so a listener that asks the
        // API or this session sees them as present.
        this.manager.announceJoin(player, this, false);
        return true;

    }

    public boolean spectate(Player player) {

        if (this.manager.session(player) != null) {

            Messages.send(player, Messages.bad("You are already in a Spleef arena. Use /spleef leave first."));
            return false;

        }

        // Requiring somebody to already be there stops an idle arena being used as a
        // free spectator-mode pass.
        if (!this.arena.enabled() || !this.arena.isComplete() || this.state == ArenaState.ENDING || !this.isActive()
                || !this.manager.isArenaRuntimeValid(this.arena))
        {

            Messages.send(player, Messages.bad("That arena cannot be spectated right now."));
            return false;

        }

        if (this.manager.isInCombat(player)) {

            Messages.send(player, Messages.bad("You cannot spectate Spleef while combat tagged."));
            return false;

        }

        if (!this.manager.enter(player, this, true, this.arena.spectatorSpawn())) {

            return false;

        }

        this.spectators.add(player.getUniqueId());
        this.manager.updateScoreboards(this);
        this.manager.announceJoin(player, this, true);
        return true;

    }

    public void leave(Player player) {

        UUID playerId = player.getUniqueId();
        boolean participant = this.players.remove(playerId);
        boolean spectator = this.spectators.remove(playerId);
        if (!participant && !spectator) {

            return;

        }

        if (participant && this.state == ArenaState.IN_GAME && this.matchPlayers.contains(playerId)) {

            this.alive.remove(playerId);
            this.eliminated.add(playerId);

        }

        this.manager.exit(player, this);
        if (participant && (this.state == ArenaState.WAITING || this.state == ArenaState.COUNTDOWN)) {

            this.broadcastCount(player.getName(), "left");

        }

        if (this.state == ArenaState.IN_GAME) {

            this.checkForWinner();

        }

    }

    public void eliminate(Player player) {

        UUID playerId = player.getUniqueId();
        if (!this.alive.remove(playerId)) {

            return;

        }

        this.eliminated.add(playerId);
        this.broadcast(Messages.body().append(Messages.name(player.getName()))
                .append(Component.text(" was eliminated. ")).append(Messages.value(Integer.toString(this.alive.size())))
                .append(Component.text(" remain.")).build());
        this.checkForWinner();

    }

    public void tick() {

        switch (this.state) {

            case WAITING -> this.tickWaiting();
            case COUNTDOWN -> this.tickCountdown();
            case IN_GAME -> this.tickGame();
            case ENDING -> this.tickEnding();

        }

        this.manager.updateScoreboards(this);

    }

    public void shutdown() {

        for (UUID playerId : this.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                this.manager.exit(player, this);

            }

        }

        this.clearRuntime();
        this.manager.resetLayers(this.arena);

    }

    private void tickWaiting() {

        if (this.players.size() < this.manager.config().minimumPlayers()) {

            return;

        }

        this.state = ArenaState.COUNTDOWN;
        this.secondsRemaining = this.manager.config().waitingSeconds();
        this.announcedFloorWait = false;
        // Started now so a large floor is written back during the countdown instead
        // of after it. Nothing can touch the floor in the meantime: waiting players
        // may not break blocks and the region denies everyone else.
        this.manager.resetLayers(this.arena);
        this.broadcastCountdown();

    }

    private void tickCountdown() {

        if (this.players.size() < this.manager.config().minimumPlayers()) {

            this.state = ArenaState.WAITING;
            this.secondsRemaining = 0;
            this.broadcast(Messages.body().append(Component.text("Countdown paused until at least "))
                    .append(Messages.value(this.manager.config().minimumPlayers() + " players"))
                    .append(Component.text(" are waiting.")).build());
            return;

        }

        if (this.secondsRemaining <= 1) {

            this.startMatch();
            return;

        }

        this.secondsRemaining--;
        if (this.secondsRemaining <= 5 || this.secondsRemaining == 10 || this.secondsRemaining == 15) {

            this.broadcastCountdown();

        }

    }

    private void startMatch() {

        if (!this.canStart()) {

            this.state = ArenaState.WAITING;
            return;

        }

        // The floor must be whole before anyone is placed on it. A reset that is
        // still being written over later ticks would otherwise leave holes at the
        // start, or fill in blocks players have just broken.
        if (this.manager.isResetting(this.arena)) {

            if (!this.announcedFloorWait) {

                this.announcedFloorWait = true;
                this.broadcast(
                        Messages.body().append(Component.text("Waiting for the floor to finish restoring.")).build());

            }

            return;

        }

        this.announcedFloorWait = false;
        this.state = ArenaState.IN_GAME;
        this.secondsRemaining = this.manager.config().timeLimitSeconds();
        this.alive.clear();
        this.alive.addAll(this.players);
        this.matchPlayers.clear();
        this.matchPlayers.addAll(this.players);
        this.eliminated.clear();

        List<org.bukkit.Location> spawns = this.arena.spawns();
        List<Player> unplaced = new java.util.ArrayList<>();
        int spawnIndex = 0;
        for (UUID playerId : this.players) {

            Player player = Bukkit.getPlayer(playerId);
            // Spawns resolve against loaded worlds, so a spawn can be missing here even
            // though capacity() counted it. Nobody is placed without one.
            if (player != null && (spawnIndex >= spawns.size()
                    || !this.manager.startPlayer(player, this.arena, spawns.get(spawnIndex++))))
            {

                unplaced.add(player);

            }

        }

        // Someone whose spawn teleport was refused is not on the floor and cannot
        // play. Taking them out through the normal leave path puts their state back
        // and lets the match go on, or end, without them.
        for (Player player : unplaced) {

            Messages.send(player,
                    Messages.bad("You could not be placed in the arena, so you were removed from the match."));
            this.leave(player);

        }

        if (this.state == ArenaState.IN_GAME) {

            this.broadcast(Messages.good("Go!"));

        }

    }

    private void tickGame() {

        if (this.secondsRemaining <= 1) {

            this.finish(null, true);
            return;

        }

        this.secondsRemaining--;
        if (this.secondsRemaining <= 10 || this.secondsRemaining == 30 || this.secondsRemaining == 60) {

            this.broadcast(Messages.body().append(Messages.value(this.secondsRemaining + " seconds"))
                    .append(Component.text(" remaining.")).build());

        }

    }

    private void tickEnding() {

        if (this.secondsRemaining <= 1) {

            this.resetAfterMatch();
            return;

        }

        this.secondsRemaining--;

    }

    private boolean canStart() {

        return this.players.size() >= this.manager.config().minimumPlayers()
                && this.players.size() <= this.arena.capacity() && this.manager.isArenaRuntimeValid(this.arena);

    }

    private void checkForWinner() {

        if (this.state != ArenaState.IN_GAME || this.alive.size() > 1) {

            return;

        }

        UUID winner = this.alive.stream().findFirst().orElse(null);
        this.finish(winner, false);

    }

    private void finish(UUID winner, boolean timedOut) {

        if (this.state != ArenaState.IN_GAME) {

            return;

        }

        this.state = ArenaState.ENDING;
        this.secondsRemaining = this.manager.config().victorySeconds();

        for (UUID playerId : this.matchPlayers) {

            String name = this.playerNames.getOrDefault(playerId, "Unknown");
            if (playerId.equals(winner)) {

                this.manager.stats().recordWin(playerId, name);

            } else if (timedOut && this.alive.contains(playerId)) {

                this.manager.stats().recordTie(playerId, name);

            } else {

                this.manager.stats().recordLoss(playerId, name);

            }

        }

        this.manager.stats().save();

        if (winner != null) {

            String winnerName = this.playerNames.getOrDefault(winner, "Unknown");
            this.broadcast(Messages.good(winnerName + " won the match!"));

        } else if (timedOut && !this.alive.isEmpty()) {

            this.broadcast(Messages.warn("Time expired. The match is a draw."));

        } else {

            this.broadcast(Messages.warn("The match ended without a winner."));

        }

        this.manager.showEnding(this);

    }

    private void resetAfterMatch() {

        for (UUID playerId : this.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                this.manager.exit(player, this);

            }

        }

        this.clearRuntime();
        this.manager.resetLayers(this.arena);

    }

    private void clearRuntime() {

        this.releaseScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        this.players.clear();
        this.spectators.clear();
        this.alive.clear();
        this.matchPlayers.clear();
        this.eliminated.clear();
        this.playerNames.clear();
        this.state = ArenaState.WAITING;
        this.secondsRemaining = 0;

    }

    // Returns this arena's sidebar, building it on first use so idle arenas cost
    // nothing.
    ArenaScoreboard scoreboard(Component title) {

        if (this.scoreboard == null) {

            this.scoreboard = new ArenaScoreboard(title, SCOREBOARD_LINES);

        }

        return this.scoreboard;

    }

    // Moves anyone still looking at the arena sidebar onto fallback, then frees it.
    void releaseScoreboard(Scoreboard fallback) {

        if (this.scoreboard == null) {

            return;

        }

        for (UUID playerId : this.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.getScoreboard() == this.scoreboard.board() && fallback != null) {

                player.setScoreboard(fallback);

            }

        }

        this.scoreboard.unregister();
        this.scoreboard = null;

    }

    public Set<UUID> allPresent() {

        Set<UUID> result = new LinkedHashSet<>(this.players);
        result.addAll(this.spectators);
        return result;

    }

    public Set<UUID> players() {

        return Set.copyOf(this.players);

    }

    public Set<UUID> spectators() {

        return Set.copyOf(this.spectators);

    }

    private void broadcast(Component message) {

        for (UUID playerId : this.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                Messages.send(player, message);

            }

        }

    }

    private void broadcastCount(String playerName, String verb) {

        this.broadcast(Messages.body().append(Messages.name(playerName)).append(Component.text(" " + verb + ". "))
                .append(Messages.value("(" + this.players.size() + "/" + this.arena.capacity() + ")")).build());

    }

    private void broadcastCountdown() {

        this.broadcast(Messages.body().append(Component.text("Match starts in "))
                .append(Messages.value(this.secondsRemaining + " seconds")).append(Component.text(".")).build());

    }

}
