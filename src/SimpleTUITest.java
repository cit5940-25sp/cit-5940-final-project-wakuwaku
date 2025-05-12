import java.io.IOException;
import java.util.*;

/**
 * Simplified TUI test
 */
public class SimpleTUITest {

    public static void main(String[] args) throws IOException {
        // load movie data
        System.out.println("Loading movie data...");
        String moviesPath = "resources/tmdb_5000_movies.csv";
        String creditsPath = "resources/tmdb_5000_credits.csv";

        MovieIndex movieIndex = MovieDataLoader.buildIndex(moviesPath, creditsPath);
        System.out.println("Loaded " + movieIndex.size() + " movies.");

        // initialize controller
        GameController controller = new GameController(movieIndex);
        controller.setTurnTimeLimit(300);

        // set initial movie
        List<Movie> popularMovies = findPopularMovies(movieIndex);
        Movie initialMovie = selectRandomMovie(popularMovies);
        controller.setInitialMovie(initialMovie);
        System.out.println("Initial movie: " + initialMovie.getTitle() + " (" +
                String.join(", ", initialMovie.getGenres()) + ")");

        // show the connections (easy for test)
        printMovieCredits(initialMovie);

        // set player's genre
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nAvailable genres: ");
        Set<String> allGenres = getAllGenres(movieIndex);
        System.out.println(String.join(", ", allGenres));

        System.out.print("Player 1 genre: ");
        String player1Genre = scanner.nextLine().trim();

        System.out.print("Player 2 genre: ");
        String player2Genre = scanner.nextLine().trim();

        Player player1 = new Player("Player 1", new GenreWinCondition(player1Genre, 5));
        Player player2 = new Player("Player 2", new GenreWinCondition(player2Genre, 5));

        controller.addPlayer(player1);
        controller.addPlayer(player2);

        // start
        controller.startGame();

        // game logic
        List<Movie> gameHistory = new ArrayList<>();
        gameHistory.add(initialMovie);

        while (controller.getState() == GameController.GameState.IN_PROGRESS) {
            Player currentPlayer = controller.getCurrentPlayer();
            System.out.println("\n=========================================");
            System.out.println("Turn " + controller.getTurnCount() + " - " + currentPlayer.getName() + "'s turn");
            System.out.println("Player 1 score: " + player1.getScore() + "/5");
            System.out.println("Player 2 score: " + player2.getScore() + "/5");

            // show the last movie
            Movie previousMovie = gameHistory.get(gameHistory.size() - 1);
            System.out.println("\nPrevious movie: " + previousMovie.getTitle());
            printMovieCredits(previousMovie);

            // show the possible have connection movies
            System.out.println("\nPossible connections (up to 5 examples):");
            Set<Movie> possibleMovies = controller.getPossibleMovies();
            List<Movie> suggestions = new ArrayList<>(possibleMovies);
            if (suggestions.size() > 5) {
                Collections.shuffle(suggestions);
                suggestions = suggestions.subList(0, 5);
            }
            for (Movie m : suggestions) {
                System.out.println("- " + m.getTitle() + " (" + String.join(", ", m.getGenres()) + ")");
            }

            // user input
            System.out.print("\nEnter movie title (or 'quit' to exit): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                controller.endGame();
                break;
            }

            if (input.equalsIgnoreCase("skip")) {
                // skip the current turn
                System.out.println("Skipping turn...");
                controller.processTurn("__skip_turn__");
                continue;
            }

            // processing input
            System.out.println("Processing: " + input);
            boolean validMove = controller.processTurn(input);

            if (validMove) {
                System.out.println("Valid move!");
                Movie namedMovie = movieIndex.getByTitle(input);
                if (namedMovie != null) {
                    gameHistory.add(namedMovie);

                    // check whether genre matched
                    boolean matchesGenre = false;
                    String targetGenre = "";

                    if (currentPlayer.getWinCondition() instanceof GenreWinCondition) {
                        GenreWinCondition gc = (GenreWinCondition) currentPlayer.getWinCondition();
                        targetGenre = gc.getGenre();

                        for (String genre : namedMovie.getGenres()) {
                            if (genre.equalsIgnoreCase(targetGenre)) {
                                matchesGenre = true;
                                break;
                            }
                        }
                    }

                    System.out.println("Movie genres: " + String.join(", ", namedMovie.getGenres()));
                    System.out.println("Player's target genre: " + targetGenre);
                    System.out.println("Genre match: " + matchesGenre);
                    System.out.println("Player score: " + currentPlayer.getScore());
                    System.out.println("Win progress: " + currentPlayer.getProgressDescription());
                }
            } else {
                System.out.println("Invalid move! Try again.");
            }

            // check whether to end
            if (controller.getState() == GameController.GameState.COMPLETED) {
                Player winner = controller.getWinner();
                if (winner != null) {
                    System.out.println("\nGame Over! Winner: " + winner.getName());
                    System.out.println("Score: " + winner.getScore() + "/5");
                } else {
                    System.out.println("\nGame Over! No winner.");
                }
                break;
            }
        }

        // game summary
        System.out.println("\n=========== GAME SUMMARY ===========");
        System.out.println(controller.getGameSummary());

        scanner.close();
    }

    // helper function to find the movies have the most connections
    private static List<Movie> findPopularMovies(MovieIndex movieIndex) {
        List<Movie> popular = new ArrayList<>();
        for (Movie m : movieIndex.all()) {
            if (hasGoodConnections(m)) {
                popular.add(m);
            }
        }
        return popular;
    }

    // helper function of having enough connections
    private static boolean hasGoodConnections(Movie movie) {
        int totalPeople = 0;
        for (MovieRole role : MovieRole.values()) {
            totalPeople += movie.getPeople(role).size();
        }
        return totalPeople >= 5;
    }

    // helper function to randomly selected initial movie
    private static Movie selectRandomMovie(List<Movie> movies) {
        if (movies.isEmpty()) return null;
        Random random = new Random();
        return movies.get(random.nextInt(movies.size()));
    }

    // helper function to print the connection
    private static void printMovieCredits(Movie movie) {
        System.out.println("- Actors: " + String.join(", ", movie.getPeople(MovieRole.ACTOR)));
        System.out.println("- Directors: " + String.join(", ", movie.getPeople(MovieRole.DIRECTOR)));
        System.out.println("- Writers: " + String.join(", ", movie.getPeople(MovieRole.WRITER)));
    }

    // helper function to get genere
    private static Set<String> getAllGenres(MovieIndex movieIndex) {
        Set<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Movie m : movieIndex.all()) {
            genres.addAll(m.getGenres());
        }
        return genres;
    }
}