import java.io.IOException;
import java.util.Map;

/**
 * Responsible for coordinating the loading process:
 * first metadata, then credits, and finally building the index.
 */
public final class MovieDataLoader {

    public static MovieIndex buildIndex(String moviesCsv, String creditsCsv) throws IOException {

        // Step 1: Load basic movie metadata
        Map<Integer, Movie> movies = MovieFactory.loadMovies(moviesCsv);

        // Step 2: Apply credits (actors, directors, etc.) to each movie
        CreditFactory.applyCredits(creditsCsv, movies);

        // Step 3: Build the index structure from enriched movie data
        MovieIndex idx = new MovieIndex();

        for (Movie m : movies.values()) {
            idx.addMovie(m);
        }
        return idx;
    }
}
