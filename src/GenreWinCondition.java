/**
 * Win condition: first to name a certain number of movies of a specific genre.
 * for this game, we will set 5 movies with the genre that users chose as win condition
 * but the specific number (5) will be handled in GameController class..
 */
public class GenreWinCondition implements WinConditionStrategy {
    private final String genre;
    private final int targetCount;
    private int currentCount = 0;

    public GenreWinCondition(String genre, int targetCount) {
        this.genre = genre.toLowerCase();
        this.targetCount = targetCount;
    }

    @Override
    public void recordMovie(Movie movie) {
        if (movie.getGenres().stream()
                .anyMatch(g -> g.equalsIgnoreCase(genre))) {
            currentCount++;
        }
    }

    @Override
    public boolean isSatisfied() {
        return currentCount >= targetCount;
    }

    @Override
    public String getDescription() {
        return String.format("First to name %d %s movie(s)", targetCount, genre);
    }

    @Override
    public int getCurrentCount() {
        return currentCount;
    }

    @Override
    public int getTargetCount() {
        return targetCount;
    }

    public String getGenre() {
        return genre;
    }
}


