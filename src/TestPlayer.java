import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
public class TestPlayer {
    private Player player;
    private WinConditionStrategy actionWinCondition;
    private WinConditionStrategy dramaWinCondition;
    private Movie avatarMovie, darkKnightMovie, titanicMovie, inceptionMovie, pulpFictionMovie;
    private static final String PLAYER_NAME = "Test Player";

    /**
     * Sets up the test environment with win conditions and sample movies.
     */
    @Before
    public void setUp() {
        // Create actual WinConditionStrategy implementations
        actionWinCondition = new GenreWinCondition("Action", 5);
        dramaWinCondition = new GenreWinCondition("Drama", 3);

        // Create test player
        player = new Player(PLAYER_NAME, actionWinCondition);

        // Create test movies IDs may differ from actual TMDB
        avatarMovie = new Movie(19995, "Avatar", 2009, Arrays.asList("Action", "Adventure", "Fantasy", "Science Fiction"));
        darkKnightMovie = new Movie(155, "The Dark Knight", 2008, Arrays.asList("Action", "Crime", "Drama", "Thriller"));
        titanicMovie = new Movie(597, "Titanic", 1997, Arrays.asList("Drama", "Romance"));
        inceptionMovie = new Movie(27205, "Inception", 2010, Arrays.asList("Action", "Science Fiction", "Adventure"));
        pulpFictionMovie = new Movie(680, "Pulp Fiction", 1994, Arrays.asList("Thriller", "Crime"));

        // Add movie credits to establish connections between films
        avatarMovie.addCredit(MovieRole.ACTOR, "Sam Worthington");
        avatarMovie.addCredit(MovieRole.DIRECTOR, "James Cameron");

        darkKnightMovie.addCredit(MovieRole.ACTOR, "Christian Bale");
        darkKnightMovie.addCredit(MovieRole.DIRECTOR, "Christopher Nolan");

        titanicMovie.addCredit(MovieRole.ACTOR, "Leonardo DiCaprio");
        titanicMovie.addCredit(MovieRole.ACTOR, "Kate Winslet");
        titanicMovie.addCredit(MovieRole.DIRECTOR, "James Cameron");

        inceptionMovie.addCredit(MovieRole.ACTOR, "Leonardo DiCaprio");
        inceptionMovie.addCredit(MovieRole.DIRECTOR, "Christopher Nolan");

        pulpFictionMovie.addCredit(MovieRole.ACTOR, "John Travolta");
        pulpFictionMovie.addCredit(MovieRole.ACTOR, "Samuel L. Jackson");
        pulpFictionMovie.addCredit(MovieRole.DIRECTOR, "Quentin Tarantino");
    }

    /**
     * Tests the Player constructor.
     */
    @Test
    public void testConstructor() {
        // Verify name
        assertEquals(PLAYER_NAME, player.getName());

        // Verify score is initialized to 0
        assertEquals(0, player.getScore());

        // Verify named movies list is initially empty
        assertTrue(player.getNamedMovies().isEmpty());

        // Verify win condition is set correctly
        assertSame(actionWinCondition, player.getWinCondition());
    }

    /**
     * Tests the getName method.
     */
    @Test
    public void testGetName() {
        assertEquals(PLAYER_NAME, player.getName());
    }

    /**
     * Tests the getScore and addScore methods.
     */
    @Test
    public void testScoreMethods() {
        // Initial score should be 0
        assertEquals(0, player.getScore());

        // Add positive score
        player.addScore(3);
        assertEquals(3, player.getScore());

        // Add more points
        player.addScore(2);
        assertEquals(5, player.getScore());

        // Try adding negative score (should be ignored)
        player.addScore(-2);
        assertEquals(5, player.getScore());

        // Try adding zero score (should be ignored)
        player.addScore(0);
        assertEquals(5, player.getScore());
    }

    /**
     * Tests the getNamedMovies and addNamedMovie methods.
     */
    @Test
    public void testNamedMoviesMethods() {
        // Initial named movies list should be empty
        assertTrue(player.getNamedMovies().isEmpty());

        // Add a movie
        player.addNamedMovie(avatarMovie);

        // Verify movie was added
        List<Movie> namedMovies = player.getNamedMovies();
        assertEquals(1, namedMovies.size());
        assertEquals(avatarMovie, namedMovies.get(0));

        // Add a second movie
        player.addNamedMovie(darkKnightMovie);

        // Verify both movies are in the list
        namedMovies = player.getNamedMovies();
        assertEquals(2, namedMovies.size());
        assertEquals(avatarMovie, namedMovies.get(0));
        assertEquals(darkKnightMovie, namedMovies.get(1));

        // Test adding null movie (should be ignored)
        player.addNamedMovie(null);
        assertEquals(2, player.getNamedMovies().size());
    }

    /**
     * Tests the getWinCondition method.
     */
    @Test
    public void testGetWinCondition() {
        assertSame(actionWinCondition, player.getWinCondition());
    }

    /**
     * Tests the hasWon method with Action genre win condition.
     */
    @Test
    public void testHasWonWithActionGenre() {
        // Initial state should be not won
        assertFalse(player.hasWon());

        // Add a qualifying movie (Action genre)
        player.addNamedMovie(avatarMovie);  // Avatar
        assertFalse(player.hasWon());  // Only 1 movie, doesn't meet 5 movie requirement

        // Add more qualifying movies
        player.addNamedMovie(darkKnightMovie);  // The Dark Knight
        player.addNamedMovie(inceptionMovie);   // Inception
        assertFalse(player.hasWon());  // Only 3 movies, doesn't meet 5 movie requirement

        // Create two more action movies
        Movie jurassicWorld = new Movie(135397, "Jurassic World", 2015, Arrays.asList("Action", "Adventure", "Science Fiction", "Thriller"));
        Movie madMaxFuryRoad = new Movie(76341, "Mad Max: Fury Road", 2015, Arrays.asList("Action", "Adventure", "Science Fiction", "Thriller"));

        // Add these movies
        player.addNamedMovie(jurassicWorld);
        player.addNamedMovie(madMaxFuryRoad);

        // Should now satisfy win condition
        assertTrue(player.hasWon());
    }

    /**
     * Tests the hasWon method with Drama genre win condition.
     */
    @Test
    public void testHasWonWithDramaGenre() {
        // Create a drama player
        Player dramaPlayer = new Player("Drama Player", dramaWinCondition);

        // Initial state should be not won
        assertFalse(dramaPlayer.hasWon());

        // Add a non-qualifying movie
        dramaPlayer.addNamedMovie(pulpFictionMovie);  // Pulp Fiction (no Drama tag)
        assertFalse(dramaPlayer.hasWon());

        // Add qualifying movies
        dramaPlayer.addNamedMovie(titanicMovie);  // Titanic (Drama, Romance)
        dramaPlayer.addNamedMovie(darkKnightMovie);  // The Dark Knight (includes Drama)
        assertFalse(dramaPlayer.hasWon());  // Only 2 movies, doesn't meet 3 movie requirement

        // Create another drama movie
        Movie fightClub = new Movie(550, "Fight Club", 1999, Arrays.asList("Drama"));

        // Add this movie
        dramaPlayer.addNamedMovie(fightClub);

        // Should now satisfy win condition
        assertTrue(dramaPlayer.hasWon());
    }

    /**
     * Tests the getProgressDescription method.
     */
    @Test
    public void testGetProgressDescription() {
        // Initial progress should be 0/5
        String expectedInitialDescription = "0/5 - First to name 5 action movie(s)";
        assertEquals(expectedInitialDescription, player.getProgressDescription());

        // Add a qualifying movie
        player.addNamedMovie(avatarMovie);  // Avatar (includes Action)

        // Progress should now be 1/5
        String expectedUpdatedDescription = "1/5 - First to name 5 action movie(s)";
        assertEquals(expectedUpdatedDescription, player.getProgressDescription());

        // Add a non-qualifying movie
        player.addNamedMovie(titanicMovie);  // Titanic (no Action tag)

        // Progress should still be 1/5
        assertEquals(expectedUpdatedDescription, player.getProgressDescription());

        // Add another qualifying movie
        player.addNamedMovie(darkKnightMovie);  // The Dark Knight (includes Action)

        // Progress should now be 2/5
        expectedUpdatedDescription = "2/5 - First to name 5 action movie(s)";
        assertEquals(expectedUpdatedDescription, player.getProgressDescription());
    }

    /**
     * Tests the getLastNamedMovie method.
     */
    @Test
    public void testGetLastNamedMovie() {
        // Initial should be null as no movies have been named
        assertNull(player.getLastNamedMovie());

        // Add a movie
        player.addNamedMovie(avatarMovie);
        assertEquals(avatarMovie, player.getLastNamedMovie());

        // Add a second movie
        player.addNamedMovie(darkKnightMovie);
        assertEquals(darkKnightMovie, player.getLastNamedMovie());

        // Add a third movie
        player.addNamedMovie(titanicMovie);
        assertEquals(titanicMovie, player.getLastNamedMovie());
    }

    /**
     * Tests the resetScore method.
     */
    @Test
    public void testResetScore() {
        // Add score first
        player.addScore(5);
        assertEquals(5, player.getScore());

        // Reset score
        player.resetScore();
        assertEquals(0, player.getScore());
    }

    /**
     * Tests the resetNamedMovies method.
     */
    @Test
    public void testResetNamedMovies() {
        // Add some movies
        player.addNamedMovie(avatarMovie);
        player.addNamedMovie(darkKnightMovie);
        assertEquals(2, player.getNamedMovies().size());

        // Reset named movies
        player.resetNamedMovies();
        assertTrue(player.getNamedMovies().isEmpty());
        assertNull(player.getLastNamedMovie());
    }

    /**
     * Tests the toString method.
     */
    @Test
    public void testToString() {
        String expected = "Player: Test Player | Score: 0 | Progress: 0/5 - First to name 5 action movie(s)";
        assertEquals(expected, player.toString());

        // Add a qualifying movie
        player.addNamedMovie(avatarMovie);  // Avatar (includes Action)

        // Test after state change
        player.addScore(1);
        expected = "Player: Test Player | Score: 1 | Progress: 1/5 - First to name 5 action movie(s)";
        assertEquals(expected, player.toString());
    }

    /**
     * Tests the constructor with null name parameter.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullName() {
        new Player(null, actionWinCondition);
    }

    /**
     * Tests the constructor with null win condition parameter.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullWinCondition() {
        new Player(PLAYER_NAME, null);
    }

    /**
     * Tests GenreWinCondition behavior and interaction with Player.
     */
    @Test
    public void testGenreWinConditionBehavior() {
        // Add a qualifying movie
        player.addNamedMovie(avatarMovie);  // Avatar (includes Action)
        assertEquals(1, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());

        // Add a non-qualifying movie
        player.addNamedMovie(titanicMovie);  // Titanic (no Action tag)
        assertEquals(1, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());

        // Add another qualifying movie
        player.addNamedMovie(darkKnightMovie);  // The Dark Knight (includes Action)
        assertEquals(2, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());

        // Add a third qualifying movie
        player.addNamedMovie(inceptionMovie);   // Inception (includes Action)
        assertEquals(3, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());
    }

    /**
     * Tests win condition behavior with mixed genre movies.
     */
    @Test
    public void testMixedGenresAndWinCondition() {
        // Add mixed genre movies
        player.addNamedMovie(avatarMovie);      // Avatar (includes Action)
        player.addNamedMovie(titanicMovie);     // Titanic (no Action tag)
        player.addNamedMovie(darkKnightMovie);  // The Dark Knight (includes Action)
        player.addNamedMovie(pulpFictionMovie); // Pulp Fiction (no Action tag)
        player.addNamedMovie(inceptionMovie);   // Inception (includes Action)

        // Should be two movies short of winning
        assertEquals(3, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());
        assertFalse(player.hasWon());

        // Add two more action movies
        Movie theAvengers = new Movie(24428, "The Avengers", 2012, Arrays.asList("Action", "Adventure", "Science Fiction"));
        Movie starWars = new Movie(11, "Star Wars", 1977, Arrays.asList("Action", "Adventure", "Science Fiction"));

        player.addNamedMovie(theAvengers);
        player.addNamedMovie(starWars);

        // Should now reach win condition
        assertEquals(5, ((GenreWinCondition)player.getWinCondition()).getCurrentCount());
        assertTrue(player.hasWon());
    }
    /**
     * Tests for the extra time functionality
     */
    @Test
    public void testHasExtraTimeAvailable() {
        // A new player should have extra time available
        assertTrue("New player should have extra time available", player.hasExtraTimeAvailable());

        // Create another player to ensure initial state is consistent
        Player anotherPlayer = new Player("Another Player", actionWinCondition);
        assertTrue("Another player should also have extra time available", anotherPlayer.hasExtraTimeAvailable());
    }

    /**
     * Tests the useExtraTime method.
     */
    @Test
    public void testUseExtraTime() {
        // Initial state: player has extra time
        assertTrue("Player should initially have extra time available", player.hasExtraTimeAvailable());

        // Use the extra time
        player.useExtraTime();

        // After using extra time, hasExtraTimeAvailable should return false
        assertFalse("Player should not have extra time after using it", player.hasExtraTimeAvailable());

        // Using extra time again should have no effect on the state
        player.useExtraTime();
        assertFalse("Player should still not have extra time after using it again", player.hasExtraTimeAvailable());
    }

    /**
     * Tests that each player's extra time is managed independently.
     */
    @Test
    public void testExtraTimeIndependence() {
        // Create two players
        Player player1 = new Player("Player 1", actionWinCondition);
        Player player2 = new Player("Player 2", dramaWinCondition);

        // Both players should initially have extra time
        assertTrue("Player 1 should have extra time", player1.hasExtraTimeAvailable());
        assertTrue("Player 2 should have extra time", player2.hasExtraTimeAvailable());

        // Player 1 uses extra time
        player1.useExtraTime();

        // Player 1 should no longer have extra time, but Player 2 should still have it
        assertFalse("Player 1 should not have extra time after using it", player1.hasExtraTimeAvailable());
        assertTrue("Player 2 should still have extra time", player2.hasExtraTimeAvailable());

        // Player 2 uses extra time
        player2.useExtraTime();

        // Now both players should not have extra time
        assertFalse("Player 1 should not have extra time", player1.hasExtraTimeAvailable());
        assertFalse("Player 2 should not have extra time after using it", player2.hasExtraTimeAvailable());
    }

    /**
     * Tests the integration of extra time with game functionality.
     */
    @Test
    public void testExtraTimeInGameContext() {
        // Create a mock game controller to test with
        GameController gameController = new GameController(new MovieIndex());

        // Add two players
        Player player1 = new Player("Player 1", actionWinCondition);
        Player player2 = new Player("Player 2", dramaWinCondition);

        gameController.addPlayer(player1);
        gameController.addPlayer(player2);

        // Set an initial movie (required for the game to start)
        gameController.setInitialMovie(avatarMovie);
        gameController.startGame();

        // Record the initial time limit
        int initialTimeLimit = gameController.getTurnTimeLimit();

        // Player 1 uses extra time
        assertTrue("Player 1 should have extra time available", player1.hasExtraTimeAvailable());
        player1.useExtraTime();
        assertFalse("Player 1 should not have extra time after using it", player1.hasExtraTimeAvailable());

        // Add time to the current turn (simulating the game controller's behavior)
        int extraTimeAmount = 60;
        gameController.addExtraTime(extraTimeAmount);

        // Verify the time was added correctly
        assertEquals("Time limit should be increased by the extra time amount",
                initialTimeLimit + extraTimeAmount, gameController.getSecondsRemaining());

        // Clean up resources
        gameController.cleanup();
    }

    /**
     * Tests that extra time state persists correctly across game actions.
     */
    @Test
    public void testExtraTimePersistence() {
        // Initialize player with movies and score
        player.addNamedMovie(avatarMovie);
        player.addScore(3);

        // Initially has extra time
        assertTrue("Player should initially have extra time", player.hasExtraTimeAvailable());

        // Use extra time
        player.useExtraTime();
        assertFalse("Player should not have extra time after using it", player.hasExtraTimeAvailable());

        // Extra time state should persist when named movies are reset
        player.resetNamedMovies();
        assertFalse("Extra time state should persist after resetNamedMovies", player.hasExtraTimeAvailable());

        // Extra time state should persist when score is reset
        player.resetScore();
        assertFalse("Extra time state should persist after resetScore", player.hasExtraTimeAvailable());

        // Create a new player to verify the initial state again
        Player freshPlayer = new Player("Fresh Player", actionWinCondition);
        assertTrue("New player should have extra time available", freshPlayer.hasExtraTimeAvailable());
    }
}
