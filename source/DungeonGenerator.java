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
        for (int attempt = 1; attempt <= settings.maxGenerationAttempts; attempt++) {
            rooms.clear();

            fillMapWithWalls();
            placeRooms(settings.maxRooms);
            assignRoomTypes();
            connectRooms();
            createSecretRoom();
            placeLockedDoorAndKey();
            placePlayerAndExit();
            populateRooms();

            if (isExitReachable()) {
                return;
            }
        }

        System.out.println("Warning: Could not generate a fully valid dungeon.");
    }

    public void printSummary() {
        int enemies = countTiles(TileType.ENEMY);
        int treasures = countTiles(TileType.TREASURE);
        int traps = countTiles(TileType.TRAP);
        int potions = countTiles(TileType.POTION);
        int doors = countTiles(TileType.DOOR);
        int lockedDoors = countTiles(TileType.LOCKED_DOOR);
        int secretDoors = countTiles(TileType.SECRET_DOOR);

        System.out.println();
        System.out.println("Dungeon Summary:");
        System.out.println("Rooms: " + rooms.size());
        System.out.println("Enemies: " + enemies);
        System.out.println("Treasures: " + treasures);
        System.out.println("Traps: " + traps);
        System.out.println("Potions: " + potions);
        System.out.println("Doors: " + doors);
        System.out.println("Locked Doors: " + lockedDoors);
        System.out.println("Secret Doors: " + secretDoors);
    }

    private int countTiles(TileType tileType) {
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == tileType) {
                    count++;
                }
            }
        }

        return count;
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

            int x1 = previousRoom.centerX();
            int y1 = previousRoom.centerY();

            int x2 = currentRoom.centerX();
            int y2 = currentRoom.centerY();

            if (random.nextBoolean()) {
                carveHorizontalHallway(x1, x2, y1);
                carveVerticalHallway(y1, y2, x2);
            } else {
                carveVerticalHallway(y1, y2, x1);
                carveHorizontalHallway(x1, x2, y2);
            }

            placeDoorBetweenRooms(previousRoom, currentRoom);
            placeDoorBetweenRooms(currentRoom, previousRoom);
        }
    }

    private void placeDoorBetweenRooms(Room fromRoom, Room toRoom) {
        int bestX = -1;
        int bestY = -1;
        int bestDistance = Integer.MAX_VALUE;

        // Find the room-edge tile that actually touches carved hallway floor outside the room.
        // This is more reliable than guessing a side because hallways can be L-shaped and 2 tiles wide.
        for (int y = fromRoom.y; y < fromRoom.y + fromRoom.height; y++) {
            for (int x = fromRoom.x; x < fromRoom.x + fromRoom.width; x++) {
                if (!isRoomEdgeTile(x, y, fromRoom)) {
                    continue;
                }

                if (!touchesFloorOutsideRoom(x, y, fromRoom)) {
                    continue;
                }

                int distance = Math.abs(x - toRoom.centerX()) + Math.abs(y - toRoom.centerY());

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (bestX != -1 && bestY != -1) {
            map[bestY][bestX] = TileType.DOOR;
        }
    }

    private boolean isRoomEdgeTile(int x, int y, Room room) {
        return x == room.x
                || x == room.x + room.width - 1
                || y == room.y
                || y == room.y + room.height - 1;
    }

    private boolean touchesFloorOutsideRoom(int x, int y, Room room) {
        return isFloorOutsideRoom(x + 1, y, room)
                || isFloorOutsideRoom(x - 1, y, room)
                || isFloorOutsideRoom(x, y + 1, room)
                || isFloorOutsideRoom(x, y - 1, room);
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
            if (room.type == RoomType.SECRET) {
                continue;
            }

            placeDoorsForRoom(room);
        }
    }

    private void placeLockedDoorAndKey() {
        Room bossRoom = findRoomByType(RoomType.BOSS);

        if (bossRoom == null) {
            return;
        }

        for (int y = bossRoom.y - 1; y <= bossRoom.y + bossRoom.height; y++) {
            for (int x = bossRoom.x - 1; x <= bossRoom.x + bossRoom.width; x++) {
                if (!isInsideMap(x, y)) {
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
        List<Room> validRooms = new ArrayList<>();

        for (Room room : rooms) {
            if (room.type == RoomType.COMBAT || room.type == RoomType.TREASURE || room.type == RoomType.TRAP) {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) {
            return;
        }

        Room keyRoom = validRooms.get(random.nextInt(validRooms.size()));
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

    private void placePlayerAndExit() {
        Room startRoom = findRoomByType(RoomType.START);
        Room exitRoom = findRoomByType(RoomType.EXIT);

        if (startRoom == null || exitRoom == null) {
            return;
        }

        map[startRoom.centerY()][startRoom.centerX()] = TileType.PLAYER;
        map[exitRoom.centerY()][exitRoom.centerX()] = TileType.EXIT;
    }

    private void populateRooms() {
        for (Room room : rooms) {
            if (room.type == RoomType.COMBAT) {
                int enemyCount = random.nextInt(settings.maxEnemiesPerCombatRoom - settings.minEnemiesPerCombatRoom + 1)
                        + settings.minEnemiesPerCombatRoom;

                for (int i = 0; i < enemyCount; i++) {
                    placeRandomTileInRoom(room, TileType.ENEMY);
                }

                if (random.nextInt(100) < 25) {
                    placeRandomTileInRoom(room, TileType.POTION);
                }

            } else if (room.type == RoomType.TREASURE) {
                placeRandomTileInRoom(room, TileType.TREASURE);

                if (random.nextBoolean()) {
                    placeRandomTileInRoom(room, TileType.POTION);
                }

            } else if (room.type == RoomType.TRAP) {
                int trapCount = random.nextInt(settings.maxTrapsPerTrapRoom - settings.minTrapsPerTrapRoom + 1)
                        + settings.minTrapsPerTrapRoom;

                for (int i = 0; i < trapCount; i++) {
                    placeRandomTileInRoom(room, TileType.TRAP);
                }

            } else if (room.type == RoomType.BOSS) {
                placeRandomTileInRoom(room, TileType.BOSS);
                placeRandomTileInRoom(room, TileType.TREASURE);

            } else if (room.type == RoomType.SECRET) {
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

            if (roll < settings.combatRoomChance) {
                rooms.get(i).type = RoomType.COMBAT;
            } else if (roll < settings.combatRoomChance + settings.treasureRoomChance) {
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
        int doorX = secretRoom.centerX();
        int doorY = secretRoom.centerY();

        int dx = normalRoom.centerX() - secretRoom.centerX();
        int dy = normalRoom.centerY() - secretRoom.centerY();

        if (Math.abs(dx) > Math.abs(dy)) {
            doorX = dx > 0 ? secretRoom.x + secretRoom.width - 1 : secretRoom.x;
        } else {
            doorY = dy > 0 ? secretRoom.y + secretRoom.height - 1 : secretRoom.y;
        }

        if (isInsideMap(doorX, doorY)) {
            map[doorY][doorX] = TileType.SECRET_DOOR;
        }
    }

    public boolean isExitReachable() {
        int startX = -1;
        int startY = -1;
        int exitX = -1;
        int exitY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == TileType.PLAYER) {
                    startX = x;
                    startY = y;
                } else if (map[y][x] == TileType.EXIT) {
                    exitX = x;
                    exitY = y;
                }
            }
        }

        if (startX == -1 || exitX == -1) {
            return false;
        }

        boolean[][] visited = new boolean[height][width];
        return floodFill(startX, startY, exitX, exitY, visited);
    }

    private boolean floodFill(int x, int y, int exitX, int exitY, boolean[][] visited) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }

        if (visited[y][x]) {
            return false;
        }

        if (!isWalkable(map[y][x])) {
            return false;
        }

        if (x == exitX && y == exitY) {
            return true;
        }

        visited[y][x] = true;

        return floodFill(x + 1, y, exitX, exitY, visited)
                || floodFill(x - 1, y, exitX, exitY, visited)
                || floodFill(x, y + 1, exitX, exitY, visited)
                || floodFill(x, y - 1, exitX, exitY, visited);
    }

    private boolean isWalkable(TileType tile) {
        return tile == TileType.FLOOR
                || tile == TileType.DOOR
                || tile == TileType.PLAYER
                || tile == TileType.EXIT
                || tile == TileType.ENEMY
                || tile == TileType.TREASURE
                || tile == TileType.TRAP
                || tile == TileType.BOSS
                || tile == TileType.KEY
                || tile == TileType.POTION
                || tile == TileType.LOCKED_DOOR;
    }

    private void placeDoorsForRoom(Room room) {
        // Top and bottom edges
        for (int x = room.x; x < room.x + room.width; x++) {
            tryPlaceDoorAtRoomEdge(x, room.y - 1, room);
            tryPlaceDoorAtRoomEdge(x, room.y + room.height, room);
        }

        // Left and right edges
        for (int y = room.y; y < room.y + room.height; y++) {
            tryPlaceDoorAtRoomEdge(room.x - 1, y, room);
            tryPlaceDoorAtRoomEdge(room.x + room.width, y, room);
        }
    }

    private void tryPlaceDoorAtRoomEdge(int x, int y, Room room) {
        if (!isInsideMap(x, y)) {
            return;
        }

        if (map[y][x] != TileType.FLOOR) {
            return;
        }

        if (!touchesRoomInterior(x, y, room)) {
            return;
        }

        if (!touchesHallwayOutsideRoom(x, y, room)) {
            return;
        }

        if (hasNearbyDoor(x, y)) {
            return;
        }

        map[y][x] = TileType.DOOR;
    }

    private boolean touchesRoomInterior(int x, int y, Room room) {
        return isInsideRoom(x + 1, y, room)
                || isInsideRoom(x - 1, y, room)
                || isInsideRoom(x, y + 1, room)
                || isInsideRoom(x, y - 1, room);
    }

    private boolean touchesHallwayOutsideRoom(int x, int y, Room room) {
        return isFloorOutsideRoom(x + 1, y, room)
                || isFloorOutsideRoom(x - 1, y, room)
                || isFloorOutsideRoom(x, y + 1, room)
                || isFloorOutsideRoom(x, y - 1, room);
    }

    private boolean isFloorOutsideRoom(int x, int y, Room room) {
        if (!isInsideMap(x, y)) {
            return false;
        }

        if (isInsideRoom(x, y, room)) {
            return false;
        }

        return map[y][x] == TileType.FLOOR;
    }

    private boolean isInsideRoom(int x, int y, Room room) {
        return x >= room.x
                && x < room.x + room.width
                && y >= room.y
                && y < room.y + room.height;
    }

    private boolean isInsideMap(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private boolean hasNearbyDoor(int x, int y) {
        for (int checkY = y - 1; checkY <= y + 1; checkY++) {
            for (int checkX = x - 1; checkX <= x + 1; checkX++) {
                if (!isInsideMap(checkX, checkY)) {
                    continue;
                }

                if (map[checkY][checkX] == TileType.DOOR
                        || map[checkY][checkX] == TileType.LOCKED_DOOR
                        || map[checkY][checkX] == TileType.SECRET_DOOR) {
                    return true;
                }
            }
        }

        return false;
    }


    private Room findRoomByType(RoomType type) {
        for (Room room : rooms) {
            if (room.type == type) {
                return room;
            }
        }

        return null;
    }

    public String getMapAsString() {
        StringBuilder builder = new StringBuilder();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                builder.append(map[y][x].getSymbol());
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    public TileType[][] getMap() {
        return map;
    }

    public void printMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(map[y][x].getSymbol());
            }
            System.out.println();
        }
    }

    public boolean isRoomTile(int x, int y) {
        for (Room room : rooms) {
            if (x >= room.x
                    && x < room.x + room.width
                    && y >= room.y
                    && y < room.y + room.height) {
                return true;
            }
        }

        return false;
    }


    public int getRoomCount() {
        return rooms.size();
    }
}
