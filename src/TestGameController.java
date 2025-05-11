import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;
import java.util.*;

public class TestGameController {
    private GameController controller;
    private MovieIndex movieIndex;
    private Player player1, player2;
    private Movie avatarMovie, darkKnightMovie, titanicMovie, inceptionMovie, pulpFictionMovie;

    @Before
    public void setUp() {
        // Create a test MovieIndex
        movieIndex = new MovieIndex();

        // Create test movies - using lowercase genre names
        avatarMovie = createTestMovie(19995, "Avatar", 2009, Arrays.asList("action", "adventure", "fantasy", "sci-fi"));
        darkKnightMovie = createTestMovie(155, "The Dark Knight", 2008, Arrays.asList("action", "crime", "drama", "thriller"));
        titanicMovie = createTestMovie(597, "Titanic", 1997, Arrays.asList("drama", "romance"));
        inceptionMovie = createTestMovie(27205, "Inception", 2010, Arrays.asList("action", "sci-fi", "adventure"));
        pulpFictionMovie = createTestMovie(680, "Pulp Fiction", 1994, Arrays.asList("thriller", "crime"));

        // Add credits to establish direct connections between movies
        avatarMovie.addCredit(MovieRole.ACTOR, "Sam Worthington");
        avatarMovie.addCredit(MovieRole.DIRECTOR, "James Cameron");

        inceptionMovie.addCredit(MovieRole.ACTOR, "Leonardo DiCaprio");
        inceptionMovie.addCredit(MovieRole.DIRECTOR, "Christopher Nolan");
        inceptionMovie.addCredit(MovieRole.ACTOR, "Sam Worthington"); // Direct connection to Avatar

        darkKnightMovie.addCredit(MovieRole.ACTOR, "Christian Bale");
        darkKnightMovie.addCredit(MovieRole.DIRECTOR, "Christopher Nolan"); // Connection to Inception

        titanicMovie.addCredit(MovieRole.ACTOR, "Leonardo DiCaprio"); // Connection to Inception
        titanicMovie.addCredit(MovieRole.DIRECTOR, "James Cameron"); // Connection to Avatar

        pulpFictionMovie.addCredit(MovieRole.ACTOR, "John Travolta");
        pulpFictionMovie.addCredit(MovieRole.DIRECTOR, "Quentin Tarantino");
        pulpFictionMovie.addCredit(MovieRole.ACTOR, "Leonardo DiCaprio"); // Connection to Inception/Titanic

        // Add movies
        movieIndex.addMovie(avatarMovie);
        movieIndex.addMovie(darkKnightMovie);
        movieIndex.addMovie(titanicMovie);
        movieIndex.addMovie(inceptionMovie);
        movieIndex.addMovie(pulpFictionMovie);

        // Create controller
        controller = new GameController(movieIndex);

        // Create players with win target that matches implementation
        WinConditionStrategy sciFiWinCondition = new GenreWinCondition("sci-fi", 5);
        WinConditionStrategy actionWinCondition = new GenreWinCondition("action", 5);
        player1 = new Player("A", sciFiWinCondition);
        player2 = new Player("B", actionWinCondition);

        // Add players
        controller.addPlayer(player1);
        controller.addPlayer(player2);

        // Set initial movie
        controller.setInitialMovie(avatarMovie);
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.cleanup();
        }
    }

    /**
     * Tests set up
     */
    @Test
    public void testInitialState() {
        assertEquals(GameController.GameState.NOT_STARTED, controller.getState());
        assertNull(controller.getWinner());
        assertEquals(1, controller.getTurnCount());
    }

    /**
     * tests start game functionality
     */
    @Test
    public void testStartGame() {
        assertTrue(controller.startGame());
        assertEquals(GameController.GameState.IN_PROGRESS, controller.getState());
        assertEquals(player1, controller.getCurrentPlayer());
    }

    /**
     * Tests initial movie cannot be used
     */
    @Test
    public void testInitialMovieUsage() {
        controller.startGame();

        // should not use the initial movie (Avatar)
        boolean result = controller.processTurn("Avatar");
        assertFalse(result);
    }

    /**
     * test Process Turn with basic implementation
     */
    @Test
    public void testProcessTurn_basic() {
        controller.startGame();

        boolean resultInception = controller.processTurn("Inception");
        assertTrue(resultInception);

        // Start a new game to test with a non-matching genre movie
        controller.endGame();
        controller.startGame();

        boolean resultPulpFiction = controller.processTurn("Pulp Fiction");
        assertFalse(resultPulpFiction);
    }

    /**
     * Tests with invalid movie
     */
    @Test
    public void testInvalidMoves() {
        controller.startGame();

        // Test using an unknown movie
        boolean resultNonExistent = controller.processTurn("Non-existent Movie");
        assertFalse("Non-existent movies should be rejected", resultNonExistent);

        // Test using empty title
        boolean resultEmpty = controller.processTurn("");
        assertFalse("Empty titles should be rejected", resultEmpty);

        // Test using null title
        boolean resultNull = controller.processTurn(null);
        assertFalse("Null titles should be rejected", resultNull);
    }

    /**
     * Test get possible movie functionality
     */
    @Test
    public void testGetPossibleMovies() {
        controller.startGame();

        // Get possible movies before first turn
        Set<Movie> initialPossibleMovies = controller.getPossibleMovies();

        // Initial movie should NOT be in possible movies (since it's added to usedMovieIds)
        boolean initialMovieIncluded = initialPossibleMovies.contains(avatarMovie);
        assertFalse("Initial movie should NOT be in possible movies", initialMovieIncluded);

        // There should be at least one possible movie
        assertFalse("Should have at least one possible movie", initialPossibleMovies.isEmpty());

        // Make a move
        boolean moveResult = controller.processTurn("Inception");

        if (moveResult) {
            Set<Movie> nextPossibleMovies = controller.getPossibleMovies();

            // Used movies should not be included
            assertFalse("Initial movie should not be in possible movies after a move",
                    nextPossibleMovies.contains(avatarMovie));
            assertFalse("Used movie (Inception) should not be in possible movies",
                    nextPossibleMovies.contains(inceptionMovie));

            // Should have at least one possible movie
            assertFalse("Should have at least one possible movie after first move",
                    nextPossibleMovies.isEmpty());
        }

    }

    /**
     * Test game summary contains required information
     */
    @Test
    public void testGameSummary() {
        controller.startGame();

        // Check if game summary contains basic information
        String summary = controller.getGameSummary();

        assertTrue("Game summary should include game status",
                summary.contains("Game status") || summary.contains("Status"));
        assertTrue("Game summary should include information about turns",
                summary.contains("Turn") || summary.contains("turn"));
    }

    /**
     * Test end game logic
     */
    @Test
    public void testEndGame() {
        controller.startGame();

        // End the game early
        assertTrue("Should be able to end an in-progress game", controller.endGame());
        assertEquals("Game state should be COMPLETED after ending",
                GameController.GameState.COMPLETED, controller.getState());

        // Trying to end the game again should fail
        assertFalse("Should not be able to end a completed game", controller.endGame());
    }

    /**
     * Test one connection can be maximum used 3 times
     */
    @Test
    public void testConnectionUsageLimit() {
        controller.startGame();

        Movie movie1 = createTestMovie(1001, "James Bond 1", 2000, Arrays.asList("action", "sci-fi"));
        movie1.addCredit(MovieRole.ACTOR, "Sam Worthington");
        movieIndex.addMovie(movie1);

        Movie movie2 = createTestMovie(1002, "James Bond 2", 2001, Arrays.asList("action", "sci-fi"));
        movie2.addCredit(MovieRole.ACTOR, "Sam Worthington");
        movieIndex.addMovie(movie2);

        Movie movie3 = createTestMovie(1003, "James Bond 3", 2002, Arrays.asList("action", "sci-fi"));
        movie3.addCredit(MovieRole.ACTOR, "Sam Worthington");
        movieIndex.addMovie(movie3);

        Movie movie4 = createTestMovie(1004, "James Bond 4", 2003, Arrays.asList("action", "sci-fi"));
        movie4.addCredit(MovieRole.ACTOR, "Sam Worthington");
        movieIndex.addMovie(movie4);


        assertTrue(controller.processTurn("James Bond 1"));
        assertTrue(controller.processTurn("James Bond 2"));
        assertTrue(controller.processTurn("James Bond 3"));

        assertFalse("Should not be able to use the same connection more than 3 times",
                controller.processTurn("James Bond 4"));

        assertEquals(GameController.GameState.COMPLETED, controller.getState());
        assertEquals(player1, controller.getWinner());
    }

    /**
     * Test turn time
     */
    @Test
    public void testTurnTimeLimit() {
        controller.setTurnTimeLimit(5);
        assertEquals(5, controller.getTurnTimeLimit());

        controller.startGame();
        assertEquals(5, controller.getSecondsRemaining());

        assertTrue(controller.processTurn("Inception"));

        assertEquals(player2, controller.getCurrentPlayer());
    }

    /**
     * Test get player method
     */
    @Test
    public void testPlayerMethods() {
        assertEquals(player1, controller.getPlayer(0));
        assertEquals(player2, controller.getPlayer(1));
        assertNull(controller.getPlayer(2));

        assertEquals(player2, controller.getOpponent(player1));
        assertEquals(player1, controller.getOpponent(player2));
        assertNull(controller.getOpponent(null));

        List<Player> players = controller.getPlayers();
        assertEquals(2, players.size());
        assertTrue(players.contains(player1));
        assertTrue(players.contains(player2));
    }

    /**
     * Test win condition implemented correctly
     */
    @Test
    public void testWinCondition() {
        controller.startGame();

        for (int i = 0; i < 5; i++) {
            Movie movie = createTestMovie(2000 + i, "Sci-Fi Movie " + i, 2000 + i, Arrays.asList("sci-fi"));
            if (i == 0) {
                movie.addCredit(MovieRole.ACTOR, "Sam Worthington");
            } else {
                movie.addCredit(MovieRole.ACTOR, "Sci-Fi Actor " + (i-1));
            }
            movie.addCredit(MovieRole.ACTOR, "Sci-Fi Actor " + i);
            movieIndex.addMovie(movie);
        }

        for (int i = 0; i < 5; i++) {
            boolean result = controller.processTurn("Sci-Fi Movie " + i);
            assertTrue(result);

            if (i < 4) {
                Movie actionMovie = createTestMovie(3000 + i, "Action Movie " + i, 2000 + i, Arrays.asList("action"));
                actionMovie.addCredit(MovieRole.ACTOR, "Sci-Fi Actor " + i);
                actionMovie.addCredit(MovieRole.ACTOR, "Action Actor " + i);
                movieIndex.addMovie(actionMovie);

                boolean result2 = controller.processTurn("Action Movie " + i);
                assertTrue(result2);
            }
        }

        assertEquals(GameController.GameState.COMPLETED, controller.getState());
        assertEquals(player1, controller.getWinner());
        assertEquals(5, player1.getScore());
    }

    /**
     *  Test the connection usage functionality in the game.
     */
    @Test
    public void testConnectionUsageTracking() {
        controller.startGame();

        Movie movie1 = createTestMovie(1001, "Connected Movie 1", 2000, Arrays.asList("sci-fi"));
        movie1.addCredit(MovieRole.ACTOR, "Sam Worthington");
        movieIndex.addMovie(movie1);

        assertTrue(controller.processTurn("Connected Movie 1"));

        Map<String, Integer> connectionUsage = controller.getConnectionUsageCount();
        assertFalse(connectionUsage.isEmpty());

        boolean foundConnection = false;
        for (Map.Entry<String, Integer> entry : connectionUsage.entrySet()) {
            if (entry.getKey().contains("Sam Worthington")) {
                foundConnection = true;
                assertEquals(Integer.valueOf(1), entry.getValue());
                break;
            }
        }
        assertTrue("Should have tracked connection usage", foundConnection);
    }

    // Helper method to create test movie
    private Movie createTestMovie(int id, String title, int year, List<String> genres) {
        return new Movie(id, title, year, genres);
    }

    /**
     * Test the addExtraTime method to ensure it only affects the current turn
     */
    @Test
    public void testAddExtraTime() {
        controller.startGame();

        // Initial seconds remaining should match the turn time limit
        int initialTimeLimit = controller.getTurnTimeLimit();
        assertEquals(initialTimeLimit, controller.getSecondsRemaining());

        // Add extra time
        int extraTime = 60;
        controller.addExtraTime(extraTime);

        // Check that seconds remaining was increased but turn time limit stays unchanged
        assertEquals(initialTimeLimit + extraTime, controller.getSecondsRemaining());
        assertEquals(initialTimeLimit, controller.getTurnTimeLimit());

        // Make a move to trigger next turn
        assertTrue(controller.processTurn("Inception"));

        // Verify next turn starts with the original time limit, not the extended one
        assertEquals(initialTimeLimit, controller.getSecondsRemaining());
    }

    /**
     * Test that handleTimeExpired correctly ends the game when time runs out
     */
    @Test
    public void testHandleTimeExpired() {
        controller.startGame();

        // Initially player1 is the current player
        assertEquals(player1, controller.getCurrentPlayer());

        // Simulate time expiration
        controller.handleTimeExpired();

        // Game should be completed
        assertEquals(GameController.GameState.COMPLETED, controller.getState());

        // Player2 should be the winner (since player1's time expired)
        assertEquals(player2, controller.getWinner());
    }

    /**
     * Test starting a game with missing requirements
     */
    @Test
    public void testStartGameWithMissingRequirements() {
        // Create a new controller without players or initial movie
        GameController emptyController = new GameController(movieIndex);

        // Should not start without players
        assertFalse(emptyController.startGame());

        // Add one player but not two
        emptyController.addPlayer(player1);
        assertFalse(emptyController.startGame());

        // Add second player but no initial movie
        emptyController.addPlayer(player2);
        assertFalse(emptyController.startGame());

        // Set initial movie
        emptyController.setInitialMovie(avatarMovie);
        assertTrue(emptyController.startGame());

        // Cleanup
        emptyController.cleanup();
    }

    /**
     * Test that adding players after game has started is not allowed
     */
    @Test
    public void testAddPlayerAfterGameStarted() {
        controller.startGame();

        // Try adding another player after game has started
        Player newPlayer = new Player("C", new GenreWinCondition("comedy", 5));
        assertFalse(controller.addPlayer(newPlayer));
    }

    /**
     * Test setting initial movie after game has started
     */
    @Test
    public void testSetInitialMovieAfterGameStarted() {
        controller.startGame();

        // Initial movie should already be set
        assertNotNull(controller.getInitialMovie());

        // Try setting a different initial movie
        Movie newMovie = createTestMovie(999, "New Initial Movie", 2020, Arrays.asList("action"));
        controller.setInitialMovie(newMovie);

        // Initial movie should not change
        assertEquals(avatarMovie, controller.getInitialMovie());
    }

    /**
     * Test setting invalid turn time limit
     */
    @Test
    public void testSetInvalidTurnTimeLimit() {
        int originalLimit = controller.getTurnTimeLimit();

        // Try setting negative value
        controller.setTurnTimeLimit(-10);

        // Time limit should not change
        assertEquals(originalLimit, controller.getTurnTimeLimit());

        // Try setting zero
        controller.setTurnTimeLimit(0);

        // Time limit should not change
        assertEquals(originalLimit, controller.getTurnTimeLimit());

        // Try setting valid value
        controller.setTurnTimeLimit(60);

        // Time limit should change
        assertEquals(60, controller.getTurnTimeLimit());
    }

    /**
     * Test getting current player when game is not in progress
     */
    @Test
    public void testGetCurrentPlayerWhenNotInProgress() {
        // Game not started
        assertNull(controller.getCurrentPlayer());

        // Start and then end game
        controller.startGame();
        controller.endGame();

        // Game completed
        assertNull(controller.getCurrentPlayer());
    }

    /**
     * Test processing a turn after game is over
     */
    @Test
    public void testProcessTurnAfterGameOver() {
        controller.startGame();
        controller.endGame();

        // Try processing a turn after game has ended
        assertFalse(controller.processTurn("Inception"));
    }

    /**
     * Test finding connection between two movies
     */
    @Test
    public void testMovieConnections() {
        controller.startGame();

        // Should be able to use first valid move
        assertTrue(controller.processTurn("Inception"));

        // Next valid move should be connected to Inception
        assertTrue(controller.processTurn("Titanic"));

        // Check that the connection has been recorded
        Map<String, Integer> connections = controller.getConnectionUsageCount();
        boolean connectionFound = false;

        for (Map.Entry<String, Integer> entry : connections.entrySet()) {
            if (entry.getKey().contains("Leonardo DiCaprio")) {
                connectionFound = true;
                assertEquals(Integer.valueOf(1), entry.getValue());
                break;
            }
        }

        assertTrue("Leonardo DiCaprio connection should be recorded", connectionFound);
    }

    /**
     * Test handling multiple players with same genre
     */
    @Test
    public void testPlayersWithSameGenre() {
        // Create new controller
        GameController genreController = new GameController(movieIndex);

        // Create two players with same genre
        Player actionPlayer1 = new Player("Action Player 1", new GenreWinCondition("action", 5));
        Player actionPlayer2 = new Player("Action Player 2", new GenreWinCondition("action", 5));

        genreController.addPlayer(actionPlayer1);
        genreController.addPlayer(actionPlayer2);
        genreController.setInitialMovie(avatarMovie);
        genreController.startGame();

        // First player should be able to make valid move
        assertTrue(genreController.processTurn("Inception"));

        // Second player should be able to make valid move
        assertTrue(genreController.processTurn("The Dark Knight"));

        // Both players should get points for action movies
        assertEquals(1, actionPlayer1.getScore());
        assertEquals(1, actionPlayer2.getScore());

        // Cleanup
        genreController.cleanup();
    }

    /**
     * Test that a player can win by making valid moves
     */
    @Test
    public void testPlayerVictory() {
        controller.startGame();

        // Set up movies for player 1 (sci-fi genre)
        for (int i = 0; i < 5; i++) {
            Movie movie = createTestMovie(5000 + i, "Sci-Fi Win " + i, 2020, Arrays.asList("sci-fi"));
            if (i == 0) {
                movie.addCredit(MovieRole.ACTOR, "Sam Worthington");
            } else {
                movie.addCredit(MovieRole.ACTOR, "Win Actor " + (i-1));
            }
            movie.addCredit(MovieRole.ACTOR, "Win Actor " + i);
            movieIndex.addMovie(movie);
        }

        // Player 1 names sci-fi movies
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                // Player 2 needs to make moves in-between
                Movie actionMovie = createTestMovie(6000 + i, "Action Filler " + i, 2020, Arrays.asList("action"));
                actionMovie.addCredit(MovieRole.ACTOR, "Win Actor " + (i-1));
                movieIndex.addMovie(actionMovie);

                assertTrue(controller.processTurn("Action Filler " + i));
            }

            // Player 1's moves
            boolean result = controller.processTurn("Sci-Fi Win " + i);
            assertTrue(result);

            // Check progress after each move
            assertEquals(i + 1, player1.getWinCondition().getCurrentCount());
        }

        // Game should be over with player 1 as winner
        assertEquals(GameController.GameState.COMPLETED, controller.getState());
        assertEquals(player1, controller.getWinner());
    }

    /**
     * Test handling non-existent movie name
     */
    @Test
    public void testNonExistentMovieName() {
        controller.startGame();

        // Try to name a movie that doesn't exist in the index
        assertFalse(controller.processTurn("This Movie Does Not Exist"));

        // Game should still be in progress
        assertEquals(GameController.GameState.IN_PROGRESS, controller.getState());

        // Current player should still be player 1
        assertEquals(player1, controller.getCurrentPlayer());
    }
    /**
     * Test case where a movie has no connection to the previous movie
     */
    @Test
    public void testNoConnectionBetweenMovies() {
        controller.startGame();

        // Create a movie with no connections to Avatar
        Movie unconnectedMovie = createTestMovie(9999, "Unconnected Movie", 2020, Arrays.asList("comedy"));
        // Do not add any shared credits with previous movies
        unconnectedMovie.addCredit(MovieRole.ACTOR, "Unique Actor");
        unconnectedMovie.addCredit(MovieRole.DIRECTOR, "Unique Director");
        movieIndex.addMovie(unconnectedMovie);

        // Player tries to name an unconnected movie
        boolean result = controller.processTurn("Unconnected Movie");

        // Should fail due to no connection
        assertFalse("Movie with no connection should be rejected", result);

        // Game should be complete with player2 as winner (opponent of current player)
        assertEquals(GameController.GameState.COMPLETED, controller.getState());
        assertEquals(player2, controller.getWinner());
    }

    /**
     * Test that using a connection more than 3 times leads to game end
     */
    @Test
    public void testConnectionUsedTooManyTimes() {
        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);
        controller.startGame();

        // Create a distinctive actor that we'll use as our shared connection
        String sharedActor = "Very Unique Actor For Testing";

        // Create movie 1 - connects to Avatar through Sam Worthington
        Movie movie1 = createTestMovie(8001, "Connection Limit 1", 2020, Arrays.asList("action", "sci-fi"));
        movie1.addCredit(MovieRole.ACTOR, "Sam Worthington"); // Connection to initial movie
        movie1.addCredit(MovieRole.ACTOR, sharedActor);       // Introduce our shared actor
        movieIndex.addMovie(movie1);

        // Create movie 2 - connects to movie 1 through the shared actor
        Movie movie2 = createTestMovie(8002, "Connection Limit 2", 2020, Arrays.asList("action", "sci-fi"));
        movie2.addCredit(MovieRole.ACTOR, sharedActor);       // Reuse the shared actor (connection count: 1)
        movieIndex.addMovie(movie2);

        // Create movie 3 - connects to movie 2 through the shared actor
        Movie movie3 = createTestMovie(8003, "Connection Limit 3", 2020, Arrays.asList("action", "sci-fi"));
        movie3.addCredit(MovieRole.ACTOR, sharedActor);       // Reuse the shared actor (connection count: 2)
        movieIndex.addMovie(movie3);

        // Create movie 4 - connects to movie 3 through the shared actor
        Movie movie4 = createTestMovie(8004, "Connection Limit 4", 2020, Arrays.asList("action", "sci-fi"));
        movie4.addCredit(MovieRole.ACTOR, sharedActor);       // Reuse the shared actor (connection count: 3)
        movieIndex.addMovie(movie4);

        // Create movie 5 - one more that would exceed the limit
        Movie movie5 = createTestMovie(8005, "Connection Limit 5", 2020, Arrays.asList("action", "sci-fi"));
        movie5.addCredit(MovieRole.ACTOR, sharedActor);       // Reuse the shared actor (would be count: 4)
        movieIndex.addMovie(movie5);

        // First move - First player names movie1 (connects to initial movie)
        assertTrue("First use of connection should succeed", controller.processTurn("Connection Limit 1"));

        // Second move - Second player names movie2 (reuses shared actor, count 1)
        assertTrue("Second use of connection should succeed", controller.processTurn("Connection Limit 2"));

        // Third move - First player names movie3 (reuses shared actor, count 2)
        assertTrue("Third use of connection should succeed", controller.processTurn("Connection Limit 3"));

        // Fourth move - Second player names movie4 (reuses shared actor, count 3)
        assertTrue("Fourth use of connection should succeed", controller.processTurn("Connection Limit 4"));

        // Fifth move - First player attempts to name movie5 (would reuse shared actor, count 4)
        // This should fail as it would exceed the 3-time limit
        assertFalse("Fifth use of connection should fail (exceeds 3-time limit)", controller.processTurn("Connection Limit 5"));

        // Game should be completed with the opponent as winner
        assertEquals("Game should be completed", GameController.GameState.COMPLETED, controller.getState());
        assertEquals("Player 2 should be winner", player2, controller.getWinner());
    }

    /**
     * Tests the various branches of the winner determination in endGame
     */
    @Test
    public void testEndGameWinnerDetermination() {
        // Test case 1: Player 1 has won
        player1 = new Player("Player 1", new GenreWinCondition("action", 5));
        player2 = new Player("Player 2", new GenreWinCondition("comedy", 5));

        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);

        // Manually set player1 to winning state
        for (int i = 0; i < 5; i++) {
            Movie actionMovie = createTestMovie(7000 + i, "Action Winner " + i, 2020, Arrays.asList("action"));
            player1.addNamedMovie(actionMovie);
            player1.addScore(1);
        }

        controller.startGame();
        controller.endGame();

        assertEquals(player1, controller.getWinner());

        // Test case 2: Player 2 has won
        player1 = new Player("Player 1", new GenreWinCondition("action", 5));
        player2 = new Player("Player 2", new GenreWinCondition("comedy", 5));

        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);

        // Manually set player2 to winning state
        for (int i = 0; i < 5; i++) {
            Movie comedyMovie = createTestMovie(7100 + i, "Comedy Winner " + i, 2020, Arrays.asList("comedy"));
            player2.addNamedMovie(comedyMovie);
            player2.addScore(1);
        }

        controller.startGame();
        controller.endGame();

        assertEquals(player2, controller.getWinner());

        // Test case 3: No winner
        player1 = new Player("Player 1", new GenreWinCondition("action", 5));
        player2 = new Player("Player 2", new GenreWinCondition("comedy", 5));

        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);

        // Neither player has met win conditions
        controller.startGame();
        controller.endGame();

        assertNull(controller.getWinner());
    }

    /**
     * Test game summary generation in different game states
     */
    @Test
    public void testGameSummaryAllStates() {
        // Test summary before game starts
        String notStartedSummary = controller.getGameSummary();
        assertTrue(notStartedSummary.contains("Game status: NOT_STARTED"));

        // Test summary during game
        controller.startGame();
        String inProgressSummary = controller.getGameSummary();
        assertTrue(inProgressSummary.contains("Game status: IN_PROGRESS"));
        assertTrue(inProgressSummary.contains("Current player:"));
        assertTrue(inProgressSummary.contains("Time remaining:"));

        // Test summary with a winner
        player1.addScore(5);
        for (int i = 0; i < 5; i++) {
            Movie scifiMovie = createTestMovie(7200 + i, "Sci-Fi Winner " + i, 2020, Arrays.asList("sci-fi"));
            player1.addNamedMovie(scifiMovie);
        }
        controller.endGame();

        String completedSummary = controller.getGameSummary();
        assertTrue(completedSummary.contains("Game status: COMPLETED"));
        assertTrue(completedSummary.contains("Winner: " + player1.getName()));

        // Test summary with no winner
        player1 = new Player("Player 1", new GenreWinCondition("action", 5));
        player2 = new Player("Player 2", new GenreWinCondition("comedy", 5));

        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);
        controller.startGame();
        controller.endGame();

        String noWinnerSummary = controller.getGameSummary();
        assertTrue(noWinnerSummary.contains("Game ended with no winner"));
    }

    /**
     * Test connection usage display in game summary
     */
    @Test
    public void testConnectionUsageInSummary() {
        controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);
        controller.startGame();

        // Create movies with the same connection
        String sharedActor = "Recurring Connection";

        // First movie - connects to initial movie
        Movie movie1 = createTestMovie(7301, "Connection Summary 1", 2020, Arrays.asList("action", "sci-fi"));
        movie1.addCredit(MovieRole.ACTOR, "Sam Worthington"); // Connection to initial movie
        movie1.addCredit(MovieRole.ACTOR, sharedActor);       // Add shared actor
        movieIndex.addMovie(movie1);

        // Second movie - connected via shared actor
        Movie movie2 = createTestMovie(7302, "Connection Summary 2", 2020, Arrays.asList("action", "sci-fi"));
        movie2.addCredit(MovieRole.ACTOR, sharedActor);
        movieIndex.addMovie(movie2);

        // Third movie - to have a different connection available
        Movie movie3 = createTestMovie(7303, "Connection Summary 3", 2020, Arrays.asList("action", "sci-fi"));
        movie3.addCredit(MovieRole.ACTOR, "Different Actor");
        movie3.addCredit(MovieRole.ACTOR, sharedActor);
        movieIndex.addMovie(movie3);

        // Use the connection multiple times
        assertTrue(controller.processTurn("Connection Summary 1"));
        assertTrue(controller.processTurn("Connection Summary 2"));

        Map<String, Integer> connectionUsage = controller.getConnectionUsageCount();

        // verify whether the connection exists
        boolean foundConnection = false;
        for (Map.Entry<String, Integer> entry : connectionUsage.entrySet()) {
            if (entry.getKey().contains(sharedActor)) {
                foundConnection = true;
                break;
            }
        }

        assertTrue("Should find connection usage record for the shared actor", foundConnection);

        // Get the game summary
        String summary = controller.getGameSummary();
        assertTrue("Summary should include connection usage information",
                summary.contains("Connection usage counts"));
    }

    /**
     * Test empty connection usage map in game summary
     */
    @Test
    public void testEmptyConnectionUsageInSummary() {
        // New game with no connections used yet
        GameController controller = new GameController(movieIndex);
        controller.addPlayer(player1);
        controller.addPlayer(player2);
        controller.setInitialMovie(avatarMovie);
        controller.startGame();

        // No connections used yet
        String summary = controller.getGameSummary();

        // Should not contain connection usage counts section
        assertFalse(summary.contains("Connection usage counts"));

        controller.cleanup();
    }


}
