/**
 * MovieIndex provides efficient lookup and autocomplete capabilities
 * for a collection of Movie objects, indexed by title and genre.
 * It also enables finding connections between movies based on shared cast and crew.
 */

import java.util.*;

public class MovieIndex {
    private final Map<String, Movie> titleMap = new HashMap<>();
    private final Map<String, List<Movie>> genreMap = new HashMap<>();
    private final MovieTrie trie = new MovieTrie();

    public void addMovie(Movie movie) {
        String lowerTitle = movie.getTitle().toLowerCase();
        titleMap.put(lowerTitle, movie);
        trie.insert(movie.getTitle());

        for (String genre : movie.getGenres()) {
            if (genre != null && !genre.isEmpty()) {
                // Store only lowercase genres for consistency
                String lowerGenre = genre.toLowerCase();
                genreMap.computeIfAbsent(lowerGenre, k -> new ArrayList<>()).add(movie);
            }
        }
    }

    public Movie getByTitle(String title) {
        if (title == null) return null;
        return titleMap.get(title.toLowerCase());
    }

    public List<Movie> getByGenre(String genre) {
        if (genre == null) return Collections.emptyList();
        // Always convert to lowercase for lookup
        return genreMap.getOrDefault(genre.toLowerCase(), Collections.emptyList());
    }

    public List<String> suggestTitles(String prefix) {
        if (prefix == null) return Collections.emptyList();
        return trie.autocomplete(prefix);
    }

    public List<Movie> all() {
        return new ArrayList<>(titleMap.values());
    }

    /**
     * Finds all movies that are connected to the given movie through shared people.
     * Two movies are connected if they share an actor, director, writer, cinematographer, or composer.
     *
     * @param movie The movie to find connections for
     * @return Set of movies that share at least one person with the given movie
     */
    public Set<Movie> getConnectedMovies(Movie movie) {
        if (movie == null) return Collections.emptySet();

        Set<Movie> connectedMovies = new HashSet<>();

        // Check each role for connections
        for (MovieRole role : MovieRole.values()) {
            // Get all people in this role for the given movie
            Set<String> people = movie.getPeople(role);

            // For each person, find all other movies they appear in
            for (String person : people) {
                // Look at all movies
                for (Movie otherMovie : all()) {
                    // Skip the original movie
                    if (otherMovie.getId() == movie.getId()) continue;

                    // Check if this person appears in any role in the other movie
                    boolean isConnected = false;
                    for (MovieRole otherRole : MovieRole.values()) {
                        if (otherMovie.getPeople(otherRole).contains(person)) {
                            isConnected = true;
                            break;
                        }
                    }

                    if (isConnected) {
                        connectedMovies.add(otherMovie);
                    }
                }
            }
        }

        return connectedMovies;
    }

    /**
     * Returns the total number of movies in the index.
     *
     * @return The number of movies in the index
     */
    public int size() {
        return titleMap.size();
    }
}