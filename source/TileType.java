// Every tile that can appear on the map.
// The character symbols are mostly for text/debug export; the UI renders colors.
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
    KEY('K'),
    SECRET_DOOR('?'),
    POTION('!');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}