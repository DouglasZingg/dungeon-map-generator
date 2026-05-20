public class DungeonSettings {
    public int maxRooms = 12;

    public int minRoomWidth = 4;
    public int maxRoomWidth = 9;

    public int minRoomHeight = 4;
    public int maxRoomHeight = 8;

    public int minEnemiesPerCombatRoom = 1;
    public int maxEnemiesPerCombatRoom = 3;

    public int minTrapsPerTrapRoom = 1;
    public int maxTrapsPerTrapRoom = 3;

    public long seed = System.currentTimeMillis();

    public int maxGenerationAttempts = 25;
}