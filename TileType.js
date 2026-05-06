public enum TileType {
    WALL('#'),
        FLOOR('.');

    private final char symbol;

TileType(char symbol) {
    this.symbol = symbol;
}

    public char getSymbol() {
    return symbol;
}
}