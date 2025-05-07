/**
 * Enum representing all possible roles that can link two movies together.
 */
public enum MovieRole {
    ACTOR,
    DIRECTOR,
    WRITER,
    CINEMATOGRAPHER,
    COMPOSER;

    /**
     * Maps the original TMDB job string to our standardized MovieRole enum.
     */
    public static MovieRole fromJob(String job) {
        if (job == null || job.trim().isEmpty()) return null;
        String j = job.toLowerCase().trim();
        return switch (j) {
            case "actor", "actress", "cast" -> ACTOR;
            case "director" -> DIRECTOR;
            case "writer", "screenplay", "story", "story by", "screenplay by" -> WRITER;
            case "director of photography", "cinematographer", "cinematography" -> CINEMATOGRAPHER;
            case "original music composer", "music", "composer", "music by" -> COMPOSER;
            default -> null;
        };
    }
}