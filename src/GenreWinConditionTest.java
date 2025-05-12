import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GenreWinConditionTest {

    private GenreWinCondition genreCondition;
    private Movie horrorMovie;
    private Movie comedyMovie;
    private Movie mixedMovie;

    //testing the genrewincondition class

    @BeforeEach
    void setUp() {
        genreCondition = new GenreWinCondition("Horror", 5);
        horrorMovie = new Movie(1, "Scary Night", 2020, List.of("Horror", "Thriller"));
        comedyMovie = new Movie(2, "Funny Day", 2021, List.of("Comedy", "Family"));
        mixedMovie = new Movie(3, "Mixed Feelings", 2022, List.of("Horror", "Comedy"));
    }

    @Test
    void testInitialization() {
        assertEquals(true, genreCondition.getDescription().toLowerCase().contains("horror"));
        assertEquals(false, genreCondition.getDescription().toLowerCase().contains("action"));
        assertEquals(false, genreCondition.getDescription().toLowerCase().contains("comedy"));
        assertEquals(5, genreCondition.getTargetCount());
        assertEquals(0, genreCondition.getCurrentCount());
    }

    @Test
    void testRecordMovieMatchingGenre() {
        genreCondition.recordMovie(horrorMovie);
        assertEquals(1, genreCondition.getCurrentCount());

        genreCondition.recordMovie(mixedMovie);
        assertEquals(2, genreCondition.getCurrentCount());
    }

    @Test
    void testRecordMovieNonMatchingGenre() {
        genreCondition.recordMovie(comedyMovie);
        assertEquals(0, genreCondition.getCurrentCount());
    }

    @Test
    void testIsSatisfiedFalse() {
        genreCondition.recordMovie(horrorMovie);
        genreCondition.recordMovie(mixedMovie);
        assertFalse(genreCondition.isSatisfied());
    }

    @Test
    void testIsSatisfiedTrue() {
        for (int i = 0; i < 5; i++) {
            genreCondition.recordMovie(horrorMovie);
        }
        assertTrue(genreCondition.isSatisfied());
    }

    @Test
    void testDescriptionFormat() {
        String expected = "First to name 5 horror movie(s)";
        assertEquals(expected, genreCondition.getDescription());
    }

    @Test
    void testCaseInsensitiveGenreMatching() {
        GenreWinCondition genreConditionLower = new GenreWinCondition("horror", 3);
        genreConditionLower.recordMovie(horrorMovie);
        assertEquals(1, genreConditionLower.getCurrentCount());

        GenreWinCondition genreConditionMixed = new GenreWinCondition("HoRrOr", 3);
        genreConditionMixed.recordMovie(mixedMovie);
        assertEquals(1, genreConditionMixed.getCurrentCount());
    }

    @Test
    void testEdgeCaseEmptyGenre() {
        GenreWinCondition emptyCondition = new GenreWinCondition("", 3);
        assertEquals(0, emptyCondition.getCurrentCount());
        emptyCondition.recordMovie(horrorMovie);
        assertEquals(0, emptyCondition.getCurrentCount());
    }
}

