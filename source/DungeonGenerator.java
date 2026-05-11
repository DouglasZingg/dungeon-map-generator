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
        assignRoomTypes();
        connectRooms();
        placeDoors();
        placePlayerAndExit();
        populateRooms();
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
        for (Room room : rooms) {
            placeRoomDoors(room);
        }
    }

    private void placeRoomDoors(Room room) {
        // Top and bottom walls
        for (int x = room.x; x < room.x + room.width; x++) {
            tryPlaceDoor(x, room.y - 1);
            tryPlaceDoor(x, room.y + room.height);
        }

        // Left and right walls
        for (int y = room.y; y < room.y + room.height; y++) {
            tryPlaceDoor(room.x - 1, y);
            tryPlaceDoor(room.x + room.width, y);
        }
    }

    private void tryPlaceDoor(int x, int y) {
        if (x <= 0 || x >= width - 1 || y <= 0 || y >= height - 1) {
            return;
        }

        if (map[y][x] != TileType.FLOOR) {
            return;
        }

        if (hasNearbyDoor(x, y)) {
            return;
        }

        map[y][x] = TileType.DOOR;
    }

    private boolean hasNearbyDoor(int x, int y) {
        for (int checkY = y - 1; checkY <= y + 1; checkY++) {
            for (int checkX = x - 1; checkX <= x + 1; checkX++) {
                if (map[checkY][checkX] == TileType.DOOR) {
                    return true;
                }
            }
        }

        return false;
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

    private void populateRooms() {
        for (Room room : rooms) {
            if (room.type == RoomType.COMBAT) {
                int enemyCount = random.nextInt(3) + 1; // 1 to 3 enemies

                for (int i = 0; i < enemyCount; i++) {
                    placeRandomTileInRoom(room, TileType.ENEMY);
                }

            } else if (room.type == RoomType.TREASURE) {
                placeRandomTileInRoom(room, TileType.TREASURE);

                if (random.nextBoolean()) {
                    placeRandomTileInRoom(room, TileType.TREASURE);
                }

            } else if (room.type == RoomType.TRAP) {
                int trapCount = random.nextInt(3) + 1; // 1 to 3 traps

                for (int i = 0; i < trapCount; i++) {
                    placeRandomTileInRoom(room, TileType.TRAP);
                }
            }else if (room.type == RoomType.BOSS) {
                placeRandomTileInRoom(room, TileType.BOSS);
                placeRandomTileInRoom(room, TileType.TREASURE);
            }
        }
    }

    private void placeRandomTileInRoom(Room room, TileType tile) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = random.nextInt(room.width - 2) + room.x + 1;
            int y = random.nextInt(room.height - 2) + room.y + 1;

            if (map[y][x] == TileType.FLOOR) {
                map[y][x] = tile;
                return;
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