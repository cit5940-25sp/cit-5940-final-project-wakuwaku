import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameViewTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testDisplayGenreOptions_prints20Genres() {
        // exactly the 20 genres
        Set<String> genres = new LinkedHashSet<>(Set.of(
                "Action", "Adventure", "Animation", "Comedy", "Crime",
                "Documentary", "Drama", "Family", "Fantasy", "Foreign",
                "History", "Horror", "Music", "Mystery", "Romance",
                "Science Fiction", "Thriller", "TV Movie", "War", "Western"
        ));

        // 2) Exercise the printing method
        GameView view = new GameView();
        view.displayGenreOptions(genres);

        // 3) Grab the second line (the comma-joined genres)
        String[] lines = outContent.toString().split("\\r?\\n");
        // lines[0] == "Here are your genre options:"
        // lines[1] == "Action, Adventure, ..., Western"
        String genreLine = lines[1];

        // 4) Split on ", " and verify count
        String[] items = genreLine.split(", ");
        assertEquals(20, items.length, "Expected exactly 20 genres to be printed");
    }
}

