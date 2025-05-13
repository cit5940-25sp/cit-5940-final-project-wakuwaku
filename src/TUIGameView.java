import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * A full TUI view for the Movie Name Game:
 *  - genre selection at startup
 *  - per-turn input with 30s timer & autocomplete
 *  - live display of both players, progress, round, and history
 */
public class TUIGameView {
    private final Screen screen;
    private final Terminal terminal;
    private final MovieIndex movieIndex;
    private final GameController controller;
    private final List<String> genresList;
    private final int winTarget;
    private final int screenWidth;
    private final int screenHeight;

    // Store movie connections for display
    private final Map<Movie, String> movieConnections = new HashMap<>();

    // Mutable state during prompts
    private final StringBuilder currentInput = new StringBuilder();
    private final List<String> suggestions = new ArrayList<>();
    private volatile boolean timedOut;
    private ScheduledExecutorService timerExecutor;
    private ScheduledFuture<?> timerTask;

    /**
     * @param movieIndex  your existing MovieIndex
     * @param allGenres   set of all genres
     * @param winTarget   e.g. 5
     * @param controller  the GameController instance (for getOpponent, turnCount, timer)
     */
    public TUIGameView(MovieIndex movieIndex,
                       Set<String> allGenres,
                       int winTarget,
                       GameController controller) throws IOException {
        this.movieIndex = movieIndex;
        this.winTarget  = winTarget;
        this.controller = controller;

        this.genresList = new ArrayList<>(allGenres);
        this.genresList.sort(String.CASE_INSENSITIVE_ORDER);

        // Create a larger terminal to avoid text truncation
        terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(100, 30))
                .createTerminal();
        screen = new TerminalScreen(terminal);
        screen.startScreen();

        // Save screen dimensions for later use
        screenWidth = screen.getTerminalSize().getColumns();
        screenHeight = screen.getTerminalSize().getRows();

        // Initialize timer
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Closes the terminal and screen cleanly.
     */
    public void close() throws IOException {
        stopTimer();
        if (screen != null) {
            screen.stopScreen();
        }
        if (terminal != null) {
            terminal.close();
        }
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdownNow();
        }
    }

    /**
     * At startup: let each player pick ONE genre (with autocomplete) for a fixed winTarget.
     * @param playerName The name of the player selecting the genre
     */
    public String promptGenreSelection(String playerName) throws IOException {
        currentInput.setLength(0);
        updateGenreSuggestions();
        drawGenreScreen(playerName);

        while (true) {
            KeyStroke key = terminal.readInput();
            if (key.getKeyType() == KeyType.Character) {
                currentInput.append(key.getCharacter());
            } else if (key.getKeyType() == KeyType.Backspace && currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            } else if (key.getKeyType() == KeyType.Enter) {
                String attempt = currentInput.toString().trim();
                for (String g : genresList) {
                    if (g.equalsIgnoreCase(attempt)) {
                        return g;
                    }
                }
                currentInput.setLength(0);
                drawGenreError("Invalid genre—try again!");
            }
            updateGenreSuggestions();
            drawGenreScreen(playerName);
        }
    }

    private void updateGenreSuggestions() {
        suggestions.clear();
        String prefix = currentInput.toString().toLowerCase();
        if (!prefix.isEmpty()) {
            for (String g : genresList) {
                if (g.toLowerCase().startsWith(prefix) && suggestions.size() < 5) {
                    suggestions.add(g);
                }
            }
        }
    }

    private void drawGenreScreen(String playerName) throws IOException {
        screen.clear();
        int row = 0;

        // Use regular text instead of emoji to ensure all terminals display correctly
        printString(0, row++, "[MOVIE] Welcome to Movie Name Game! [MOVIE]");
        printString(0, row++, "");

        // Add player prompt
        String playerPrompt = playerName + ": Choose your genre";
        printString(0, row++, playerPrompt);
        printString(0, row++, "");

        // Add extra time instruction
        printString(0, row++, "You have one chance to add 60 seconds to any of your turns.");
        printString(0, row++, "To use it, simply type '++' during your turn. When you are ready, let's begin!");
        printString(0, row++, "");

        // Display long text in wrapped form to ensure it doesn't exceed screen width
        String winMessage = "Your win condition is simple: be the first to name " + winTarget + " movies in your chosen genre.";
        printWrappedString(0, row++, winMessage);

        printString(0, row++, "");
        printString(0, row++, "Here are your genre options:");

        // Split long genre list by screen width
        printWrappedString(0, row++, String.join(", ", genresList));

        row++; // Empty line
        printString(0, row++, "> " + currentInput);

        for (int i = 0; i < suggestions.size(); i++) {
            printString(2, row + i, suggestions.get(i));
        }

        screen.refresh();
    }

    private void drawGenreError(String msg) throws IOException {
        // Error message appears in a fixed position to avoid conflict with auto-completion suggestions
        int errorRow = screenHeight - 2;
        // Clear old content from this line
        clearLine(errorRow);
        // Print error message
        printString(0, errorRow, msg);
        screen.refresh();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        // Clear error message
        clearLine(errorRow);
        screen.refresh();
    }

    /**
     * Find connection between two movies
     * @return Connection description, e.g. "Shared Actor: Tom Hanks"
     */
    private String findConnection(Movie movie1, Movie movie2) {
        if (movie1 == null || movie2 == null) return "";

        // Check all role types for shared people
        for (MovieRole role : MovieRole.values()) {
            Set<String> people1 = movie1.getPeople(role);

            // Check each person's role in movie1
            for (String person : people1) {
                // Check if this person appears in any role in movie2
                for (MovieRole role2 : MovieRole.values()) {
                    if (movie2.getPeople(role2).contains(person)) {
                        return String.format("%s: %s", getRoleDescription(role, role2), person);
                    }
                }
            }
        }

        return "No connection found";
    }

    /**
     * Get descriptive text for the roles involved in the connection
     */
    private String getRoleDescription(MovieRole role1, MovieRole role2) {
        if (role1 == role2) {
            switch (role1) {
                case ACTOR: return "Shared Actor";
                case DIRECTOR: return "Shared Director";
                case WRITER: return "Shared Writer";
                case CINEMATOGRAPHER: return "Shared Cinematographer";
                case COMPOSER: return "Shared Composer";
                default: return "Shared Role";
            }
        } else {
            return String.format("%s/%s", getSimpleRoleName(role1), getSimpleRoleName(role2));
        }
    }

    /**
     * Get simplified role name
     */
    private String getSimpleRoleName(MovieRole role) {
        switch (role) {
            case ACTOR: return "Actor";
            case DIRECTOR: return "Director";
            case WRITER: return "Writer";
            case CINEMATOGRAPHER: return "Cinematographer";
            case COMPOSER: return "Composer";
            default: return "Other";
        }
    }

    /**
     * During gameplay: prompt the active player to enter a movie title.
     * Returns null on timeout, or the entered title on Enter.
     */
    public String promptMovie(Player active,
                              List<Movie> history) throws IOException {
        // init per-turn
        currentInput.setLength(0);
        suggestions.clear();
        timedOut = false;

        // Start timer
        startTimer();

        drawGameScreen(active, history);
        while (!timedOut) {
            KeyStroke key = terminal.pollInput();
            if (key == null) {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                continue;
            }
            KeyType kt = key.getKeyType();
            if (kt == KeyType.Character) {
                currentInput.append(key.getCharacter());

                // Check if player typed "++" to add extra time
                if (currentInput.toString().trim().equals("++") && active.hasExtraTimeAvailable()) {
                    active.useExtraTime();
                    controller.addExtraTime(60); // Add 60 seconds
                    currentInput.setLength(0); // Clear the "++" input
                    drawGameScreen(active, history);
                }
            } else if (kt == KeyType.Backspace && currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            } else if (kt == KeyType.Enter) {
                stopTimer();
                return currentInput.toString().trim();
            }
            updateMovieSuggestions();
            drawGameScreen(active, history);
        }

        // timeout
        stopTimer();
        return null;
    }

    /**
     * Start timer, setting timedOut to true when countdown ends
     */
    private void startTimer() {
        stopTimer();
        timedOut = false;

        timerTask = timerExecutor.scheduleAtFixedRate(() -> {
            try {
                updateTimerDisplay();
                if (controller.getSecondsRemaining() <= 0) {
                    timedOut = true;
                    stopTimer();
                }
            } catch (IOException e) {
                // Ignore display update errors
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Stop timer
     */
    private void stopTimer() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(true);
        }
    }

    /**
     * Update timer display on screen
     */
    private void updateTimerDisplay() throws IOException {

        int seconds = controller.getSecondsRemaining();
        String timerStr = "Time Remaining: " + seconds;

        // Center the timer on row 1 (below the round)
        int cols = screenWidth;
        
        // Clear only the timer area, not the entire line
        int startX = Math.max(0, (cols - timerStr.length()) / 2);
        int endX = Math.min(cols, startX + timerStr.length() + 4); // +4 for safety margin
        for (int i = startX; i < endX; i++) {
            screen.setCharacter(i, 1, new TextCharacter(' ', TextColor.ANSI.WHITE, TextColor.ANSI.BLACK));
        }
        printString((cols - timerStr.length())/2, 1, timerStr);

        screen.refresh();
    }

    /**
     * Update movie suggestions list
     */
    private void updateMovieSuggestions() {
        suggestions.clear();
        String prefix = currentInput.toString().toLowerCase();
        if (!prefix.isEmpty() && !prefix.equals("++")) {
            List<String> titles = movieIndex.suggestTitles(prefix);
            for (int i = 0; i < titles.size() && i < 5; i++) {
                suggestions.add(titles.get(i));
            }
        }
    }

    /**
     * Draw main game screen showing player info, history, and other game state
     */
    private void drawGameScreen(Player active, List<Movie> history) throws IOException {
        screen.clear();
        int cols = screenWidth;
        int round = controller.getTurnCount();

        // Header
        // Active player info (left)
        printString(0, 0, active.getName() + " (You)");
        WinConditionStrategy myC = active.getWinCondition();
        String myDesc = myC.getDescription() + " [" + myC.getCurrentCount() + "/" + myC.getTargetCount() + "]";
        printString(0, 1, myDesc);

        // Add score display - more prominently
        String scoreStr = "Score: " + active.getScore() + "/" + this.winTarget;
        printString(0, 2, scoreStr);

        // Add extra time display
        String extraTimeStr = "Add Time: " + (active.hasExtraTimeAvailable() ? "1/1" : "0/1");
        printString(0, 3, extraTimeStr);

        // Opponent info (right)
        Player other = controller.getOpponent(active);
        if (other != null) {
            String nameRight = other.getName() + " (Them)";
            printString(cols - nameRight.length(), 0, nameRight);
            WinConditionStrategy oC = other.getWinCondition();
            String oDesc = oC.getDescription() + " [" + oC.getCurrentCount() + "/" + oC.getTargetCount() + "]";
            printString(cols - oDesc.length(), 1, oDesc);

            // Add opponent score display
            String oppScoreStr = "Score: " + other.getScore() + "/" + this.winTarget;
            printString(cols - oppScoreStr.length(), 2, oppScoreStr);

            // Add opponent extra time display
            String oppExtraTimeStr = "Add Time: " + (other.hasExtraTimeAvailable() ? "1/1" : "0/1");
            printString(cols - oppExtraTimeStr.length(), 3, oppExtraTimeStr);
        }

        // Round (center)
        String roundStr = "Round: " + round;
        printString((cols - roundStr.length())/2, 0, roundStr);


        // Game status info
        printString(0, 5, "Game status: " + controller.getState());

        // History
        printString(0, 7, "Movie History (last 5):");
        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            Movie m = history.get(i);

            // Basic movie info line - includes title, year and genres
            String line = String.format("%d. %s (%d) %s",
                    i + 1,
                    m.getTitle(),
                    m.getReleaseYear(),
                    m.getGenres());

            // Ensure history doesn't exceed screen width
            if (line.length() > cols) {
                line = line.substring(0, cols - 3) + "...";
            }

            printString(0, 8 + (i - start) * 2, line);

            // Add connection information between movies
            if (i > start) {
                Movie prevMovie = history.get(i - 1);
                String connection = findConnection(prevMovie, m);

                String connectionLine = String.format("   ↑ %s", connection);
                if (connectionLine.length() > cols) {
                    connectionLine = connectionLine.substring(0, cols - 3) + "...";
                }

                printString(0, 8 + (i - start) * 2 - 1, connectionLine);
            }
        }

        // Connection Rules
        // Rules display position may need adjustment as history now takes more space
        int rulesRow = Math.max(19, 8 + (Math.min(5, history.size()) * 2) + 1);
        printString(0, rulesRow++, "Game Rules:");
        printString(0, rulesRow++, "- Each movie must be connected to the previous movie");
        printString(0, rulesRow++, "- Connections: shared actor, director, writer, etc.");
        printString(0, rulesRow++, "- Each connection can only be used 3 times");
        printString(0, rulesRow++, "- First to name " + this.winTarget + " movies in their genre wins");
        //printString(0, rulesRow++, "- Type '++' to add 60 seconds (once per player)");

        // Input & Suggestions
        int inputRow = screenHeight - 7; // Leave enough space for suggestions
        printString(0, inputRow, "Enter a movie title connected to the previous one:");
        printString(0, inputRow + 1, "> " + currentInput);

        // Clear suggestions area
        for (int i = 1; i <= 5; i++) {
            clearLine(inputRow + 2 + i);
        }

        // Display suggestions
        if (!suggestions.isEmpty()) {
            printString(0, inputRow + 2, "Suggestions:");
            for (int i = 0; i < suggestions.size(); i++) {
                printString(2, inputRow + 3 + i, suggestions.get(i));
            }
        }

        screen.refresh();
    }

    /**
     * Clear all content from specified row
     */
    private void clearLine(int row) {
        for (int i = 0; i < screenWidth; i++) {
            screen.setCharacter(i, row, new TextCharacter(' ', TextColor.ANSI.WHITE, TextColor.ANSI.BLACK));
        }
    }

    /**
     * Display wrapped text, ensuring it doesn't exceed screen width
     */
    private void printWrappedString(int col, int row, String text) {
        int maxWidth = screenWidth - col;
        if (text.length() <= maxWidth) {
            printString(col, row, text);
            return;
        }

        // Split text by words
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int currentRow = row;

        for (String word : words) {
            // If current line plus new word doesn't exceed width, add to current line
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // Otherwise print current line and start a new one
                printString(col, currentRow++, currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        // Print the last line
        if (currentLine.length() > 0) {
            printString(col, currentRow, currentLine.toString());
        }
    }

    /**
     * Place text at specified column/row, handling special characters and width limits
     */
    private void printString(int col, int row, String text) {
        // Filter out special characters that might cause display problems
        String safeText = text.replaceAll("[^\u0000-\uFFFF]", "*");

        // Ensure text doesn't exceed screen width
        int availableWidth = screenWidth - col;
        if (safeText.length() > availableWidth) {
            safeText = safeText.substring(0, availableWidth);
        }

        for (int i = 0; i < safeText.length(); i++) {
            screen.setCharacter(col + i, row,
                    new TextCharacter(safeText.charAt(i),
                            TextColor.ANSI.WHITE,
                            TextColor.ANSI.BLACK));
        }
    }

    /**
     * Game demo:
     * 1) Loads the CSVs into a MovieIndex
     * 2) Creates GameController (default winTarget=5)
     * 3) Gathers all distinct genres from the index
     * 4) Creates two players who pick their genres
     * 5) Sets up the game and plays it
     */
    public static void main(String[] args) throws IOException {
        // 1) Build the movie index from CSVs
        // Update these paths to where your CSV files are located
        String moviesPath = "resources/tmdb_5000_movies.csv";
        String creditsPath = "resources/tmdb_5000_credits.csv";

        // Create initialization screen with larger size
        Terminal terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(100, 30))
                .createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        // Define output display function
        int row = 0;
        screen.clear();

        // Print loading information
        printStringToScreen(screen, 0, row++, "Loading movie data...");
        screen.refresh();

        MovieIndex movieIndex = MovieDataLoader.buildIndex(moviesPath, creditsPath);

        printStringToScreen(screen, 0, row++, "Loaded " + movieIndex.size() + " movies.");
        screen.refresh();

        // 2) Create the game controller (30s turns, first‐to‐5 by default)
        GameController controller = new GameController(movieIndex);

        // 3) Extract all distinct genres (case‐insensitive, sorted)
        printStringToScreen(screen, 0, row++, "Extracting genres...");
        screen.refresh();

        Set<String> allGenres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Movie m : movieIndex.all()) {
            allGenres.addAll(m.getGenres());
        }

        printStringToScreen(screen, 0, row++, "Found " + allGenres.size() + " unique genres.");
        screen.refresh();

        // Set initial movie
        printStringToScreen(screen, 0, row++, "Selecting initial movie...");
        screen.refresh();

        List<Movie> allMovies = movieIndex.all();
        if (!allMovies.isEmpty()) {
            Random random = new Random();
            Movie initialMovie = allMovies.get(random.nextInt(allMovies.size()));
            controller.setInitialMovie(initialMovie);

            String initialMovieMsg = "Initial movie set: " + initialMovie.getTitle();
            printStringToScreen(screen, 0, row++, initialMovieMsg);
            screen.refresh();
        } else {
            printStringToScreen(screen, 0, row++, "Error: No movies in index!");
            screen.refresh();
            screen.stopScreen();
            terminal.close();
            return;
        }

        // Show loading complete, game about to start
        printStringToScreen(screen, 0, row++, "Setup complete! Press any key to continue...");
        screen.refresh();

        // Wait for user to press key to continue
        terminal.readInput();

        // Close initialization screen
        screen.stopScreen();
        terminal.close();

        // 4) Launch the TUI for genre‐selection for both players
        int winTarget = 5;  // if you later add a getter, you could use controller.getScoreToWin()
        TUIGameView tui = new TUIGameView(movieIndex, allGenres, winTarget, controller);

        try {
            // Player 1 selects genre
            String player1Genre = tui.promptGenreSelection("Player 1");
            Player player1 = new Player("Player 1", new GenreWinCondition(player1Genre, winTarget));
            controller.addPlayer(player1);

            // Reset screen for Player 2
            tui.close();
            tui = new TUIGameView(movieIndex, allGenres, winTarget, controller);

            // Player 2 selects genre
            String player2Genre = tui.promptGenreSelection("Player 2");
            Player player2 = new Player("Player 2", new GenreWinCondition(player2Genre, winTarget));
            controller.addPlayer(player2);

            // 5) Start the game
            controller.startGame();

            // Main game loop
            List<Movie> gameHistory = new ArrayList<>();
            if (controller.getInitialMovie() != null) {
                gameHistory.add(controller.getInitialMovie());
            }

            while (controller.getState() == GameController.GameState.IN_PROGRESS) {
                Player currentPlayer = controller.getCurrentPlayer();

                // Reset screen for each turn
                tui.close();
                tui = new TUIGameView(movieIndex, allGenres, winTarget, controller);

                String movieTitle = tui.promptMovie(currentPlayer, gameHistory);

                if (movieTitle == null) {
                    // Time expired - handled by GameController
                    controller.handleTimeExpired();
                    break;
                } else {
                    boolean validMove = controller.processTurn(movieTitle);

                    if (validMove) {
                        Movie namedMovie = movieIndex.getByTitle(movieTitle);
                        if (namedMovie != null) {
                            gameHistory.add(namedMovie);
                        }
                    }
                }

                // Check if game is over
                if (controller.getState() == GameController.GameState.COMPLETED) {
                    break;
                }
            }

            // Game over - show final screen
            tui.close();

            // Display game results
            Terminal finalTerminal = new DefaultTerminalFactory()
                    .setInitialTerminalSize(new TerminalSize(100, 30))
                    .createTerminal();
            Screen finalScreen = new TerminalScreen(finalTerminal);
            finalScreen.startScreen();

            int finalRow = 0;
            finalScreen.clear();

            printStringToScreen(finalScreen, 0, finalRow++, "[GAME] Game Over! [GAME]");
            finalRow++;

            if (controller.getWinner() != null) {
                printStringToScreen(finalScreen, 0, finalRow++, "Winner: " + controller.getWinner().getName() + "!");
                printStringToScreen(finalScreen, 0, finalRow++, "Score: " + controller.getWinner().getScore() + "/" + winTarget);

                // Add winner's genre information
                if (controller.getWinner().getWinCondition() instanceof GenreWinCondition) {
                    String winnerGenre = ((GenreWinCondition)controller.getWinner().getWinCondition()).getGenre();
                    printStringToScreen(finalScreen, 0, finalRow++, "Genre: " + winnerGenre);
                }
            } else {
                printStringToScreen(finalScreen, 0, finalRow++, "No winner.");
            }

            finalRow++;
            printStringToScreen(finalScreen, 0, finalRow++, "Game Summary:");

            // Get game summary and display line by line
            String summary = controller.getGameSummary();
            String[] summaryLines = summary.split("\n");
            for (String line : summaryLines) {
                // If line is too long, truncate display
                if (line.length() > finalScreen.getTerminalSize().getColumns()) {
                    line = line.substring(0, finalScreen.getTerminalSize().getColumns() - 3) + "...";
                }
                printStringToScreen(finalScreen, 0, finalRow++, line);
            }

            finalRow++;
            printStringToScreen(finalScreen, 0, finalRow++, "Press any key to exit...");
            finalScreen.refresh();

            // Wait for user to press any key to exit
            finalTerminal.readInput();
            finalScreen.stopScreen();
            finalTerminal.close();

        } finally {
            // Ensure proper cleanup
            try {
                tui.close();
            } catch (Exception e) {
                // Ignore exceptions during close
            }
            controller.cleanup();
        }
    }

    // Helper method to output string at specified position
    private static void printStringToScreen(Screen screen, int col, int row, String text) {
        // Filter out special characters that might cause display problems
        String safeText = text.replaceAll("[^\u0000-\uFFFF]", "*");

        // Ensure text doesn't exceed screen width
        int maxWidth = screen.getTerminalSize().getColumns() - col;
        if (safeText.length() > maxWidth) {
            safeText = safeText.substring(0, maxWidth);
        }

        for (int i = 0; i < safeText.length(); i++) {
            screen.setCharacter(col + i, row,
                    new TextCharacter(safeText.charAt(i),
                            TextColor.ANSI.WHITE,
                            TextColor.ANSI.BLACK));
        }
    }
}
