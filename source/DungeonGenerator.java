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
        connectRooms();
        placeDoors();
        placePlayerAndExit();
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

    private void connectRooms() {
        for (int i = 1; i < rooms.size(); i++) {
            Room previousRoom = rooms.get(i - 1);
            Room currentRoom = rooms.get(i);

            int previousX = previousRoom.centerX();
            int previousY = previousRoom.centerY();

            int currentX = currentRoom.centerX();
            int currentY = currentRoom.centerY();

            if (random.nextBoolean()) {
                carveHorizontalHallway(previousX, currentX, previousY);
                carveVerticalHallway(previousY, currentY, currentX);
            } else {
                carveVerticalHallway(previousY, currentY, previousX);
                carveHorizontalHallway(previousX, currentX, currentY);
            }
        }
    }

    private void carveHorizontalHallway(int startX, int endX, int y) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);

        for (int x = minX; x <= maxX; x++) {
            carveHallwayTile(x, y);
        }
    }

    private void carveVerticalHallway(int startY, int endY, int x) {
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);

        for (int y = minY; y <= maxY; y++) {
            carveHallwayTile(x, y);
        }
    }

    private void carveHallwayTile(int x, int y) {
        if (map[y][x] == TileType.WALL) {
            map[y][x] = TileType.FLOOR;
        }
    }

    private void placeDoors() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] == TileType.FLOOR && shouldPlaceDoor(x, y)) {
                    map[y][x] = TileType.DOOR;
                }
            }
        }
    }

    private boolean shouldPlaceDoor(int x, int y) {
        boolean floorLeft = map[y][x - 1] == TileType.FLOOR;
        boolean floorRight = map[y][x + 1] == TileType.FLOOR;
        boolean floorUp = map[y - 1][x] == TileType.FLOOR;
        boolean floorDown = map[y + 1][x] == TileType.FLOOR;

        boolean wallLeft = map[y][x - 1] == TileType.WALL;
        boolean wallRight = map[y][x + 1] == TileType.WALL;
        boolean wallUp = map[y - 1][x] == TileType.WALL;
        boolean wallDown = map[y + 1][x] == TileType.WALL;

        boolean horizontalDoor = floorLeft && floorRight && wallUp && wallDown;
        boolean verticalDoor = floorUp && floorDown && wallLeft && wallRight;

        return horizontalDoor || verticalDoor;
    }

    private void placePlayerAndExit() {
        if (rooms.isEmpty()) {
            return;
        }

        Room startRoom = rooms.get(0);
        Room exitRoom = rooms.get(rooms.size() - 1);

        map[startRoom.centerY()][startRoom.centerX()] = TileType.PLAYER;
        map[exitRoom.centerY()][exitRoom.centerX()] = TileType.EXIT;
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