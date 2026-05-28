import java.awt.Color;

public class TileColorHelper {
    public static Color getColor(TileType tile, DungeonGenerator dungeon, int x, int y, DungeonTheme theme) {
        switch (tile) {
            case WALL:
                return theme.wall;
            case FLOOR:
                if (dungeon != null && dungeon.isRoomTile(x, y)) {
                    return theme.roomFloor;
                }
                return theme.hallwayFloor;
            case DOOR:
                return theme.door;
            case LOCKED_DOOR:
                return theme.lockedDoor;
            case PLAYER:
                return theme.player;
            case EXIT:
                return theme.exit;
            case ENEMY:
                return theme.enemy;
            case TREASURE:
                return theme.treasure;
            case TRAP:
                return theme.trap;
            case BOSS:
                return theme.boss;
            case KEY:
                return theme.key;
            case POTION:
                return theme.potion;
            case SECRET_DOOR:
                return theme.secretDoor;
            default:
                return Color.GRAY;
        }
    }

    public static Color getColor(TileType tile, DungeonGenerator dungeon, int x, int y) {
        return getColor(tile, dungeon, x, y, DungeonTheme.CLASSIC);
    }
}
