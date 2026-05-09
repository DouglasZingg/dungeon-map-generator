public enum TileType {
    WALL('#'),
    FLOOR('.'),
    DOOR('+'),
    PLAYER('@'),
    EXIT('>'),
    ENEMY('E'),
    TREASURE('$'),
    TRAP('^');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}