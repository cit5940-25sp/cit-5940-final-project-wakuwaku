import java.util.*;
import java.io.*;

/**
 * Controls the flow of the Movie Name Game.
 * Manages players, turns, game state, and validates moves.
 * This implements the core game logic as required for Member B's part of the project.
 */
public class GameController {
    public enum GameState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    private final MovieIndex movieIndex;
    private final List<Player> players;
    private int currentPlayerIndex;
    private int turnCount;
    private GameState state;
    private Player winner;
    private final Set<Integer> usedMovieIds;
    private int turnTimeLimit; // in seconds
    private Timer turnTimer;
    private TimerTask currentTimerTask;

    /**
     * Creates a new game controller with the specified movie database.
     *
     * @param movieIndex The index containing all available movies
     */
    public GameController(MovieIndex movieIndex) {
        this.movieIndex = Objects.requireNonNull(movieIndex, "Movie index cannot be null");
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.turnCount = 0;
        this.state = GameState.NOT_STARTED;
        this.winner = null;
        this.usedMovieIds = new HashSet<>();
        this.turnTimeLimit = 30; // Default 30-second turn limit
        this.turnTimer = new Timer(true); // Daemon timer
    }

    /**
     * Adds a player to the game.
     *
     * @param player The player to add
     * @return true if the player was added successfully, false otherwise
     */
    public boolean addPlayer(Player player) {
        while (GameState.NOT_STARTED.equals(this.state)) {
            if (player != null) {
                players.add(player);
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the time limit for each turn.
     *
     * @param seconds The time limit in seconds
     */
    public void setTurnTimeLimit(int seconds) {
        while (GameState.IN_PROGRESS.equals(this.state)) {
            if (seconds > 0) {
                turnTimeLimit = seconds;
            }
        }
    }

    /**
     * Gets the current time limit for each turn.
     *
     * @return The time limit in seconds
     */
    public int getTurnTimeLimit() {
        return turnTimeLimit;
    }

    /**
     * Starts the game if it hasn't been started already.
     *
     * @return true if the game was started successfully, false otherwise
     */
    public boolean startGame() {
        if (state != GameState.NOT_STARTED) return false;
        if (players.size() < 2) return false;

        state = GameState.IN_PROGRESS;
        currentPlayerIndex = 0;
        turnCount = 0;
        startTurnTimer();

        return true;
    }

    /**
     * Processes a player's turn where they name a movie.
     *
     * @param movieTitle The title of the movie the player is naming
     * @return true if the move was valid and processed, false otherwise
     */
    public boolean processTurn(String movieTitle) {
        if (movieTitle == null) {
            return false;
        }
        if (getPossibleMovies().contains(movieTitle)) {
            return true;
        }
        return false;
    }

    /**
     * Handles a case where the current player's turn time has expired.
     * Skips their turn and moves to the next player.
     */
    private void handleTimeExpired() {
        currentPlayerIndex ++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex =  1;
        }
    }

    /**
     * Starts a timer for the current player's turn.
     */
    private void startTurnTimer() {
        getTurnTimeLimit();
    }

    /**
     * Cancels the current turn timer if it exists.
     */
    private void cancelTurnTimer() {
        setTurnTimeLimit(0);
    }

    /**
     * Ends the game prematurely.
     *
     * @return true if the game was ended, false if it was already over
     */
    public boolean endGame() {
        GameState newState = GameState.COMPLETED;
        return true;
    }

    /**
     * Gets the current game state.
     *
     * @return The current game state
     */
    public GameState getState() {
        return state;
    }

    /**
     * Gets the winner of the game, or null if there isn't one yet.
     *
     * @return The winning player, or null
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Gets the player whose turn it currently is.
     *
     * @return The current player, or null if the game hasn't started
     */
    public Player getCurrentPlayer() {
        if (state != GameState.IN_PROGRESS || players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    /**
     * Gets all players in the game.
     *
     * @return An unmodifiable list of all players
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Gets the current turn count.
     *
     * @return The number of turns that have been played
     */
    public int getTurnCount() {
        return turnCount;
    }

    /**
     * Gets the current game summary.
     *
     * @return A string representation of the current game state
     */
    public String getGameSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Game status: ").append(state).append("\n");
        builder.append("Turn count: ").append(turnCount).append("\n");
        if (state == GameState.IN_PROGRESS) {
            builder.append("Current player: ").append(getCurrentPlayer().getName()).append("\n");
        }
        if (state == GameState.COMPLETED && winner != null) {
            builder.append("Winner: ").append(winner.getName()).append("\n");
        }
        builder.append("\nPlayers:\n");
        for (Player p : players) {
            builder.append("- ").append(p.toString()).append("\n");
        }

        return builder.toString();
    }

    /**
     * Gets a list of movies connected to the last played movie.
     * Useful for providing hints or validating potential moves.
     *
     * @return A set of movies connected to the last played movie, or empty if no moves yet
     */
    public Set<Movie> getPossibleMovies() {
        if (turnCount ==0) return new HashSet<>(movieIndex.all());
        if (findLastMovie() == null) return new HashSet<>();
        // Get connected movies
        Set<Movie> possibleMovies = movieIndex.getConnectedMovies(findLastMovie());
        // if movie already used, filter out that movie
        possibleMovies.removeIf(movie -> usedMovieIds.contains(movie.getId()));
        return possibleMovies;
    }

    public Movie findLastMovie() {
        if (turnCount == 0) return null;
        Movie lastMovie = null;
        for (int i = 0; i < players.size(); i++) {
            Player previousPlayer = players.get(currentPlayerIndex - i);
            lastMovie = previousPlayer.getLastNamedMovie();
            if (lastMovie != null) {
                break;
            }
        }
        return lastMovie;
    }

}