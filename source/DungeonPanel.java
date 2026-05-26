import javax.swing.*;
import java.awt.*;

public class DungeonPanel extends JPanel {
    private DungeonGenerator dungeon;

    private final int tileSize = 16;

    public void setDungeon(DungeonGenerator dungeon) {
        this.dungeon = dungeon;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (dungeon == null) {
            return;
        }

        TileType[][] map = dungeon.getMap();

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {

                TileType tile = map[y][x];

                switch (tile) {
                    case WALL:
                        g.setColor(Color.DARK_GRAY);
                        break;

                    case FLOOR:
                        g.setColor(Color.LIGHT_GRAY);
                        break;

                    case DOOR:
                        g.setColor(Color.ORANGE);
                        break;

                    case LOCKED_DOOR:
                        g.setColor(Color.RED);
                        break;

                    case PLAYER:
                        g.setColor(Color.GREEN);
                        break;

                    case EXIT:
                        g.setColor(Color.BLUE);
                        break;

                    case ENEMY:
                        g.setColor(Color.MAGENTA);
                        break;

                    case TREASURE:
                        g.setColor(Color.YELLOW);
                        break;

                    case TRAP:
                        g.setColor(Color.PINK);
                        break;

                    case BOSS:
                        g.setColor(Color.BLACK);
                        break;

                    case KEY:
                        g.setColor(Color.CYAN);
                        break;

                    case POTION:
                        g.setColor(Color.WHITE);
                        break;

                    case SECRET_DOOR:
                        g.setColor(Color.GRAY);
                        break;

                    default:
                        g.setColor(Color.GRAY);
                        break;
                }

                g.fillRect(
                        x * tileSize,
                        y * tileSize,
                        tileSize,
                        tileSize
                );
            }
        }
    }
}