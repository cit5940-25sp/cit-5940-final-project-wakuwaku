
/**
 * Strategy interface for defining win conditions in the Movie Name Game.
 */
public interface WinConditionStrategy {
    /**
     * Records that the given movie has been played, updating internal counters if relevant.
     */
    void recordMovie(Movie movie);

    /**
     * Checks whether the win condition has been satisfied.
     * @return true if condition met
     */
    boolean isSatisfied();

    /**
     * @return A human-readable description of this win condition.
     */
    String getDescription();

    /**
     * @return Current progress counter towards the win condition (for display).
     */
    int getCurrentCount();

    /**
     * @return Target count needed to win.
     */
    int getTargetCount();
}

