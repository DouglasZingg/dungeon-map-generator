import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    private final int width;
    private final int height;
    private final TileType[][] map;
    private final List<Room> rooms;
    private final Random random;

    public DungeonGenerator(int width, int height) {
        this.width = width;
        this.height = height;
        this.map = new TileType[height][width];
        this.rooms = new ArrayList <> ();
        this.random = new Random();
    }

    public void generate() {
        fillMapWithWalls();
        placeRooms(12);
    }

    private void fillMapWithWalls() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = TileType.WALL;
            }
        }
    }

    private void placeRooms(int maxRooms) {
        for (int i = 0; i < maxRooms; i++) {
            int roomWidth = random.nextInt(6) + 4;
            int roomHeight = random.nextInt(5) + 4;

            int x = random.nextInt(width - roomWidth - 2) + 1;
            int y = random.nextInt(height - roomHeight - 2) + 1;

            Room newRoom = new Room(x, y, roomWidth, roomHeight);

            if (!doesRoomOverlap(newRoom)) {
                carveRoom(newRoom);
                rooms.add(newRoom);
            }
        }
    }

    private boolean doesRoomOverlap(Room newRoom) {
        for (Room room : rooms) {
            if (newRoom.intersects(room)) {
                return true;
            }
        }

        return false;
    }

    private void carveRoom(Room room) {
        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                map[y][x] = TileType.FLOOR;
            }
        }
    }

    public void printMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(map[y][x].getSymbol());
            }
            System.out.println();
        }
    }
}