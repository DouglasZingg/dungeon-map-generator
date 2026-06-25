import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

// This is the main generation engine.
// The UI collects settings, then this class builds the actual tile map.
// The rough flow is:
// 1. Split the map into packed areas.
// 2. Carve rooms inside those areas.
// 3. Connect rooms into a main path, side paths, and dead ends.
// 4. Place doors, special rooms, entities, and validation markers.
public class DungeonGenerator {
    private final int width;
    private final int height;
    private final TileType[][] map;
    private final List<Room> rooms;
    private final List<Room> mainPathRooms = new ArrayList<>();
    private final Random random;
    private final DungeonSettings settings;
    private final Set<String> connectedRoomPairs = new HashSet<>();

    // A temporary rectangle used by the packed/BSP-style room placement.
    // These are not rooms yet; they are just chunks of available map space.
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

    // Try a few times because procedural generation can occasionally make
    // an awkward layout. If a generated map is not reachable, we throw it
    // away and try again with the same seed stream.
    public void generate() {
        for (int attempt = 1; attempt <= settings.maxGenerationAttempts; attempt++) {
            rooms.clear();
            mainPathRooms.clear();
            connectedRoomPairs.clear();

            fillMapWithWalls();
            placePackedRooms(settings.maxRooms);
            connectRoomsPacked();
            assignRoomTypes();
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

    // Always start from a solid block of walls. Every room and hallway
    // carves floor tiles out of this grid.
    private void fillMapWithWalls() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = TileType.WALL;
            }
        }
    }

    // Splits the map into leaf areas, then places one room inside each leaf.
    // This creates a fuller, battle-map style layout without huge empty gaps.
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

    // Split the biggest available area either vertically or horizontally.
    // The aspect-ratio checks help avoid long skinny slices.
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

    // Turns one leaf area into an actual room. The room is usually slightly
    // smaller than the leaf so neighboring rooms do not touch directly.
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

        Room room = new Room(x, y, roomWidth, roomHeight);
        assignRoomShape(room);
        return room;
    }

    // Pick a room shape. Rectangles are still the most common because they
    // are reliable, while L/T/cross rooms add some handcrafted-looking variety.
    private void assignRoomShape(Room room) {
        // Very small rooms stay rectangular so doors and entities have reliable floor space.
        if (room.width < 7 || room.height < 7) {
            room.shape = RoomShape.RECTANGLE;
            room.shapeVariant = 0;
            return;
        }

        int roll = random.nextInt(100);

        if (roll < 55) {
            room.shape = RoomShape.RECTANGLE;
        } else if (roll < 75) {
            room.shape = RoomShape.L_SHAPE;
        } else if (roll < 90) {
            room.shape = RoomShape.T_SHAPE;
        } else {
            room.shape = RoomShape.CROSS;
        }

        room.shapeVariant = random.nextInt(4);
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

    // Carving only changes selected tiles to FLOOR. The room object still keeps
    // its full bounding box, which keeps collision and door logic simpler.
    private void carveRoom(Room room) {
        switch (room.shape) {
            case L_SHAPE:
                carveLShape(room);
                break;
            case T_SHAPE:
                carveTShape(room);
                break;
            case CROSS:
                carveCross(room);
                break;
            case RECTANGLE:
            default:
                carveRectangle(room);
                break;
        }
    }

    private void carveRectangle(Room room) {
        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                setRoomFloor(x, y);
            }
        }
    }

    private void carveLShape(Room room) {
        int verticalWidth = Math.max(3, room.width / 2);
        int horizontalHeight = Math.max(3, room.height / 2);

        boolean right = room.shapeVariant == 1 || room.shapeVariant == 3;
        boolean bottom = room.shapeVariant >= 2;

        int verticalStartX = right ? room.x + room.width - verticalWidth : room.x;
        int horizontalStartY = bottom ? room.y + room.height - horizontalHeight : room.y;

        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = verticalStartX; x < verticalStartX + verticalWidth; x++) {
                setRoomFloor(x, y);
            }
        }

        for (int y = horizontalStartY; y < horizontalStartY + horizontalHeight; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                setRoomFloor(x, y);
            }
        }
    }

    private void carveTShape(Room room) {
        int barThickness = Math.max(3, room.height / 3);
        int stemWidth = Math.max(3, room.width / 3);
        int stemX = room.x + room.width / 2 - stemWidth / 2;

        boolean upsideDown = room.shapeVariant % 2 == 1;
        int barY = upsideDown ? room.y + room.height - barThickness : room.y;

        for (int y = barY; y < barY + barThickness; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                setRoomFloor(x, y);
            }
        }

        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = stemX; x < stemX + stemWidth; x++) {
                setRoomFloor(x, y);
            }
        }
    }

    private void carveCross(Room room) {
        int armWidth = Math.max(3, room.width / 3);
        int armHeight = Math.max(3, room.height / 3);
        int verticalX = room.x + room.width / 2 - armWidth / 2;
        int horizontalY = room.y + room.height / 2 - armHeight / 2;

        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = verticalX; x < verticalX + armWidth; x++) {
                setRoomFloor(x, y);
            }
        }

        for (int y = horizontalY; y < horizontalY + armHeight; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                setRoomFloor(x, y);
            }
        }
    }

    private void setRoomFloor(int x, int y) {
        if (isInsideMap(x, y)) {
            map[y][x] = TileType.FLOOR;
        }
    }

    // Build the dungeon graph: one main path, then side branches and a few loops.
    // The hallway carving itself is handled later by connectTwoRooms().
    private void connectRoomsPacked() {
        if (rooms.size() < 2) {
            return;
        }

        buildMainPath();

        // Critical path: Start -> ... -> Boss -> Exit.
        for (int i = 1; i < mainPathRooms.size(); i++) {
            connectTwoRooms(mainPathRooms.get(i - 1), mainPathRooms.get(i));
        }

        connectSidePathsAndDeadEnds();
        addOptionalLoops();
    }

    // Choose a start room near the top-left and an exit room far away from it.
    // Rooms between them become the critical path through the dungeon.
    private void buildMainPath() {
        mainPathRooms.clear();

        if (rooms.isEmpty()) {
            return;
        }

        Room startRoom = findStartCandidateRoom();
        Room exitRoom = findFarthestRoom(startRoom);

        List<Room> candidates = new ArrayList<>(rooms);
        candidates.remove(startRoom);
        candidates.remove(exitRoom);

        candidates.sort((a, b) -> {
            double projectionA = projectedDistanceAlongLine(startRoom, exitRoom, a);
            double projectionB = projectedDistanceAlongLine(startRoom, exitRoom, b);
            return Double.compare(projectionA, projectionB);
        });

        int desiredMainCount = Math.max(
                settings.minMainPathRooms,
                rooms.size() * settings.mainPathPercent / 100
        );
        desiredMainCount = clamp(desiredMainCount, 2, rooms.size());

        mainPathRooms.add(startRoom);

        int middleRoomsNeeded = desiredMainCount - 2;
        for (int i = 0; i < candidates.size() && mainPathRooms.size() < middleRoomsNeeded + 1; i++) {
            Room candidate = candidates.get(i);
            candidate.pathRole = RoomPathRole.MAIN_PATH;
            mainPathRooms.add(candidate);
        }

        mainPathRooms.add(exitRoom);

        for (Room room : rooms) {
            room.pathRole = RoomPathRole.SIDE_PATH;
        }
        for (Room room : mainPathRooms) {
            room.pathRole = RoomPathRole.MAIN_PATH;
        }
    }

    private Room findStartCandidateRoom() {
        Room best = rooms.get(0);
        int bestScore = Integer.MAX_VALUE;

        for (Room room : rooms) {
            int score = room.x + room.y;
            if (score < bestScore) {
                bestScore = score;
                best = room;
            }
        }

        return best;
    }

    private Room findFarthestRoom(Room startRoom) {
        Room farthest = startRoom;
        int farthestDistance = -1;

        for (Room room : rooms) {
            if (room == startRoom) {
                continue;
            }

            int dx = room.centerX() - startRoom.centerX();
            int dy = room.centerY() - startRoom.centerY();
            int distance = dx * dx + dy * dy;

            if (distance > farthestDistance) {
                farthestDistance = distance;
                farthest = room;
            }
        }

        return farthest;
    }

    private double projectedDistanceAlongLine(Room startRoom, Room exitRoom, Room room) {
        double ax = startRoom.centerX();
        double ay = startRoom.centerY();
        double bx = exitRoom.centerX();
        double by = exitRoom.centerY();
        double px = room.centerX();
        double py = room.centerY();

        double vx = bx - ax;
        double vy = by - ay;
        double lengthSquared = vx * vx + vy * vy;

        if (lengthSquared == 0) {
            return 0;
        }

        return ((px - ax) * vx + (py - ay) * vy) / lengthSquared;
    }

    // Attach non-main rooms onto the critical path as optional branches.
    // Some branch endings are intentionally marked as dead ends for rewards/traps.
    private void connectSidePathsAndDeadEnds() {
        List<Room> unconnectedSideRooms = new ArrayList<>();

        for (Room room : rooms) {
            if (!mainPathRooms.contains(room)) {
                unconnectedSideRooms.add(room);
            }
        }

        // Build side branches off main path. Most branches intentionally end as dead ends.
        while (!unconnectedSideRooms.isEmpty()) {
            Room branchRoot = findNearestRoomInList(unconnectedSideRooms.get(0), mainPathRooms);
            Room previous = branchRoot;
            int branchLength = random.nextInt(settings.sideBranchMaxLength) + 1;

            for (int i = 0; i < branchLength && !unconnectedSideRooms.isEmpty(); i++) {
                Room next = findNearestRoomInList(previous, unconnectedSideRooms);
                connectTwoRooms(previous, next);

                previous = next;
                unconnectedSideRooms.remove(next);

                if (random.nextInt(100) < settings.deadEndChance) {
                    previous.pathRole = RoomPathRole.DEAD_END;
                    break;
                }
            }
        }
    }

    private Room findNearestRoomInList(Room fromRoom, List<Room> candidates) {
        Room nearest = candidates.get(0);
        int nearestDistance = Integer.MAX_VALUE;

        for (Room candidate : candidates) {
            if (candidate == fromRoom) {
                continue;
            }

            int distance = edgeDistance(fromRoom, candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    // Optional loops make the dungeon less linear. They are kept short so the
    // map still reads clearly.
    private void addOptionalLoops() {
        // A small number of short loops keeps maps from feeling too linear.
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

    // Connect two rooms once. This places doors at the room edges and asks the
    // pathfinder to carve a one-tile-wide hallway between them.
    private void connectTwoRooms(Room roomA, Room roomB) {
        int indexA = rooms.indexOf(roomA);
        int indexB = rooms.indexOf(roomB);
        if (indexA != -1 && indexB != -1 && isConnected(indexA, indexB)) {
            return;
        }

        ConnectionPoints connection = getConnectionPoints(roomA, roomB);

        carveHallwayPath(connection.startX, connection.startY, connection.endX, connection.endY);

        placeDoor(connection.startX, connection.startY);
        placeDoor(connection.endX, connection.endY);

        if (indexA != -1 && indexB != -1) {
            connectedRoomPairs.add(pairKey(indexA, indexB));
        }
    }

    // Door coordinates live on room floors. Start/end coordinates live just
    // outside the rooms, where the hallway is allowed to begin/end.
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

    // Pick the best door sides based on room positions. Horizontal connections
    // prefer left/right doors; vertical connections prefer top/bottom doors.
    private ConnectionPoints getConnectionPoints(Room a, Room b) {
        ConnectionPoints points = new ConnectionPoints();

        boolean horizontal = Math.abs(a.centerX() - b.centerX()) >= Math.abs(a.centerY() - b.centerY());

        if (horizontal) {
            int sideA = b.centerX() >= a.centerX() ? 1 : -1;
            int sideB = -sideA;
            int[] doorA = findDoorCandidateOnHorizontalSide(a, sideA, b.centerY());
            int[] doorB = findDoorCandidateOnHorizontalSide(b, sideB, doorA[1]);

            points.doorAX = doorA[0];
            points.doorAY = doorA[1];
            points.doorBX = doorB[0];
            points.doorBY = doorB[1];
            points.startX = points.doorAX + sideA;
            points.startY = points.doorAY;
            points.endX = points.doorBX + sideB;
            points.endY = points.doorBY;
        } else {
            int sideA = b.centerY() >= a.centerY() ? 1 : -1;
            int sideB = -sideA;
            int[] doorA = findDoorCandidateOnVerticalSide(a, sideA, b.centerX());
            int[] doorB = findDoorCandidateOnVerticalSide(b, sideB, doorA[0]);

            points.doorAX = doorA[0];
            points.doorAY = doorA[1];
            points.doorBX = doorB[0];
            points.doorBY = doorB[1];
            points.startX = points.doorAX;
            points.startY = points.doorAY + sideA;
            points.endX = points.doorBX;
            points.endY = points.doorBY + sideB;
        }

        points.startX = clamp(points.startX, 1, width - 2);
        points.startY = clamp(points.startY, 1, height - 2);
        points.endX = clamp(points.endX, 1, width - 2);
        points.endY = clamp(points.endY, 1, height - 2);

        return points;
    }

    private int[] findDoorCandidateOnHorizontalSide(Room room, int side, int preferredY) {
        int edgeX = side > 0 ? room.x + room.width - 1 : room.x;
        int bestY = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int y = room.y; y < room.y + room.height; y++) {
            if (!isCarvedRoomTile(edgeX, y, room)) {
                continue;
            }

            int outsideX = edgeX + side;
            if (!isInsideMap(outsideX, y) || isInsideAnyRoom(outsideX, y)) {
                continue;
            }

            int distance = Math.abs(y - preferredY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestY = y;
            }
        }

        if (bestY == -1) {
            bestY = clamp(room.centerY(), room.y, room.y + room.height - 1);
            setRoomFloor(edgeX, bestY);
        }

        return new int[] { edgeX, bestY };
    }

    private int[] findDoorCandidateOnVerticalSide(Room room, int side, int preferredX) {
        int edgeY = side > 0 ? room.y + room.height - 1 : room.y;
        int bestX = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int x = room.x; x < room.x + room.width; x++) {
            if (!isCarvedRoomTile(x, edgeY, room)) {
                continue;
            }

            int outsideY = edgeY + side;
            if (!isInsideMap(x, outsideY) || isInsideAnyRoom(x, outsideY)) {
                continue;
            }

            int distance = Math.abs(x - preferredX);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestX = x;
            }
        }

        if (bestX == -1) {
            bestX = clamp(room.centerX(), room.x, room.x + room.width - 1);
            setRoomFloor(bestX, edgeY);
        }

        return new int[] { bestX, edgeY };
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    // Door placement is intentionally conservative. A door only appears when
    // it sits on carved room floor and touches a hallway outside the room.
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


    // Small node class used by the hallway pathfinder priority queue.
    private static class PathNode implements Comparable<PathNode> {
        int x;
        int y;
        int priority;

        PathNode(int x, int y, int priority) {
            this.x = x;
            this.y = y;
            this.priority = priority;
        }

        @Override
        public int compareTo(PathNode other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    // A* hallway routing. This is what keeps corridors from cutting straight
    // through rooms when the packed layout gets crowded.
    private void carveHallwayPath(int startX, int startY, int endX, int endY) {
        if (!isInsideMap(startX, startY) || !isInsideMap(endX, endY)) {
            return;
        }

        int[][] cost = new int[height][width];
        int[][] previousX = new int[height][width];
        int[][] previousY = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cost[y][x] = Integer.MAX_VALUE;
                previousX[y][x] = -1;
                previousY[y][x] = -1;
            }
        }

        PriorityQueue<PathNode> open = new PriorityQueue<>();
        cost[startY][startX] = 0;
        open.add(new PathNode(startX, startY, heuristic(startX, startY, endX, endY)));

        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };

        while (!open.isEmpty()) {
            PathNode current = open.poll();

            if (current.x == endX && current.y == endY) {
                carveReconstructedPath(previousX, previousY, startX, startY, endX, endY);
                return;
            }

            for (int i = 0; i < dx.length; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (!canHallwayPathUseTile(nx, ny, startX, startY, endX, endY)) {
                    continue;
                }

                int newCost = cost[current.y][current.x] + hallwayPathCost(nx, ny);
                if (newCost < cost[ny][nx]) {
                    cost[ny][nx] = newCost;
                    previousX[ny][nx] = current.x;
                    previousY[ny][nx] = current.y;
                    int priority = newCost + heuristic(nx, ny, endX, endY);
                    open.add(new PathNode(nx, ny, priority));
                }
            }
        }

        // Fallback: use the old simple L carve if pathfinding cannot find a route.
        // This should be rare, but it prevents disconnected rooms on very crowded maps.
        carveHorizontalHallway(startX, endX, startY);
        carveVerticalHallway(startY, endY, endX);
    }

    private int heuristic(int x, int y, int targetX, int targetY) {
        return Math.abs(x - targetX) + Math.abs(y - targetY);
    }

    private boolean canHallwayPathUseTile(int x, int y, int startX, int startY, int endX, int endY) {
        if (!isInsideMap(x, y)) {
            return false;
        }

        if ((x == startX && y == startY) || (x == endX && y == endY)) {
            return true;
        }

        // Keep hallway routes out of room bounding boxes. This stops hallways from
        // slicing through rooms or creating broken gaps inside shaped rooms.
        return !isInsideAnyRoom(x, y);
    }

    private int hallwayPathCost(int x, int y) {
        TileType tile = map[y][x];

        if (tile == TileType.FLOOR && !isInsideAnyRoom(x, y)) {
            // Reusing an existing corridor is okay, but it should not be so cheap
            // that every connection collapses into one messy hallway clump.
            return 6;
        }

        if (touchesAnyRoom(x, y)) {
            // Avoid running parallel directly against room edges unless needed.
            return 14;
        }

        return 10;
    }

    private void carveReconstructedPath(int[][] previousX, int[][] previousY,
                                        int startX, int startY, int endX, int endY) {
        int x = endX;
        int y = endY;

        while (!(x == startX && y == startY)) {
            carveFloor(x, y);

            int px = previousX[y][x];
            int py = previousY[y][x];

            if (px == -1 || py == -1) {
                return;
            }

            x = px;
            y = py;
        }

        carveFloor(startX, startY);
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

    // The one rule for hallway carving: only carve outside room interiors.
    // This keeps rooms readable and prevents weird doubled-up room/hallway tiles.
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





    // Uses the room bounding boxes, not just carved floor. This is stricter,
    // but it helps keep hallways outside shaped rooms too.
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
        return isCarvedRoomTile(x + 1, y)
                || isCarvedRoomTile(x - 1, y)
                || isCarvedRoomTile(x, y + 1)
                || isCarvedRoomTile(x, y - 1);
    }

    // Checks whether a tile belongs to the actual carved shape of any room.
    // This matters for L/T/cross rooms, where not every bounding-box tile is floor.
    private boolean isCarvedRoomTile(int x, int y) {
        for (Room room : rooms) {
            if (isCarvedRoomTile(x, y, room)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCarvedRoomTile(int x, int y, Room room) {
        if (!isInsideMap(x, y)) {
            return false;
        }

        if (x < room.x
                || x >= room.x + room.width
                || y < room.y
                || y >= room.y + room.height) {
            return false;
        }

        TileType tile = map[y][x];
        return tile == TileType.FLOOR
                || tile == TileType.PLAYER
                || tile == TileType.EXIT
                || tile == TileType.ENEMY
                || tile == TileType.TREASURE
                || tile == TileType.TRAP
                || tile == TileType.BOSS
                || tile == TileType.KEY
                || tile == TileType.POTION;
    }

    // The boss room gets one locked door when possible, and a key is placed
    // somewhere earlier in the dungeon.
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

    // Important markers are placed last so they do not get overwritten by
    // room population.
    private void placePlayerAndExit() {
        Room startRoom = findRoomByType(RoomType.START);
        Room exitRoom = findRoomByType(RoomType.EXIT);

        if (startRoom == null || exitRoom == null) {
            return;
        }

        placeTileNearRoomCenter(startRoom, TileType.PLAYER);
        placeTileNearRoomCenter(exitRoom, TileType.EXIT);
    }

    private void placeTileNearRoomCenter(Room room, TileType tile) {
        int bestX = -1;
        int bestY = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int y = room.y; y < room.y + room.height; y++) {
            for (int x = room.x; x < room.x + room.width; x++) {
                if (map[y][x] != TileType.FLOOR) {
                    continue;
                }

                int distance = Math.abs(x - room.centerX()) + Math.abs(y - room.centerY());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (bestX != -1) {
            map[bestY][bestX] = tile;
        }
    }

    // Fill rooms based on their type. This keeps gameplay intent separate from
    // layout generation.
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
        List<int[]> validTiles = new ArrayList<>();

        for (int y = room.y + 1; y < room.y + room.height - 1; y++) {
            for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
                if (map[y][x] == TileType.FLOOR) {
                    validTiles.add(new int[] { x, y });
                }
            }
        }

        if (validTiles.isEmpty()) {
            return;
        }

        int[] position = validTiles.get(random.nextInt(validTiles.size()));
        map[position[1]][position[0]] = tile;
    }

    // Assign gameplay roles after the room graph exists. The main path gives us
    // a natural start, boss, and exit; side/dead-end rooms get varied content.
    private void assignRoomTypes() {
        if (rooms.isEmpty()) {
            return;
        }

        // Use the critical path for start, boss, and exit placement.
        // This makes the level read as: start -> main path -> boss -> exit,
        // with optional side branches and dead ends for rewards/traps.
        List<Room> typePath = mainPathRooms.isEmpty() ? rooms : mainPathRooms;

        for (Room room : rooms) {
            room.type = RoomType.COMBAT;
        }

        Room startRoom = typePath.get(0);
        Room exitRoom = typePath.get(typePath.size() - 1);

        startRoom.type = RoomType.START;
        exitRoom.type = RoomType.EXIT;

        if (typePath.size() > 2) {
            Room bossRoom = typePath.get(typePath.size() - 2);
            if (bossRoom != startRoom && bossRoom != exitRoom) {
                bossRoom.type = RoomType.BOSS;
            }
        }

        for (Room room : rooms) {
            if (room.type == RoomType.START || room.type == RoomType.EXIT || room.type == RoomType.BOSS) {
                continue;
            }

            if (room.pathRole == RoomPathRole.DEAD_END) {
                // Dead ends are good places for rewards or danger.
                int roll = random.nextInt(100);
                if (roll < 45) {
                    room.type = RoomType.TREASURE;
                } else if (roll < 75) {
                    room.type = RoomType.TRAP;
                } else {
                    room.type = RoomType.COMBAT;
                }
                continue;
            }

            int roll = random.nextInt(100);

            if (roll < settings.combatRoomChance) {
                room.type = RoomType.COMBAT;
            } else if (roll < settings.combatRoomChance + settings.treasureRoomChance) {
                room.type = RoomType.TREASURE;
            } else {
                room.type = RoomType.TRAP;
            }
        }
    }

    // Secret rooms are optional. If the map is too packed, we simply skip it
    // instead of forcing a bad room placement.
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
        carveHallwayPath(connection.startX, connection.startY, connection.endX, connection.endY);
        placeDoor(connection.endX, connection.endY);
        if (isInsideMap(connection.startX, connection.startY)
                && map[connection.startY][connection.startX] == TileType.FLOOR
                && !isInsideAnyRoom(connection.startX, connection.startY)
                && touchesAnyRoom(connection.startX, connection.startY)) {
            map[connection.startY][connection.startX] = TileType.SECRET_DOOR;
        }
    }

    // Final sanity check. A good map must let the player reach the exit.
    public boolean isExitReachable() {
        int startX = -1;
        int startY = -1;
        int exitX = -1;
        int exitY = -1;

        TileType startTile = getValidationStartTile();
        TileType targetTile = getValidationTargetTile();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y][x] == startTile) {
                    startX = x;
                    startY = y;
                } else if (map[y][x] == targetTile) {
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

    private TileType getValidationStartTile() {
        return TileType.PLAYER;
    }

    private TileType getValidationTargetTile() {
        return TileType.EXIT;
    }

    // Simple recursive flood fill. The maps are small enough that this is easy
    // to read and safe for the current project sizes.
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

    // Useful for debugging or exporting a quick text version of the dungeon.
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




    // The renderer reads the tile map directly. For a small project, this keeps
    // the code simple; larger projects might return a defensive copy instead.
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
        return isCarvedRoomTile(x, y);
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
