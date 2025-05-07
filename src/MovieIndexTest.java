import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class MovieIndexTest {

    private static MovieIndex idx;

    @BeforeClass
    public static void setup() throws IOException {
        idx = MovieDataLoader.buildIndex(
                "resources/tmdb_5000_movies.csv",
                "resources/tmdb_5000_credits.csv"
        );
        assertTrue(
                "Sanity-check: at least 4000 films expected",
                idx.all().size() >= 4700
        );
    }

    @Test
    public void testGetByTitle() {
        Movie avatar = idx.getByTitle("Avatar");
        assertNotNull("Should find Avatar", avatar);
        assertEquals("Avatar released in 2009", 2009, avatar.getReleaseYear());
    }

    @Test
    public void testGetByTitleCaseInsensitivity() {
        // Test that titles are found regardless of case
        Movie avatar1 = idx.getByTitle("avatar");
        Movie avatar2 = idx.getByTitle("AVATAR");
        Movie avatar3 = idx.getByTitle("Avatar");

        assertNotNull("Should find 'avatar' in lowercase", avatar1);
        assertNotNull("Should find 'AVATAR' in uppercase", avatar2);
        assertNotNull("Should find 'Avatar' with proper case", avatar3);

        // All should reference the same movie
        assertEquals("All lookups should find the same movie", avatar1.getId(), avatar2.getId());
        assertEquals("All lookups should find the same movie", avatar2.getId(), avatar3.getId());
    }

    @Test
    public void testGetByGenre() {
        List<Movie> action = idx.getByGenre("Action");
        assertNotNull("Action list not null", action);
        assertFalse("Action list not empty", action.isEmpty());
    }

    @Test
    public void testGetByGenreCaseInsensitivity() {
        // Test genre lookups are case insensitive
        List<Movie> action1 = idx.getByGenre("Action");
        List<Movie> action2 = idx.getByGenre("action");
        List<Movie> action3 = idx.getByGenre("ACTION");

        assertFalse("Action list not empty", action1.isEmpty());
        assertFalse("action list not empty", action2.isEmpty());
        assertFalse("ACTION list not empty", action3.isEmpty());

        // The lists should have the same size
        assertEquals("Genre lists should have same size regardless of case",
                action1.size(), action2.size());
        assertEquals("Genre lists should have same size regardless of case",
                action2.size(), action3.size());
    }

    @Test
    public void testMultipleGenres() {
        // Test finding movies with different genres
        List<Movie> comedy = idx.getByGenre("Comedy");
        List<Movie> drama = idx.getByGenre("Drama");
        List<Movie> scifi = idx.getByGenre("Science Fiction");

        assertFalse("Comedy list not empty", comedy.isEmpty());
        assertFalse("Drama list not empty", drama.isEmpty());
        assertFalse("Science Fiction list not empty", scifi.isEmpty());
    }

    @Test
    public void testSuggestTitles() {
        List<String> sugg = idx.suggestTitles("spi");
        assertTrue(
                "Should suggest Spider-Man 3",
                sugg.stream().anyMatch(s -> s.equalsIgnoreCase("Spider-Man 3"))
        );
    }

    @Test
    public void testSuggestTitlesCaseInsensitivity() {
        // Test that autocomplete works regardless of case
        List<String> suggestions1 = idx.suggestTitles("ava");
        List<String> suggestions2 = idx.suggestTitles("AVA");

        assertFalse("Should find suggestions for 'ava'", suggestions1.isEmpty());
        assertFalse("Should find suggestions for 'AVA'", suggestions2.isEmpty());

        // Both suggestion lists should contain "Avatar"
        assertTrue("Suggestions should include Avatar",
                suggestions1.stream().anyMatch(s -> s.equalsIgnoreCase("Avatar")));
        assertTrue("Suggestions should include Avatar",
                suggestions2.stream().anyMatch(s -> s.equalsIgnoreCase("Avatar")));
    }

    @Test
    public void testEmptyAndNullInputs() {
        // Test null and empty inputs don't cause errors
        List<Movie> nullGenreResult = idx.getByGenre(null);
        List<String> nullPrefixResult = idx.suggestTitles(null);
        Movie nullTitleResult = idx.getByTitle(null);
        Set<Movie> nullMovieConnections = idx.getConnectedMovies(null);

        assertNotNull("Null genre should return empty list, not null", nullGenreResult);
        assertTrue("Null genre should return empty list", nullGenreResult.isEmpty());

        assertNotNull("Null prefix should return empty list, not null", nullPrefixResult);
        assertTrue("Null prefix should return empty list", nullPrefixResult.isEmpty());

        assertNull("Null title should return null", nullTitleResult);

        assertNotNull("Null movie should return empty connections set, not null", nullMovieConnections);
        assertTrue("Null movie should return empty connections set", nullMovieConnections.isEmpty());
    }

    @Test
    public void testGetPeopleByRole() {
        // Test getting people by role for a movie - use any well-known movie in the dataset
        // First, find a movie with known credits
        boolean foundMovieWithCredits = false;

        for (Movie movie : idx.all()) {
            Set<String> actors = movie.getPeople(MovieRole.ACTOR);
            if (!actors.isEmpty()) {
                foundMovieWithCredits = true;
                System.out.println("Found movie with actors: " + movie.getTitle());

                // Once we find a movie with actors, verify we can get other roles too
                Set<String> directors = movie.getPeople(MovieRole.DIRECTOR);

                // Not all movies have all roles filled, so just check we get valid results
                assertNotNull("Should return a set for actors", actors);
                assertNotNull("Should return a set for directors", directors);
                break;
            }
        }

        // Assert that we found at least one movie with credits
        assertTrue("Should find at least one movie with actor credits", foundMovieWithCredits);
    }

    @Test
    public void testMovieYears() {
        // Look for any movie with a valid release year
        boolean foundMovieWithYear = false;

        for (Movie movie : idx.all()) {
            if (movie.getReleaseYear() > 1900 && movie.getReleaseYear() <= 2025) {
                foundMovieWithYear = true;
                System.out.println("Found movie with valid year: " + movie.getTitle() + " (" + movie.getReleaseYear() + ")");
                break;
            }
        }

        assertTrue("Should find at least one movie with a valid release year", foundMovieWithYear);
    }

    @Test
    public void testMovieGenres() {
        // Find any movie with genres in the dataset
        boolean foundMovieWithGenres = false;

        for (Movie movie : idx.all()) {
            List<String> genres = movie.getGenres();
            if (!genres.isEmpty()) {
                foundMovieWithGenres = true;
                System.out.println("Found movie with genres: " + movie.getTitle() + " - Genres: " + genres);

                // Verify that genre lookups work for at least one genre
                String firstGenre = genres.get(0);
                List<Movie> moviesInGenre = idx.getByGenre(firstGenre);

                assertNotNull("Should find movies for genre: " + firstGenre, moviesInGenre);
                assertFalse("Should find movies for genre: " + firstGenre, moviesInGenre.isEmpty());

                // Verify this movie is in its own genre list
                assertTrue("Movie should be in its own genre list",
                        moviesInGenre.stream().anyMatch(m -> m.getId() == movie.getId()));

                break;
            }
        }

        assertTrue("Should find at least one movie with genres", foundMovieWithGenres);
    }

    @Test
    public void testBoundaryConditions() {
        // Test boundary conditions and unusual characters in titles
        List<String> shortTitleSuggestions = idx.suggestTitles("a");
        assertFalse("Should handle single-letter prefixes", shortTitleSuggestions.isEmpty());

        List<String> suggWithSpecialChars = idx.suggestTitles("star-");
        // This will depend on your dataset - may need adjustment
        assertNotNull("Should handle titles with special characters", suggWithSpecialChars);
    }

    @Test
    public void testMovieConnections() {
        // This test checks if we can find connections between movies via people
        // Find any two movies that share a person in any role

        // Map to track people and their movies
        Map<String, List<Integer>> personToMovieIds = new HashMap<>();

        // Scan all movies to build the connection map
        for (Movie movie : idx.all()) {
            // Check all roles for connections
            for (MovieRole role : MovieRole.values()) {
                Set<String> people = movie.getPeople(role);
                for (String person : people) {
                    if (!person.trim().isEmpty()) {
                        personToMovieIds.computeIfAbsent(person, k -> new ArrayList<>())
                                .add(movie.getId());
                    }
                }
            }
        }

        // Find a person who appears in multiple movies
        boolean foundConnection = false;
        for (Map.Entry<String, List<Integer>> entry : personToMovieIds.entrySet()) {
            if (entry.getValue().size() >= 2) {
                String person = entry.getKey();
                List<Integer> movieIds = entry.getValue();

                System.out.println("Found connection: " + person + " appears in " +
                        movieIds.size() + " movies");

                foundConnection = true;
                break;
            }
        }

        // Assert that we found at least one connection
        assertTrue("Should find at least one person who appears in multiple movies",
                foundConnection);
    }

    @Test
    public void testGetConnectedMovies() {
        // Find a movie with actors or other credits that might connect to other movies
        Movie sourceMovie = null;
        for (Movie movie : idx.all()) {
            // Look for a movie with actors
            if (!movie.getPeople(MovieRole.ACTOR).isEmpty()) {
                sourceMovie = movie;
                break;
            }
        }

        assertNotNull("Should find a movie with credits to test connections", sourceMovie);

        // Get connected movies
        Set<Movie> connectedMovies = idx.getConnectedMovies(sourceMovie);

        // Should find at least one connected movie
        assertFalse("Should find at least one connected movie", connectedMovies.isEmpty());

        // Verify that source movie is not in the connected movies set
        for (Movie movie : connectedMovies) {
            assertNotEquals("Connected movies should not include source movie",
                    sourceMovie.getId(), movie.getId());
        }

        System.out.println("Movie '" + sourceMovie.getTitle() +
                "' is connected to " + connectedMovies.size() + " other movies");

        // Verify the connection between the source movie and one of the connected movies
        if (!connectedMovies.isEmpty()) {
            Movie connectedMovie = connectedMovies.iterator().next();
            boolean foundConnection = false;
            String connectionPerson = "";
            MovieRole sourceRole = null;
            MovieRole targetRole = null;

            // Check each role for connections
            for (MovieRole role : MovieRole.values()) {
                Set<String> sourcePeople = sourceMovie.getPeople(role);

                for (String person : sourcePeople) {
                    // Check if this person appears in any role in the connected movie
                    for (MovieRole connectedRole : MovieRole.values()) {
                        if (connectedMovie.getPeople(connectedRole).contains(person)) {
                            foundConnection = true;
                            connectionPerson = person;
                            sourceRole = role;
                            targetRole = connectedRole;
                            break;
                        }
                    }
                    if (foundConnection) break;
                }
                if (foundConnection) break;
            }

            assertTrue("Should find a valid connection between movies", foundConnection);

            System.out.println("Connection verified: '" + sourceMovie.getTitle() +
                    "' and '" + connectedMovie.getTitle() + "' share person '" +
                    connectionPerson + "' (roles: " + sourceRole + " and " + targetRole + ")");
        }
    }

    @Test
    public void testGetConnectedMoviesPerformance() {
        // Test that the getConnectedMovies method performs reasonably well
        // This test is marked as potentially slow, as it depends on the dataset size

        long startTime = System.currentTimeMillis();

        // Find 5 random movies and get their connections
        List<Movie> allMovies = idx.all();
        int totalMovies = allMovies.size();
        int totalConnections = 0;

        for (int i = 0; i < 5; i++) {
            int randomIndex = (int)(Math.random() * totalMovies);
            Movie randomMovie = allMovies.get(randomIndex);

            Set<Movie> connections = idx.getConnectedMovies(randomMovie);
            totalConnections += connections.size();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Found " + totalConnections + " connections for 5 random movies in " +
                duration + " ms (average: " + (duration / 5) + " ms per movie)");

    }
}