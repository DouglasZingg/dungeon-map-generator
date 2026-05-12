public class Main {
    public static void main(String[] args) {
        DungeonSettings settings = new DungeonSettings();
        DungeonGenerator dungeon = new DungeonGenerator(60, 30, settings);

        dungeon.generate();
        dungeon.printMap();
        dungeon.printLegend();
    }
}