import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV ↔ cast/crew credits loader for movie data.
 */
public final class CreditFactory {

    /**
     * Loads all cast and crew information from tmdb_5000_credits.csv and attaches them to
     * the appropriate Movie objects via Movie.addCredit.
     */
    public static void applyCredits(String csvPath, Map<Integer, Movie> movies) throws IOException {
        System.out.println("Loading credits from: " + csvPath);
        int loadedCreditsCount = 0;
        int skippedRowsCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_8))) {
            // 1) Read header and locate columns
            String headerLine = reader.readLine();
            if (headerLine == null) return;
            List<String> headers = MovieFactory.splitCsvLine(headerLine);

            // Find required columns
            int idIdx = headers.indexOf("movie_id");
            if (idIdx == -1) idIdx = headers.indexOf("id"); // Try alternative column name

            int castIdx = headers.indexOf("cast");
            int crewIdx = headers.indexOf("crew");

            if (idIdx == -1 || castIdx == -1 || crewIdx == -1) {
                System.err.println("Required columns missing in credits CSV. Found headers: " + headers);
                return;
            }

            System.out.println("Credit CSV columns - movie_id: " + idIdx + ", cast: " + castIdx + ", crew: " + crewIdx);

            // 2) Parse each row
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    List<String> fields = MovieFactory.splitCsvLine(line);
                    if (fields.size() <= Math.max(Math.max(idIdx, castIdx), crewIdx)) {
                        skippedRowsCount++;
                        continue;
                    }

                    // Parse movie ID
                    int id = Integer.parseInt(fields.get(idIdx).trim());
                    Movie movie = movies.get(id);
                    if (movie == null) {
                        skippedRowsCount++;
                        continue;
                    }

                    // Process cast (actors)
                    String castJson = fields.get(castIdx);
                    List<String> actorNames = extractNames(castJson);
                    for (String name : actorNames) {
                        movie.addCredit(MovieRole.ACTOR, name);
                    }

                    // Process crew (directors, writers, etc.)
                    String crewJson = fields.get(crewIdx);
                    Map<String, String> jobToPerson = extractJobsAndNames(crewJson);

                    for (Map.Entry<String, String> entry : jobToPerson.entrySet()) {
                        String job = entry.getKey();
                        String person = entry.getValue();

                        MovieRole role = MovieRole.fromJob(job);
                        if (role != null && person != null && !person.trim().isEmpty()) {
                            movie.addCredit(role, person.trim());
                        }
                    }

                    loadedCreditsCount++;
                } catch (Exception e) {
                    skippedRowsCount++;
                    System.err.println("Error processing credits row: " + e.getMessage());
                }
            }
        }

        System.out.println("Credits loaded: " + loadedCreditsCount + ", Skipped rows: " + skippedRowsCount);

        // Verify some credits were loaded
        int moviesWithActors = 0;
        for (Movie m : movies.values()) {
            if (!m.getPeople(MovieRole.ACTOR).isEmpty()) {
                moviesWithActors++;
            }
        }
        System.out.println("Movies with actors: " + moviesWithActors);
    }

    /**
     * Extract person names from JSON-like cast string.
     * Handles both formats: with quotes ({"name":"Name"}) and without quotes ({name: Name})
     */
    private static List<String> extractNames(String json) {
        List<String> names = new ArrayList<>();
        if (json == null || json.isBlank()) return names;

        // Try to extract with "name": "VALUE" format (with quotes)
        names.addAll(extractWithPattern(json, "\"name\":", "\""));

        // If nothing found, try name: VALUE format (without quotes)
        if (names.isEmpty()) {
            names.addAll(extractWithPattern(json, "name:", ",}"));
        }

        return names;
    }

    /**
     * Extracts values using a specific pattern from JSON-like string.
     * @param json The JSON-like string to parse
     * @param keyPattern The pattern that precedes the value (e.g., "name": or name:)
     * @param endChars Possible characters that could mark the end of the value
     * @return List of extracted values
     */
    private static List<String> extractWithPattern(String json, String keyPattern, String endChars) {
        List<String> values = new ArrayList<>();
        int pos = 0;

        while ((pos = json.indexOf(keyPattern, pos)) != -1) {
            pos += keyPattern.length();

            // Skip whitespace and opening quotes if present
            while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == '"')) {
                pos++;
            }

            // Find the end of the value
            int endPos = -1;
            for (char endChar : endChars.toCharArray()) {
                int tempEnd = json.indexOf(endChar, pos);
                if (tempEnd != -1 && (endPos == -1 || tempEnd < endPos)) {
                    endPos = tempEnd;
                }
            }

            if (endPos == -1) {
                // If no end character found, try next closing bracket
                endPos = json.indexOf('}', pos);
            }

            if (endPos > pos) {
                String value = json.substring(pos, endPos).trim();
                // Remove closing quotes if present
                if (value.endsWith("\"")) {
                    value = value.substring(0, value.length() - 1);
                }
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }

            // Move position forward
            pos = (endPos == -1) ? (pos + 1) : (endPos + 1);
        }

        return values;
    }

    /**
     * Extract job-person pairs from JSON-like crew string.
     * Handles both quoted and unquoted formats.
     */
    private static Map<String, String> extractJobsAndNames(String json) {
        Map<String, String> jobToPerson = new HashMap<>();
        if (json == null || json.isBlank()) return jobToPerson;

        // Look for job and name pairs in the JSON
        int pos = 0;
        while (pos < json.length()) {
            // First find a job entry
            int jobPos = json.indexOf("job", pos);
            if (jobPos == -1) break;

            // Find the job value
            int jobValueStart = -1;
            int jobValueEnd = -1;

            // Check for quoted format "job":"value"
            int quotedStart = json.indexOf(":", jobPos);
            if (quotedStart != -1) {
                quotedStart++;
                while (quotedStart < json.length() && Character.isWhitespace(json.charAt(quotedStart))) {
                    quotedStart++;
                }

                if (quotedStart < json.length() && json.charAt(quotedStart) == '"') {
                    jobValueStart = quotedStart + 1;
                    jobValueEnd = json.indexOf('"', jobValueStart);
                } else {
                    // Unquoted format job: value
                    jobValueStart = quotedStart;
                    jobValueEnd = findEndOfValue(json, jobValueStart);
                }
            }

            if (jobValueStart == -1 || jobValueEnd == -1) {
                pos = jobPos + 3;
                continue;
            }

            String job = json.substring(jobValueStart, jobValueEnd).trim();

            // Now look for the corresponding name
            int namePos = json.indexOf("name", jobValueEnd);
            // If we don't find a name before the next job, skip this entry
            int nextJobPos = json.indexOf("job", jobValueEnd);
            if (namePos == -1 || (nextJobPos != -1 && namePos > nextJobPos)) {
                pos = jobValueEnd + 1;
                continue;
            }

            // Find the name value
            int nameValueStart = -1;
            int nameValueEnd = -1;

            // Check for quoted format "name":"value"
            int nameColonPos = json.indexOf(":", namePos);
            if (nameColonPos != -1) {
                nameColonPos++;
                while (nameColonPos < json.length() && Character.isWhitespace(json.charAt(nameColonPos))) {
                    nameColonPos++;
                }

                if (nameColonPos < json.length() && json.charAt(nameColonPos) == '"') {
                    nameValueStart = nameColonPos + 1;
                    nameValueEnd = json.indexOf('"', nameValueStart);
                } else {
                    // Unquoted format name: value
                    nameValueStart = nameColonPos;
                    nameValueEnd = findEndOfValue(json, nameValueStart);
                }
            }

            if (nameValueStart != -1 && nameValueEnd != -1) {
                String name = json.substring(nameValueStart, nameValueEnd).trim();
                jobToPerson.put(job, name);
            }

            // Move position forward
            pos = (nameValueEnd == -1) ? (namePos + 4) : (nameValueEnd + 1);
        }

        return jobToPerson;
    }

    /**
     * Find the end of an unquoted value in JSON-like string.
     */
    private static int findEndOfValue(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']') {
                return i;
            }
        }
        return json.length();
    }
}