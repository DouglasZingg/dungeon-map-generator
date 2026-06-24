public class DungeonSettings {
    // Packed-room defaults create a fuller, battle-map style layout.
    public int maxRooms = 22;
    public int maxRoomPlacementAttempts = 900;

    // Kept for compatibility with older controls/code. The packed algorithm mostly uses BSP leaves.
    public int roomPadding = 1;

    public int minRoomWidth = 4;
    public int maxRoomWidth = 11;

    public int minRoomHeight = 4;
    public int maxRoomHeight = 9;

    public int minEnemiesPerCombatRoom = 1;
    public int maxEnemiesPerCombatRoom = 3;

    public int minTrapsPerTrapRoom = 1;
    public int maxTrapsPerTrapRoom = 3;

    public long seed = System.currentTimeMillis();

    public int maxGenerationAttempts = 50;


    public int combatRoomChance = 60;
    public int treasureRoomChance = 25;
    public int trapRoomChance = 15;

    // New packed algorithm tuning.
    public int extraConnectionChance = 35;
    public int extraConnectionMaxDistance = 10;

    // Path layout tuning.
    // Higher mainPathPercent makes the critical route longer.
    // Higher deadEndChance creates more one-room side branches.
    public int mainPathPercent = 45;
    public int minMainPathRooms = 4;
    public int sideBranchMaxLength = 3;
    public int deadEndChance = 65;
}
