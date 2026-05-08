public enum TileType {
    WALL('#'),
    FLOOR('.'),
    DOOR('+');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}