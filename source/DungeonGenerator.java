import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DungeonGenerator {
    private final int width;
    private final int height;
    private final TileType[][] map;
    private final List<Room> rooms;
    private final Random random;
    private final DungeonSettings settings;
    private final Set<String> connectedRoomPairs = new HashSet<>();

    private static class Area {
        int x;
        int y;
        int width;
        int height;

        Area(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int area() {
            return width * height;
        }
    }

    public DungeonGenerator(int width, int height, DungeonSettings settings) {
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.map = new TileType[height][width];
        this.rooms = new ArrayList<>();
        this.random = new Random(settings.seed);
    }

    public void generate() {
        for (int attempt = 1; attempt <= settings.maxGenerationAttempts; attempt++) {
            rooms.clear();
            connectedRoomPairs.clear();

            fillMapWithWalls();
            placePackedRooms(settings.maxRooms);
            assignRoomTypes();
            connectRoomsPacked();
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

    private void fillMapWithWalls() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = TileType.WALL;
            }
        }
    }

    private void placePackedRooms(int targetRooms) {
        List<Area> leaves = new ArrayList<>();
        leaves.add(new Area(1, 1, width - 2, height - 2));

        int safety = 0;
        while (leaves.size() < targetRooms && safety < settings.maxRoomPlacementAttempts) {
            safety++;
            Area largest = leaves.stream().max(Comparator.comparingInt(Area::area)).orElse(null);

            if (largest == null || !canSplit(largest)) {
                break;
            }

            leaves.remove(largest);
            splitArea(largest, leaves);
        }

        for (Area leaf : leaves) {
            Room room = createRoomFromArea(leaf);
            if (room != null && !doesRoomOverlap(room)) {
                carveRoom(room);
                rooms.add(room);
            }
        }
    }

    private boolean canSplit(Area area) {
        int minLeafWidth = settings.minRoomWidth + 2;
        int minLeafHeight = settings.minRoomHeight + 2;
        return area.width >= minLeafWidth * 2 || area.height >= minLeafHeight * 2;
    }

    private void splitArea(Area area, List<Area> leaves) {
        int minLeafWidth = settings.minRoomWidth + 2;
        int minLeafHeight = settings.minRoomHeight + 2;

        boolean splitVertical;
        if (area.width > area.height * 1.25) {
            splitVertical = true;
        } else if (area.height > area.width * 1.25) {
            splitVertical = false;
        } else {
            splitVertical = random.nextBoolean();
        }

        if (splitVertical && area.width < minLeafWidth * 2) {
            splitVertical = false;
        }
        if (!splitVertical && area.height < minLeafHeight * 2) {
            splitVertical = true;
        }

        if (splitVertical) {
            int maxSplit = area.width - minLeafWidth;
            int split = random.nextInt(maxSplit - minLeafWidth + 1) + minLeafWidth;
            leaves.add(new Area(area.x, area.y, split, area.height));
            leaves.add(new Area(area.x + split, area.y, area.width - split, area.height));
        } else {
            int maxSplit = area.height - minLeafHeight;
            int split = random.nextInt(maxSplit - minLeafHeight + 1) + minLeafHeight;
            leaves.add(new Area(area.x, area.y, area.width, split));
            leaves.add(new Area(area.x, area.y + split, area.width, area.height - split));
        }
    }

    private Room createRoomFromArea(Area area) {
        // Keep a one-tile buffer inside each BSP leaf when possible.
        // This prevents rooms from visually merging/colliding when two leaves touch.
        int margin = 1;
        int usableX = area.x + margin;
        int usableY = area.y + margin;
        int usableWidth = area.width - margin * 2;
        int usableHeight = area.height - margin * 2;

        if (usableWidth < settings.minRoomWidth || usableHeight < settings.minRoomHeight) {
            usableX = area.x;
            usableY = area.y;
            usableWidth = area.width;
            usableHeight = area.height;
        }

        int maxW = Math.min(settings.maxRoomWidth, usableWidth);
        int maxH = Math.min(settings.maxRoomHeight, usableHeight);
        int minW = Math.min(settings.minRoomWidth, maxW);
        int minH = Math.min(settings.minRoomHeight, maxH);

        if (maxW < 3 || maxH < 3) {
            return null;
        }

        int roomWidth = random.nextInt(maxW - minW + 1) + minW;
        int roomHeight = random.nextInt(maxH - minH + 1) + minH;

        int freeX = Math.max(0, usableWidth - roomWidth);
        int freeY = Math.max(0, usableHeight - roomHeight);
        int x = usableX + random.nextInt(freeX + 1);
        int y = usableY + random.nextInt(freeY + 1);

        x = Math.max(1, Math.min(x, width - roomWidth - 1));
        y = Math.max(1, Math.min(y, height - roomHeight - 1));

        return new Room(x, y, roomWidth, roomHeight);
    }

    private boolean doesRoomOverlap(Room newRoom) {
        for (Room room : rooms) {
            if (roomsOverlapWithPadding(newRoom, room, settings.roomPadding)) {
                return true;
            }
        }
        return false;
    }

    private boolean roomsOverlapWithPadding(Room a, Room b, int padding) {
        return a.x - padding < b.x + b.width
                && a.x + a.width + padding > b.x
                && a.y - padding < b.y + b.height
                && a.y + a.height + padding > b.y;
    }

    private void carveRoom(Room room) {
        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                map[y][x] = TileType.FLOOR;
            }
        }
    }

    private void connectRoomsPacked() {
        if (rooms.size() < 2) {
            return;
        }

        // Main pass: connect each room to its nearest previous room.
        for (int i = 1; i < rooms.size(); i++) {
            Room currentRoom = rooms.get(i);
            Room nearestRoom = findNearestPreviousRoom(currentRoom, i);
            connectTwoRooms(nearestRoom, currentRoom);
        }

        // Optional extra short links make the dungeon feel less like a tree and more like a packed map.
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            Room nearest = findNearestRoom(room, i);
            if (nearest != null
                    && edgeDistance(room, nearest) <= settings.extraConnectionMaxDistance
                    && random.nextInt(100) < settings.extraConnectionChance) {
                connectTwoRooms(room, nearest);
            }
        }
    }

    private Room findNearestPreviousRoom(Room room, int previousRoomCount) {
        Room nearestRoom = rooms.get(0);
        int nearestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < previousRoomCount; i++) {
            Room candidate = rooms.get(i);
            int distance = edgeDistance(room, candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = candidate;
            }
        }

        return nearestRoom;
    }

    private Room findNearestRoom(Room room, int roomIndex) {
        Room nearestRoom = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < rooms.size(); i++) {
            if (i == roomIndex) {
                continue;
            }

            Room candidate = rooms.get(i);
            int distance = edgeDistance(room, candidate);
            if (distance < nearestDistance && !isConnected(roomIndex, i)) {
                nearestDistance = distance;
                nearestRoom = candidate;
            }
        }

        return nearestRoom;
    }

    private int edgeDistance(Room a, Room b) {
        int horizontalGap = Math.max(0, Math.max(a.x - (b.x + b.width - 1), b.x - (a.x + a.width - 1)));
        int verticalGap = Math.max(0, Math.max(a.y - (b.y + b.height - 1), b.y - (a.y + a.height - 1)));
        return horizontalGap + verticalGap;
    }

    private void connectTwoRooms(Room roomA, Room roomB) {
        int indexA = rooms.indexOf(roomA);
        int indexB = rooms.indexOf(roomB);
        if (indexA != -1 && indexB != -1 && isConnected(indexA, indexB)) {
            return;
        }

        ConnectionPoints connection = getConnectionPoints(roomA, roomB);

        if (random.nextBoolean()) {
            carveHorizontalHallway(connection.startX, connection.endX, connection.startY);
            carveVerticalHallway(connection.startY, connection.endY, connection.endX);
        } else {
            carveVerticalHallway(connection.startY, connection.endY, connection.startX);
            carveHorizontalHallway(connection.startX, connection.endX, connection.endY);
        }

        placeDoor(connection.startX, connection.startY);
        placeDoor(connection.endX, connection.endY);

        if (indexA != -1 && indexB != -1) {
            connectedRoomPairs.add(pairKey(indexA, indexB));
        }
    }

    private static class ConnectionPoints {
        int doorAX;
        int doorAY;
        int startX;
        int startY;
        int doorBX;
        int doorBY;
        int endX;
        int endY;
    }

    private ConnectionPoints getConnectionPoints(Room a, Room b) {
        ConnectionPoints points = new ConnectionPoints();

        boolean horizontal = Math.abs(a.centerX() - b.centerX()) >= Math.abs(a.centerY() - b.centerY());

        if (horizontal) {
            int yA = clamp(b.centerY(), a.y + 1, a.y + a.height - 2);
            int yB = clamp(yA, b.y + 1, b.y + b.height - 2);

            if (b.centerX() >= a.centerX()) {
                points.doorAX = a.x + a.width - 1;
                points.startX = points.doorAX + 1;
                points.doorBX = b.x;
                points.endX = points.doorBX - 1;
            } else {
                points.doorAX = a.x;
                points.startX = points.doorAX - 1;
                points.doorBX = b.x + b.width - 1;
                points.endX = points.doorBX + 1;
            }

            points.doorAY = yA;
            points.startY = yA;
            points.doorBY = yB;
            points.endY = yB;
        } else {
            int xA = clamp(b.centerX(), a.x + 1, a.x + a.width - 2);
            int xB = clamp(xA, b.x + 1, b.x + b.width - 2);

            if (b.centerY() >= a.centerY()) {
                points.doorAY = a.y + a.height - 1;
                points.startY = points.doorAY + 1;
                points.doorBY = b.y;
                points.endY = points.doorBY - 1;
            } else {
                points.doorAY = a.y;
                points.startY = points.doorAY - 1;
                points.doorBY = b.y + b.height - 1;
                points.endY = points.doorBY + 1;
            }

            points.doorAX = xA;
            points.startX = xA;
            points.doorBX = xB;
            points.endX = xB;
        }

        points.startX = clamp(points.startX, 1, width - 2);
        points.startY = clamp(points.startY, 1, height - 2);
        points.endX = clamp(points.endX, 1, width - 2);
        points.endY = clamp(points.endY, 1, height - 2);

        return points;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void placeDoor(int x, int y) {
        if (!isInsideMap(x, y)) {
            return;
        }

        // Doors should only be placed on hallway tiles directly outside a room.
        // This prevents doors from appearing randomly inside room interiors.
        if (map[y][x] != TileType.FLOOR) {
            return;
        }

        if (isInsideAnyRoom(x, y)) {
            return;
        }

        if (!touchesAnyRoom(x, y)) {
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
                if (!isInsideMap(checkX, checkY)) {
                    continue;
                }

                TileType tile = map[checkY][checkX];
                if (tile == TileType.DOOR
                        || tile == TileType.LOCKED_DOOR
                        || tile == TileType.SECRET_DOOR) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isConnected(int indexA, int indexB) {
        return connectedRoomPairs.contains(pairKey(indexA, indexB));
    }

    private String pairKey(int indexA, int indexB) {
        int a = Math.min(indexA, indexB);
        int b = Math.max(indexA, indexB);
        return a + "-" + b;
    }

    private void carveHorizontalHallway(int startX, int endX, int y) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);

        for (int x = minX; x <= maxX; x++) {
            carveFloor(x, y);
        }
    }

    private void carveVerticalHallway(int startY, int endY, int x) {
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);

        for (int y = minY; y <= maxY; y++) {
            carveFloor(x, y);
        }
    }

    private void carveFloor(int x, int y) {
        if (!isInsideMap(x, y)) {
            return;
        }

        // Hallways are one tile wide and should not cut through room interiors.
        // They may start/end beside a room, then a door is placed on that hallway tile.
        if (isInsideAnyRoom(x, y)) {
            return;
        }

        map[y][x] = TileType.FLOOR;
    }

    private boolean isInsideAnyRoom(int x, int y) {
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

    private boolean touchesAnyRoom(int x, int y) {
        return isInsideAnyRoom(x + 1, y)
                || isInsideAnyRoom(x - 1, y)
                || isInsideAnyRoom(x, y + 1)
                || isInsideAnyRoom(x, y - 1);
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
        if (room.width <= 2 || room.height <= 2) {
            return;
        }

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

        if (width <= roomWidth + 2 || height <= roomHeight + 2 || rooms.isEmpty()) {
            return;
        }

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
        Room nearestRoom = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Room room : rooms) {
            if (room == secretRoom) {
                continue;
            }

            int distance = edgeDistance(secretRoom, room);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = room;
            }
        }

        if (nearestRoom == null) {
            return;
        }

        ConnectionPoints connection = getConnectionPoints(secretRoom, nearestRoom);
        carveHorizontalHallway(connection.startX, connection.endX, connection.startY);
        carveVerticalHallway(connection.startY, connection.endY, connection.endX);
        placeDoor(connection.endX, connection.endY);
        if (isInsideMap(connection.startX, connection.startY)
                && map[connection.startY][connection.startX] == TileType.FLOOR
                && !isInsideAnyRoom(connection.startX, connection.startY)
                && touchesAnyRoom(connection.startX, connection.startY)) {
            map[connection.startY][connection.startX] = TileType.SECRET_DOOR;
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
        if (!isInsideMap(x, y)) {
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
                || tile == TileType.LOCKED_DOOR
                || tile == TileType.SECRET_DOOR
                || tile == TileType.PLAYER
                || tile == TileType.EXIT
                || tile == TileType.ENEMY
                || tile == TileType.TREASURE
                || tile == TileType.TRAP
                || tile == TileType.BOSS
                || tile == TileType.KEY
                || tile == TileType.POTION;
    }

    private Room findRoomByType(RoomType type) {
        for (Room room : rooms) {
            if (room.type == type) {
                return room;
            }
        }

        return null;
    }

    private boolean isInsideMap(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
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
        System.out.print(getMapAsString());
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

    public void printSummary() {
        System.out.println();
        System.out.println("Dungeon Summary:");
        System.out.println("Rooms: " + rooms.size());
        System.out.println("Enemies: " + countTiles(TileType.ENEMY));
        System.out.println("Treasures: " + countTiles(TileType.TREASURE));
        System.out.println("Traps: " + countTiles(TileType.TRAP));
        System.out.println("Potions: " + countTiles(TileType.POTION));
        System.out.println("Doors: " + countTiles(TileType.DOOR));
        System.out.println("Locked Doors: " + countTiles(TileType.LOCKED_DOOR));
        System.out.println("Secret Doors: " + countTiles(TileType.SECRET_DOOR));
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRoomCount() {
        return rooms.size();
    }
}
