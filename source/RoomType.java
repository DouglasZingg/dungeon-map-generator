// Gameplay purpose of a room.
// The tile population step uses this to decide enemies, loot, traps, etc.
public enum RoomType {
    START,
    COMBAT,
    TREASURE,
    TRAP,
    BOSS,
    SECRET,
    EXIT
}