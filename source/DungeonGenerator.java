import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    private final int width;
    private final int height;
    private final TileType[][] map;
    private final List<Room> rooms;
    private final Random random;
    private final DungeonSettings settings;

    public DungeonGenerator(int width, int height, DungeonSettings settings) {
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.map = new TileType[height][width];
        this.rooms = new ArrayList<>();
        this.random = new Random(settings.seed);
    }

    public void printLegend() {
        System.out.println();
        System.out.println("Legend:");
        System.out.println("# = Wall");
        System.out.println(". = Floor");
        System.out.println("+ = Door");
        System.out.println("@ = Player Start");
        System.out.println("> = Exit");
        System.out.println("E = Enemy");
        System.out.println("$ = Treasure");
        System.out.println("^ = Trap");
        System.out.println("B = Boss");
        System.out.println("L = Locked Door");
        System.out.println("K = Key");
        System.out.println("! = Potion");
        System.out.println("? = Secret Door");
    }

    public void generate() {
        fillMapWithWalls();
        placeRooms(settings.maxRooms);

        createSecretRoom();

        assignRoomTypes();
        connectRooms();
        placeDoors();
        placeLockedDoorAndKey();
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
            int roomWidth = random.nextInt(settings.maxRoomWidth - settings.minRoomWidth + 1)
                    + settings.minRoomWidth;

            int roomHeight = random.nextInt(settings.maxRoomHeight - settings.minRoomHeight + 1)
                    + settings.minRoomHeight;

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
            carveFloor(x, y);
            carveFloor(x, y + 1);
        }
    }

    private void carveVerticalHallway(int startY, int endY, int x) {
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);

        for (int y = minY; y <= maxY; y++) {
            carveFloor(x, y);
            carveFloor(x + 1, y);
        }
    }

    private void carveFloor(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        map[y][x] = TileType.FLOOR;
    }

    private void placeDoors() {
        for (Room room : rooms) {
            placeRoomDoors(room);
        }
    }

    private void placeLockedDoorAndKey() {
        if (rooms.size() < 4) {
            return;
        }

        Room bossRoom = rooms.get(rooms.size() - 2);

        // Try to turn one normal door near the boss room into a locked door
        for (int y = bossRoom.y - 1; y <= bossRoom.y + bossRoom.height; y++) {
            for (int x = bossRoom.x - 1; x <= bossRoom.x + bossRoom.width; x++) {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    continue;
                }

                if (map[y][x] == TileType.DOOR) {
                    map[y][x] = TileType.LOCKED_DOOR;
                    placeKey();
                    return;
                }
            }
        }
    }

    private void placeKey() {
        if (rooms.size() < 3) {
            return;
        }

        // Put the key somewhere before the boss room
        int maxRoomIndex = Math.max(1, rooms.size() - 3);
        Room keyRoom = rooms.get(random.nextInt(maxRoomIndex) + 1);

        placeRandomTileInRoom(keyRoom, TileType.KEY);
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
                int enemyCount = random.nextInt(settings.maxEnemiesPerCombatRoom - settings.minEnemiesPerCombatRoom + 1) + settings.minEnemiesPerCombatRoom;

                for (int i = 0; i < enemyCount; i++) {
                    placeRandomTileInRoom(room, TileType.ENEMY);
                }

                if (random.nextInt(100) < 25) {
                    placeRandomTileInRoom(room, TileType.POTION);
                }

            } else if (room.type == RoomType.TREASURE) {
                placeRandomTileInRoom(room, TileType.TREASURE);

                if (random.nextBoolean()) {
                    placeRandomTileInRoom(room, TileType.TREASURE);
                }else if (room.type == RoomType.TREASURE) {
                    placeRandomTileInRoom(room, TileType.TREASURE);

                    if (random.nextBoolean()) {
                        placeRandomTileInRoom(room, TileType.POTION);
                    }
                }
            } else if (room.type == RoomType.TRAP) {
                int trapCount = random.nextInt(settings.maxTrapsPerTrapRoom - settings.minTrapsPerTrapRoom + 1) + settings.minTrapsPerTrapRoom;

                for (int i = 0; i < trapCount; i++) {
                    placeRandomTileInRoom(room, TileType.TRAP);
                }
            }else if (room.type == RoomType.BOSS) {
                placeRandomTileInRoom(room, TileType.BOSS);
                placeRandomTileInRoom(room, TileType.TREASURE);
            }else if (room.type == RoomType.SECRET) {

                placeRandomTileInRoom(room, TileType.TREASURE);
                placeRandomTileInRoom(room, TileType.TREASURE);
                placeRandomTileInRoom(room, TileType.POTION);
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

    private void assignRoomTypes() {
        if (rooms.isEmpty()) {
            return;
        }

        rooms.get(0).type = RoomType.START;
        rooms.get(rooms.size() - 1).type = RoomType.EXIT;

        if (rooms.size() > 2) {
            rooms.get(rooms.size() - 2).type = RoomType.BOSS;
        }

        for (int i = 1; i < rooms.size() - 2; i++) {
            int roll = random.nextInt(100);

            if (roll < 60) {
                rooms.get(i).type = RoomType.COMBAT;
            } else if (roll < 85) {
                rooms.get(i).type = RoomType.TREASURE;
            } else {
                rooms.get(i).type = RoomType.TRAP;
            }
        }
    }

    private void createSecretRoom() {
        int roomWidth = 5;
        int roomHeight = 5;

        for (int attempt = 0; attempt < 50; attempt++) {

            int x = random.nextInt(width - roomWidth - 2) + 1;
            int y = random.nextInt(height - roomHeight - 2) + 1;

            Room secretRoom = new Room(x, y, roomWidth, roomHeight);

            if (!doesRoomOverlap(secretRoom)) {

                carveRoom(secretRoom);

                secretRoom.type = RoomType.SECRET;

                rooms.add(secretRoom);

                connectSecretRoom(secretRoom);

                return;
            }
        }
    }

    private void connectSecretRoom(Room secretRoom) {

        Room nearestRoom = rooms.get(0);

        double nearestDistance = Double.MAX_VALUE;

        for (Room room : rooms) {

            if (room == secretRoom) {
                continue;
            }

            double dx = room.centerX() - secretRoom.centerX();
            double dy = room.centerY() - secretRoom.centerY();

            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = room;
            }
        }

        int x1 = secretRoom.centerX();
        int y1 = secretRoom.centerY();

        int x2 = nearestRoom.centerX();
        int y2 = nearestRoom.centerY();

        carveSecretHallway(x1, x2, y1);
        carveSecretHallwayVertical(y1, y2, x2);

        placeSecretDoorBetween(secretRoom, nearestRoom);
    }

    private void carveSecretHallway(int startX, int endX, int y) {
        for (int x = Math.min(startX, endX); x <= Math.max(startX, endX); x++) {
            carveFloor(x, y);
        }
    }

    private void carveSecretHallwayVertical(int startY, int endY, int x) {
        for (int y = Math.min(startY, endY); y <= Math.max(startY, endY); y++) {
            carveFloor(x, y);
        }
    }

    private void placeSecretDoorBetween(Room secretRoom, Room normalRoom) {

        int doorX = normalRoom.centerX();
        int doorY = normalRoom.centerY();

        map[doorY][doorX] = TileType.SECRET_DOOR;
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