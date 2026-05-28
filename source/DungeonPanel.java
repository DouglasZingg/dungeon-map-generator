import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class DungeonPanel extends JPanel {
    private DungeonGenerator dungeon;
    private DungeonTheme theme = DungeonTheme.CLASSIC;
    private int tileSize = 16;
    private int revealedTiles = Integer.MAX_VALUE;
    private Timer animationTimer;

    public void setDungeon(DungeonGenerator dungeon) {
        this.dungeon = dungeon;
        this.revealedTiles = Integer.MAX_VALUE;
        updatePreferredSize();
        repaint();
    }

    public void setTheme(DungeonTheme theme) {
        this.theme = theme;
        repaint();
    }

    public DungeonTheme getTheme() {
        return theme;
    }

    public void startRevealAnimation() {
        if (dungeon == null) {
            return;
        }

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        revealedTiles = 0;
        TileType[][] map = dungeon.getMap();
        int totalTiles = map.length * map[0].length;
        int tilesPerFrame = Math.max(8, totalTiles / 120);

        animationTimer = new Timer(15, e -> {
            revealedTiles += tilesPerFrame;

            if (revealedTiles >= totalTiles) {
                revealedTiles = Integer.MAX_VALUE;
                animationTimer.stop();
            }

            repaint();
        });

        animationTimer.start();
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
                int tileIndex = y * map[y].length + x;

                if (tileIndex > revealedTiles) {
                    g.setColor(Color.BLACK);
                } else {
                    TileType tile = map[y][x];
                    g.setColor(TileColorHelper.getColor(tile, dungeon, x, y, theme));
                }

                g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);

                g.setColor(new Color(0, 0, 0, 90));
                g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
        updatePreferredSize();
        repaint();
    }

    public int getTileSize() {
        return tileSize;
    }

    private void updatePreferredSize() {
        if (dungeon == null) {
            return;
        }

        TileType[][] map = dungeon.getMap();
        int panelWidth = map[0].length * tileSize;
        int panelHeight = map.length * tileSize;

        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate();
    }
}
