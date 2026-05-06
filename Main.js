public class Main {
    public static void main(String[] args) {
        DungeonGenerator dungeon = new DungeonGenerator(60, 30);

        dungeon.generate();
        dungeon.printMap();
    }
}