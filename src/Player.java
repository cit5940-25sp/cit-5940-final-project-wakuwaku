import java.util.*;

/**
 * Represents a player in the Movie Name Game.
 * Tracks player state including name, score, win condition, and movie history.
 * This class handles state management for Member B's implementation, while
 * interfacing with win conditions that will be provided by Member C.
 */
public class Player {
    private final String name;
    private int score;
    private final List<Movie> namedMovies;
    private final WinCondition winCondition;
    private boolean hasWon;

    /**
     * Creates a new player with the specified name and win condition.
     *
     * @param name The player's name
     * @param winCondition The win condition for this player
     */
    public Player(String name, WinCondition winCondition) {
        this.name = Objects.requireNonNull(name, "Player name cannot be null");
        this.winCondition = Objects.requireNonNull(winCondition, "Win condition cannot be null");
        this.namedMovies = new ArrayList<>();
        this.score = 0;
        this.hasWon = false;
    }

    /**
     * Gets the player's name.
     *
     * @return The player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the player's current score.
     *
     * @return The player's score
     */
    public int getScore() {
        return score;
    }

    /**
     * Increments the player's score by the specified amount.
     *
     * @param points Points to add to the player's score
     */
    public void addScore(int points) {
        if (points > 0) {
            this.score += points;
        }
    }

    /**
     * Gets the list of movies this player has named during the game.
     *
     * @return An unmodifiable list of movies
     */
    public List<Movie> getNamedMovies() {
        return Collections.unmodifiableList(namedMovies);
    }

    /**
     * Records a movie that the player has successfully named.
     * Also checks if this movie contributes to achieving the player's win condition,
     * which will be determined by Member C's win condition implementation.
     *
     * @param movie The movie to add to the player's history
     */
    public void addNamedMovie(Movie movie) {
        if (movie != null) {
            namedMovies.add(movie);

            // Check if this movie advances the player toward their win condition
            if (winCondition != null && winCondition.movieContributes(movie)) {
                // Check if player has won after adding this movie
                if (winCondition.hasAchieved(this)) {
                    hasWon = true;
                }
            }
        }
    }

    /**
     * Gets the player's win condition.
     * This will be set to an implementation provided by Member C.
     *
     * @return The player's win condition
     */
    public WinCondition getWinCondition() {
        return winCondition;
    }

    /**
     * Checks if the player has achieved their win condition.
     *
     * @return true if the player has won, false otherwise
     */
    public boolean hasWon() {
        return hasWon;
    }

    /**
     * Gets the progress of the player toward their win condition.
     *
     * @return A description of the player's progress
     */
    public String getProgressDescription() {
        if (winCondition == null) {
            return "No win condition set";
        }
        return winCondition.getProgressDescription(this);
    }

    /**
     * Gets the most recently named movie by this player, or null if none.
     *
     * @return The last movie named by this player, or null
     */
    public Movie getLastNamedMovie() {
        if (namedMovies.isEmpty()) {
            return null;
        }
        return namedMovies.get(namedMovies.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(name)
                .append(" | Score: ").append(score)
                .append(" | Progress: ").append(getProgressDescription());

        return sb.toString();
    }
}