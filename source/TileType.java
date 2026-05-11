public enum TileType {
    WALL('#'),
    FLOOR('.'),
    DOOR('+'),
    PLAYER('@'),
    EXIT('>'),
    ENEMY('E'),
    TREASURE('$'),
    TRAP('^'),
    BOSS('B');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}