import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private Player player1;
    private Player player2;
    private boolean isPlayer1Turn;
    private int turnCount;
    private GameState state;
    private Player winner;
    private final Set<Integer> usedMovieIds;
    private int turnTimeLimit; // in seconds
    private int secondsRemaining;
    private boolean timerRunning;
    private ScheduledExecutorService scheduler;
    private final int scoreToWin;
    // Track usage of connections
    private final Map<String, Integer> connectionUsageCount;
    // Initial movie provided by TUI to start the game
    private Movie initialMovie;

    /**
     * Creates a new game controller with the specified movie database.
     *
     * @param movieIndex The index containing all available movies
     */
    public GameController(MovieIndex movieIndex) {
        this.movieIndex = Objects.requireNonNull(movieIndex, "Movie index cannot be null");
        this.player1 = null;
        this.player2 = null;
        this.isPlayer1Turn = true; // player 1 to begin the game
        this.turnCount = 0;
        this.state = GameState.NOT_STARTED;
        this.winner = null;
        this.usedMovieIds = new HashSet<>();
        this.turnTimeLimit = 30; // Default 30-second turn limit
        this.secondsRemaining = turnTimeLimit;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scoreToWin = 5;
        this.connectionUsageCount = new HashMap<>();
        this.initialMovie = null;
    }

    /**
     * Sets the initial movie for the game.
     * This movie is randomly chosen by the TUI and serves as the starting point.
     *
     * @param movie The initial movie
     */
    public void setInitialMovie(Movie movie) {
        if (state == GameState.NOT_STARTED && movie != null) {
            this.initialMovie = movie;
            this.usedMovieIds.add(movie.getId());
        }
    }

    /**
     * Gets the initial movie for the game.
     *
     * @return The initial movie, or null if not set
     */
    public Movie getInitialMovie() {
        return initialMovie;
    }

    /**
     * Adds a player to the game.
     *
     * @param player The player to add
     * @return true if the player was added successfully, false otherwise
     */
    public boolean addPlayer(Player player) {
        if (state != GameState.NOT_STARTED) return false;
        if (player == null) return false;
        if (player1 == null) {
            player1 = player;
            return true;
        } else if (player2 == null) {
            player2 = player;
            return true;
        }

        return false;
    }

    /**
     * Gets a specific player by index (0 for player1, 1 for player2).
     *
     * @param index The index of the player to get
     * @return The requested player, or null if not found
     */
    public Player getPlayer(int index) {
        return switch(index) {
            case 0 -> player1;
            case 1 -> player2;
            default -> null;
        };
    }

    /**
     * Gets the opponent of a given player.
     *
     * @param player The player whose opponent to find
     * @return The opponent player, or null if not found
     */
    public Player getOpponent(Player player) {
        if (player == null) return null;
        if (player == player1) return player2;
        if (player == player2) return player1;
        return null;
    }

    /**
     * Sets the time limit for each turn.
     *
     * @param seconds The time limit in seconds
     */
    public void setTurnTimeLimit(int seconds) {
        if (seconds > 0) {
            this.turnTimeLimit = seconds;
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
     * Gets the current turn remaining seconds.
     * @return The remaining seconds
     */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }


    /**
     * Starts the game if it hasn't been started already.
     *
     * @return true if the game was started successfully, false otherwise
     */
    public boolean startGame() {
        if (state != GameState.NOT_STARTED) return false;
        if (player1 == null || player2 == null) return false;
        if (initialMovie == null) return false;  // Ensure initial movie is set


        state = GameState.IN_PROGRESS;
        isPlayer1Turn = true;
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
        if (state != GameState.IN_PROGRESS) return false;
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            return false;
        }
        // Get the current player
        Player currentPlayer = getCurrentPlayer();

        // Find the movie in the database
        Movie movie = movieIndex.getByTitle(movieTitle.trim());

        if (movie == null || usedMovieIds.contains(movie.getId())) {
            // Movie doesn't exist or movie is used
            return false;
        }

        // Check if the current movie is connected to the previous movie
        boolean isConnected = false;
        String connectionUsed = null;

        // For the first turn, check connection with the initial movie
        Movie previousMovie = (turnCount == 0) ? initialMovie : getOpponent(currentPlayer).getLastNamedMovie();

        if (previousMovie != null) {
            // Check each role for connections
            for (MovieRole role : MovieRole.values()) {
                Set<String> previousMoviePeople = previousMovie.getPeople(role);

                // For each person in the previous movie with this role
                for (String person : previousMoviePeople) {
                    // Check if this person appears in any role in the current movie
                    for (MovieRole currentRole : MovieRole.values()) {
                        if (movie.getPeople(currentRole).contains(person)) {
                            isConnected = true;
                            // Create a unique key for this connection
                            connectionUsed = person + ":" + role.name() + ":" + currentRole.name();
                            break;
                        }
                    }
                    if (isConnected) break;
                }
                if (isConnected) break;
            }

            if (!isConnected) {
                // No connection found between the movies
                return false;
            }

            // Check if this connection has been used more than 3 times
            if (connectionUsed != null) {
                int usageCount = connectionUsageCount.getOrDefault(connectionUsed, 0);
                if (usageCount >= 3) {
                    // Connection used too many times, current player loses
                    winner = getOpponent(currentPlayer);
                    state = GameState.COMPLETED;
                    timerRunning = false;
                    return false;
                }

                // Update the usage count for this connection
                connectionUsageCount.put(connectionUsed, usageCount + 1);
            }
        }

        // Stop the current timer
        timerRunning = false;

        // Valid move, process it
        currentPlayer.addNamedMovie(movie);
        usedMovieIds.add(movie.getId());

        // Award points only if the movie matches the player's selected genre
        WinConditionStrategy winCondition = currentPlayer.getWinCondition();
        if (winCondition instanceof GenreWinCondition) {
            GenreWinCondition genreCondition = (GenreWinCondition) winCondition;
            String targetGenre = genreCondition.getGenre();

            boolean matchesGenre = false;
            for (String genre : movie.getGenres()) {
                if (genre.equalsIgnoreCase(targetGenre)) {
                    matchesGenre = true;
                    break;
                }
            }

            // Only add score if the movie matches the player's genre
            if (matchesGenre) {
                currentPlayer.addScore(1);
            }
        } else {
            // If using a different win condition type, just add the score
            currentPlayer.addScore(1);
        }

        // Check if player has won by naming enough movies of their selected genre
        if (currentPlayer.hasWon() && currentPlayer.getScore() >= scoreToWin) {
            winner = currentPlayer;
            state = GameState.COMPLETED;
            timerRunning = false;
            return true;
        }

        // Move to the next player
        isPlayer1Turn = !isPlayer1Turn;
        turnCount++;

        // Start the timer for the next turn
        timerRunning = false;
        startTurnTimer();

        return true;
    }

    /**
     * Handles a case where the current player's turn time has expired.
     * The current player loses, and the other player wins.
     */
    private void handleTimeExpired() {
        if (state != GameState.IN_PROGRESS) return;
        // Time expired for current player, so the other player wins
        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer = getOpponent(currentPlayer);

        winner = otherPlayer;
        state = GameState.COMPLETED;
        timerRunning = false;
    }

    /**
     * Starts a timer for the current player's turn.
     */
    private void startTurnTimer() {
        cancelTurnTimer();
        // reset the timer
        secondsRemaining = turnTimeLimit;
        timerRunning = true;

        scheduler.scheduleAtFixedRate(() -> {
            if (timerRunning && secondsRemaining > 0) {
                secondsRemaining--;

                // if time expired
                if (secondsRemaining == 0) {
                    handleTimeExpired();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Cancels the current turn timer if it exists.
     */
    private void cancelTurnTimer() {
        timerRunning = false;
        scheduler.shutdownNow();

        // create the new scheduler for future use
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Ends the game prematurely.
     *
     * @return true if the game was ended, false if it was already over
     */
    public boolean endGame() {
        if (state == GameState.COMPLETED) return false;
        // stop timer
        timerRunning = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        state = GameState.COMPLETED;

        // find the winner
        if (player1 != null && player1.hasWon() && player1.getScore() >= scoreToWin) {
            winner = player1;
        } else if (player2 != null && player2.hasWon() && player2.getScore() >= scoreToWin) {
            winner = player2;
        } else {
            // if none players satisfies these two conditions then no winner.
            winner = null;
        }

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
        if (state != GameState.IN_PROGRESS) return null;
        return isPlayer1Turn ? player1 : player2;
    }

    /**
     * Gets all players in the game.
     *
     * @return An unmodifiable list of all players
     */
    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>(2);
        if (player1 != null) players.add(player1);
        if (player2 != null) players.add(player2);
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
        builder.append("Win conditions: Score ≥ ").append(scoreToWin).append("\n");
        if (initialMovie != null) {
            builder.append("Initial movie: ").append(initialMovie.getTitle())
                    .append(" (").append(String.join(", ", initialMovie.getGenres())).append(")\n");
        }
        if (state == GameState.IN_PROGRESS) {
            Player current = getCurrentPlayer();
            builder.append("Current player: ").append(current.getName()).append("\n");
            builder.append("Current player score: ").append(current.getScore()).append("/").
                    append(scoreToWin).append("\n");
            builder.append("Time remaining: ").append(secondsRemaining).append(" seconds\n");
        }
        if (state == GameState.COMPLETED) {
            if (winner != null) {
                builder.append("Winner: ").append(winner.getName()).append("\n");
            } else {
                builder.append("Game ended with no winner\n");
            }
        }
        builder.append("\nPlayers:\n");
        if (player1 != null) {
            builder.append("- ").append(player1.getName())
                    .append(" | Score: ").append(player1.getScore()).append("/")
                    .append(scoreToWin).append(" | ")
                    .append(player1.getProgressDescription()).append("\n");
        }
        if (player2 != null) {
            builder.append("- ").append(player2.getName())
                    .append(" | Score: ").append(player2.getScore()).append("/").append(scoreToWin)
                    .append(" | ").append(player2.getProgressDescription()).append("\n");
        }
        // Show connection usage counts
        if (!connectionUsageCount.isEmpty()) {
            builder.append("\nConnection usage counts:\n");
            for (Map.Entry<String, Integer> entry : connectionUsageCount.entrySet()) {
                String connection = entry.getKey();
                int count = entry.getValue();
                if (count > 1) {  // Only show connections used more than once
                    String[] parts = connection.split(":");
                    String person = parts[0];
                    builder.append("- ").append(person).append(": used ").append(count)
                            .append("/3 times\n");
                }
            }
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
// For the first turn, check connections with the initial movie
        Movie sourceMovie;
        if (turnCount == 0) {
            sourceMovie = initialMovie;
        } else {
            // Get the last movie from the previous player
            Player currentPlayer = getCurrentPlayer();
            Player previousPlayer = getOpponent(currentPlayer);
            sourceMovie = previousPlayer.getLastNamedMovie();
        }

        if (sourceMovie == null) return new HashSet<>();

        // Get movies connected to the source movie through people
        Set<Movie> possibleMovies = movieIndex.getConnectedMovies(sourceMovie);

        // Filter out movies that have already been used
        possibleMovies.removeIf(movie -> usedMovieIds.contains(movie.getId()));

        // Filter out movies that would use a connection more than 3 times
        possibleMovies.removeIf(movie -> wouldExceedConnectionLimit(sourceMovie, movie));

        return possibleMovies;
    }
    /**
     * Checks if using this movie would exceed the 3-time limit for any connection.
     */
    private boolean wouldExceedConnectionLimit(Movie lastMovie, Movie newMovie) {
        // Check each role for connections
        for (MovieRole role : MovieRole.values()) {
            Set<String> lastMoviePeople = lastMovie.getPeople(role);

            // For each person in the last movie with this role
            for (String person : lastMoviePeople) {
                // Check if this person appears in any role in the new movie
                for (MovieRole newRole : MovieRole.values()) {
                    if (newMovie.getPeople(newRole).contains(person)) {
                        // Create a unique key for this connection
                        String connectionKey = person + ":" + role.name() + ":" + newRole.name();
                        int usageCount = connectionUsageCount.getOrDefault(connectionKey, 0);

                        if (usageCount >= 3) {
                            // Using this movie would exceed the connection limit
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
    /**
     * Gets the connection usage count.
     *
     * @return A map of connection identifiers to usage counts
     */
    public Map<String, Integer> getConnectionUsageCount() {
        return new HashMap<>(connectionUsageCount);
    }

    /**
     * Close the scheduler
     */
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}