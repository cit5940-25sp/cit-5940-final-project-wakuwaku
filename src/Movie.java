import java.util.*;

/**
 * Immutable domain object representing one film with metadata and credits.
 */
public final class Movie {
    private final int id;
    private final String title;
    private final int releaseYear;
    private final List<String> genres;
    private final EnumMap<MovieRole, Set<String>> credits;

    public Movie(int id, String title, int releaseYear, List<String> genres) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.releaseYear = releaseYear;

        // Make sure all genres are properly initialized, even if input is null
        List<String> filteredGenres = new ArrayList<>();
        if (genres != null) {
            for (String genre : genres) {
                if (genre != null && !genre.trim().isEmpty()) {
                    filteredGenres.add(genre.trim());
                }
            }
        }
        this.genres = Collections.unmodifiableList(filteredGenres);

        this.credits = new EnumMap<>(MovieRole.class);
        for (MovieRole r : MovieRole.values()) {
            credits.put(r, new HashSet<>());
        }
    }


    void addCredit(MovieRole role, String name) {
        if (role != null && name != null && !name.trim().isEmpty()) {
            credits.get(role).add(name.trim());
        }
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public List<String> getGenres() {
        return genres;
    }

    // Returns unmodifiable set of all people in the given role
    public Set<String> getPeople(MovieRole role) {
        return Collections.unmodifiableSet(credits.get(role));
    }

    @Override
    public String toString() {
        return String.format("🎬 %s (%d) — %s", title, releaseYear, String.join("/", genres));
    }
}