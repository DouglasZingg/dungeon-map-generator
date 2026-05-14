public enum TileType {
    WALL('#'),
    FLOOR('.'),
    DOOR('+'),
    LOCKED_DOOR('L'),
    PLAYER('@'),
    EXIT('>'),
    ENEMY('E'),
    TREASURE('$'),
    TRAP('^'),
    BOSS('B'),
    KEY('K');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}