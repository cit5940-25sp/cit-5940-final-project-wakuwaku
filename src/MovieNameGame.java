import java.io.IOException;

/**
 * Main application class for the Movie Name Game.
 *
 * NOTE: This is a temporary implementation for testing the game logic.
 * Member C will implement the actual UI and win conditions.
 */
public class MovieNameGame {

    public static void main(String[] args) {
        System.out.println("=== Movie Name Game ===");
        System.out.println("Loading movie database...");

        try {
            // Load movie data
            MovieIndex movieIndex = MovieDataLoader.buildIndex(
                    "resources/tmdb_5000_movies.csv",
                    "resources/tmdb_5000_credits.csv"
            );

            System.out.println("Loaded " + movieIndex.all().size() + " movies.");

            // Create and start the game
            GameController controller = new GameController(movieIndex);
            //GameView view = new GameView(controller);

            //view.play();

        } catch (IOException e) {
            System.err.println("Error loading movie data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}