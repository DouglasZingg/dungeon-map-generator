import java.awt.Color;

public enum DungeonTheme {
    CLASSIC(
            "Classic",
            new Color(45, 45, 45),
            new Color(180, 180, 180),
            new Color(120, 120, 120),
            Color.ORANGE,
            Color.RED,
            new Color(30, 230, 70),
            Color.BLUE,
            Color.MAGENTA,
            Color.YELLOW,
            Color.PINK,
            Color.BLACK,
            Color.CYAN,
            Color.WHITE,
            Color.GRAY
    ),
    CRYPT(
            "Crypt",
            new Color(30, 34, 40),
            new Color(125, 125, 135),
            new Color(80, 80, 90),
            new Color(120, 85, 45),
            new Color(150, 30, 30),
            new Color(30, 230, 70),
            new Color(30, 100, 255),
            new Color(160, 80, 200),
            new Color(210, 190, 90),
            new Color(120, 70, 120),
            new Color(25, 20, 20),
            new Color(160, 220, 220),
            new Color(210, 210, 230),
            new Color(70, 70, 80)
    ),
    CAVE(
            "Cave",
            new Color(55, 45, 35),
            new Color(145, 115, 80),
            new Color(95, 75, 55),
            new Color(135, 90, 45),
            new Color(150, 45, 35),
            new Color(30, 230, 70),
            new Color(30, 100, 255),
            new Color(190, 70, 70),
            new Color(230, 190, 60),
            new Color(110, 70, 50),
            new Color(35, 25, 20),
            new Color(80, 210, 200),
            new Color(230, 230, 210),
            new Color(85, 75, 65)
    ),
    ICE(
            "Ice",
            new Color(25, 50, 70),
            new Color(170, 220, 235),
            new Color(100, 170, 200),
            new Color(180, 220, 240),
            new Color(90, 120, 255),
            new Color(30, 230, 70),
            new Color(30, 100, 255),
            new Color(130, 80, 220),
            new Color(240, 240, 150),
            new Color(120, 200, 230),
            new Color(20, 30, 45),
            new Color(120, 255, 255),
            Color.WHITE,
            new Color(80, 120, 145)
    ),
    LAVA(
            "Lava",
            new Color(35, 25, 20),
            new Color(110, 70, 50),
            new Color(75, 45, 35),
            new Color(210, 110, 20),
            new Color(255, 50, 20),
            new Color(30, 230, 70),
            new Color(30, 100, 255),
            new Color(255, 70, 40),
            new Color(255, 210, 60),
            new Color(230, 80, 20),
            new Color(15, 10, 10),
            new Color(70, 220, 210),
            new Color(255, 230, 180),
            new Color(90, 70, 60)
    );

    private final String displayName;
    public final Color wall;
    public final Color roomFloor;
    public final Color hallwayFloor;
    public final Color door;
    public final Color lockedDoor;
    public final Color player;
    public final Color exit;
    public final Color enemy;
    public final Color treasure;
    public final Color trap;
    public final Color boss;
    public final Color key;
    public final Color potion;
    public final Color secretDoor;

    DungeonTheme(String displayName, Color wall, Color roomFloor, Color hallwayFloor, Color door, Color lockedDoor,
                 Color player, Color exit, Color enemy, Color treasure, Color trap, Color boss, Color key,
                 Color potion, Color secretDoor) {
        this.displayName = displayName;
        this.wall = wall;
        this.roomFloor = roomFloor;
        this.hallwayFloor = hallwayFloor;
        this.door = door;
        this.lockedDoor = lockedDoor;
        this.player = player;
        this.exit = exit;
        this.enemy = enemy;
        this.treasure = treasure;
        this.trap = trap;
        this.boss = boss;
        this.key = key;
        this.potion = potion;
        this.secretDoor = secretDoor;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
