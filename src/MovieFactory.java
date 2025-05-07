import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MovieFactory {
    public static Map<Integer, Movie> loadMovies(String csvPath) throws IOException {
        Map<Integer, Movie> movieMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return movieMap;
            List<String> headers = splitCsvLine(headerLine);

            int idIdx = headers.indexOf("id");
            int titleIdx = headers.indexOf("title");
            int genresIdx = headers.indexOf("genres");
            int dateIdx = headers.indexOf("release_date");

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = splitCsvLine(line);
                if (fields.size() <= Math.max(Math.max(idIdx, titleIdx), Math.max(genresIdx, dateIdx))) continue;

                try {
                    int id = Integer.parseInt(fields.get(idIdx).trim());
                    String title = fields.get(titleIdx).trim();
                    int year = parseYear(fields.get(dateIdx));
                    List<String> genres = parseGenres(fields.get(genresIdx));

                    movieMap.put(id, new Movie(id, title, year, genres));
                } catch (Exception e) {
                    System.err.println("Skipped row: " + e.getMessage());
                }
            }
        }
        return movieMap;
    }

    static List<String> splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private static int parseYear(String date) {
        if (date == null || date.trim().length() < 4) return 0;
        try {
            return Integer.parseInt(date.trim().substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<String> parseGenres(String json) {
        List<String> genres = new ArrayList<>();
        if (json == null || json.isBlank()) return genres;

        // Format [{id: 28, name: Action}, {id: 80, name: Crime}]
        int pos = 0;
        String namePattern = "name: ";
        while ((pos = json.indexOf(namePattern, pos)) != -1) {
            pos += namePattern.length();

            // Find the end of the genre name (next comma or closing bracket)
            int commaPos = json.indexOf(',', pos);
            int bracketPos = json.indexOf('}', pos);
            int endPos;

            if (commaPos == -1) {
                endPos = bracketPos;
            } else if (bracketPos == -1) {
                endPos = commaPos;
            } else {
                endPos = Math.min(commaPos, bracketPos);
            }

            if (endPos > pos) {
                String genre = json.substring(pos, endPos).trim();
                if (!genre.isEmpty()) {
                    genres.add(genre);
                }
            }

            // Move position forward
            pos = (endPos == -1) ? (pos + 1) : (endPos + 1);
        }

        return genres;
    }
}