public interface IAIStrategy {
    /**
     * Chooses a Move to make, given the current board.
     *
     * @param board The current game board state.
     * @return The Move selected by this strategy.
     */
    Move makeMove(Board board);
}
