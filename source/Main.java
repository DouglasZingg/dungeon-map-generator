public class Main {
    public static void main(String[] args) {
        DungeonSettings settings = new DungeonSettings();
        settings.seed = 12345L;

        DungeonGenerator dungeon = new DungeonGenerator(60, 30, settings);

        dungeon.generate();
        dungeon.printMap();
        dungeon.printLegend();
        dungeon.printSummary();

        System.out.println();
        System.out.println("Seed: " + settings.seed);
    }
}