public class Move {
    private int row;
    private int col;
    private boolean isBlack;  // If true, move is by Black; otherwise, White.

    public Move(int row, int col, boolean isBlack) {
        this.row = row;
        this.col = col;
        this.isBlack = isBlack;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isBlack() {
        return isBlack;
    }

    @Override
    public String toString() {
        return String.format("Move [row=%d, col=%d, isBlack=%b]", row, col, isBlack);
    }
}
