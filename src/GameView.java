import java.util.Scanner;
import java.util.Set;

public class GameView {
    private final Scanner scanner = new Scanner(System.in);


    public String promptGenreSelection(Set<String> genres, int targetCount) {
        // welcome-to-game prompt
        System.out.println("\n🎬 Welcome to Movie Name Game! 🎬");
        System.out.printf("Your win condition is simple: be the first " +
                        "to name **%d movies** in your chosen genre.%n", targetCount);

        // give all the genre options
        System.out.println("Here are your genre options:");
        System.out.println(String.join(", ", genres));
        System.out.println();
        System.out.printf("Please type **one** genre from the list above to set as your win condition:\n> ");

        while (true) {
            String input = scanner.nextLine().trim();
            for (String g : genres) {
                if (g.equalsIgnoreCase(input)) {
                    return g;
                }
            }
            System.out.print("Invalid genre. Try again (exactly one from the list): ");
        }
    }

    public void displayGenreOptions(Set<String> genres) {
        System.out.println("Here are your genre options:");
        System.out.println(String.join(", ", genres));
        System.out.println();
    } //assisting method for testing
}
