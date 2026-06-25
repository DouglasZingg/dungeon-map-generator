// Lightweight room data.
// The generator mutates these fields while deciding shape, type, and path role.
public class Room {
    public int x;
    public int y;
    public int width;
    public int height;
    public RoomType type = RoomType.COMBAT;
    public RoomShape shape = RoomShape.RECTANGLE;
    public int shapeVariant = 0;
    public RoomPathRole pathRole = RoomPathRole.SIDE_PATH;

    public Room(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Center is used for path sorting and rough distance checks.
    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    // Basic bounding-box overlap check. Shaped rooms still use their full box
    // for collision because it keeps placement stable and easier to reason about.
    public boolean intersects(Room other) {
        return x < other.x + other.width &&
               x + width > other.x &&
               y < other.y + other.height &&
               y + height > other.y;
    }
}