package net.trueog.spleefog.arena;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.trueog.spleefog.Messages;
import net.trueog.spleefog.model.ArenaState;
import net.trueog.spleefog.model.SpleefArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ArenaSession {

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

    public boolean join(Player player) {

        if (!this.canJoin() || this.manager.session(player) != null) {

            return false;

        }

        if (this.manager.isInCombat(player)) {

            Messages.send(player, "&cYou cannot join Spleef while combat tagged.");
            return false;

        }

        if (!this.manager.enter(player, this, false, this.arena.waitingSpawn())) {

            return false;

        }

        this.players.add(player.getUniqueId());
        this.playerNames.put(player.getUniqueId(), player.getName());
        this.broadcast(
                "&f" + player.getName() + " &7joined. &b(" + this.players.size() + "/" + this.arena.capacity() + ")");
        return true;

    }

    public boolean spectate(Player player) {

        if (!this.arena.enabled() || !this.arena.isComplete() || this.state == ArenaState.ENDING
                || this.manager.session(player) != null || !this.manager.isArenaRuntimeValid(this.arena))
        {

            return false;

        }

        if (!this.manager.enter(player, this, true, this.arena.spectatorSpawn())) {

            return false;

        }

        this.spectators.add(player.getUniqueId());
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

            this.broadcast(
                    "&f" + player.getName() + " &7left. &b(" + this.players.size() + "/" + this.arena.capacity() + ")");

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
        this.broadcast("&f" + player.getName() + " &7was eliminated. &b" + this.alive.size() + " &7remain.");
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
        this.broadcast("&7Match starts in &b" + this.secondsRemaining + " seconds&7.");

    }

    private void tickCountdown() {

        if (this.players.size() < this.manager.config().minimumPlayers()) {

            this.state = ArenaState.WAITING;
            this.secondsRemaining = 0;
            this.broadcast("&7Countdown paused until at least &b" + this.manager.config().minimumPlayers()
                    + " players &7are waiting.");
            return;

        }

        if (this.secondsRemaining <= 1) {

            this.startMatch();
            return;

        }

        this.secondsRemaining--;
        if (this.secondsRemaining <= 5 || this.secondsRemaining == 10 || this.secondsRemaining == 15) {

            this.broadcast("&7Match starts in &b" + this.secondsRemaining + " seconds&7.");

        }

    }

    private void startMatch() {

        if (!this.canStart()) {

            this.state = ArenaState.WAITING;
            return;

        }

        this.manager.resetLayers(this.arena);
        this.state = ArenaState.IN_GAME;
        this.secondsRemaining = this.manager.config().timeLimitSeconds();
        this.alive.clear();
        this.alive.addAll(this.players);
        this.matchPlayers.clear();
        this.matchPlayers.addAll(this.players);
        this.eliminated.clear();

        List<org.bukkit.Location> spawns = this.arena.spawns();
        int spawnIndex = 0;
        for (UUID playerId : this.players) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                this.manager.startPlayer(player, this.arena, spawns.get(spawnIndex++));

            }

        }

        this.broadcast("&aGo!");

    }

    private void tickGame() {

        if (this.secondsRemaining <= 1) {

            this.finish(null, true);
            return;

        }

        this.secondsRemaining--;
        if (this.secondsRemaining <= 10 || this.secondsRemaining == 30 || this.secondsRemaining == 60) {

            this.broadcast("&b" + this.secondsRemaining + " seconds &7remaining.");

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
            this.broadcast("&a" + winnerName + " won the match!");

        } else if (timedOut && !this.alive.isEmpty()) {

            this.broadcast("&eTime expired. The match is a draw.");

        } else {

            this.broadcast("&eThe match ended without a winner.");

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

        this.players.clear();
        this.spectators.clear();
        this.alive.clear();
        this.matchPlayers.clear();
        this.eliminated.clear();
        this.playerNames.clear();
        this.state = ArenaState.WAITING;
        this.secondsRemaining = 0;

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

    private void broadcast(String message) {

        for (UUID playerId : this.allPresent()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                Messages.send(player, message);

            }

        }

    }

}
