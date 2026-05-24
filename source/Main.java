public class Main {
    public static void main(String[] args) {
        DungeonSettings settings = new DungeonSettings();

        settings.seed = System.currentTimeMillis();

        settings.combatRoomChance = 70;
        settings.treasureRoomChance = 20;
        settings.trapRoomChance = 10;

        DungeonGenerator dungeon = new DungeonGenerator(60, 30, settings);

        dungeon.generate();
        dungeon.printMap();
        dungeon.printLegend();
        dungeon.printSummary();

        System.out.println();
        System.out.println("Exit reachable: " + dungeon.isExitReachable());

        System.out.println();
        System.out.println("Seed: " + settings.seed);
    }
}