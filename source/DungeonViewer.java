import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

public class DungeonViewer extends JFrame {
    private final DungeonSettings settings;
    private DungeonGenerator dungeon;

    private final DungeonPanel dungeonPanel;
    private final JSpinner roomCountSpinner;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner seedSpinner;
    private final JSpinner combatChanceSpinner;
    private final JSpinner treasureChanceSpinner;
    private final JSpinner trapChanceSpinner;
    private final JSlider zoomSlider;
    private final JComboBox<DungeonTheme> themeComboBox;
    private final JLabel statusLabel;
    private JPanel legendPanel;

    public DungeonViewer() {
        settings = new DungeonSettings();
        dungeonPanel = new DungeonPanel();

        setTitle("Dungeon Map Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        roomCountSpinner = new JSpinner(new SpinnerNumberModel(settings.maxRooms, 3, 50, 1));
        widthSpinner = new JSpinner(new SpinnerNumberModel(60, 20, 150, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 80, 1));
        seedSpinner = new JSpinner(new SpinnerNumberModel(12345L, 1L, Long.MAX_VALUE, 1L));
        combatChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.combatRoomChance, 0, 100, 5));
        treasureChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.treasureRoomChance, 0, 100, 5));
        trapChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.trapRoomChance, 0, 100, 5));
        zoomSlider = new JSlider(4, 40, dungeonPanel.getTileSize());
        themeComboBox = new JComboBox<>(DungeonTheme.values());
        statusLabel = new JLabel("Ready");

        add(createLeftPanel(), BorderLayout.WEST);
        add(new JScrollPane(dungeonPanel), BorderLayout.CENTER);

        registerListeners();
        generateDungeon(false);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new GridLayout(0, 2, 8, 8));
        controls.add(new JLabel("Rooms:"));
        controls.add(roomCountSpinner);
        controls.add(new JLabel("Map Width:"));
        controls.add(widthSpinner);
        controls.add(new JLabel("Map Height:"));
        controls.add(heightSpinner);
        controls.add(new JLabel("Combat %:"));
        controls.add(combatChanceSpinner);
        controls.add(new JLabel("Treasure %:"));
        controls.add(treasureChanceSpinner);
        controls.add(new JLabel("Trap %:"));
        controls.add(trapChanceSpinner);
        controls.add(new JLabel("Theme:"));
        controls.add(themeComboBox);
        controls.add(new JLabel("Zoom:"));
        controls.add(zoomSlider);
        controls.add(new JLabel("Seed:"));
        controls.add(seedSpinner);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        JButton generateButton = new JButton("Generate Dungeon");
        generateButton.addActionListener(e -> generateDungeon(false));

        JButton animateButton = new JButton("Animate Generation");
        animateButton.addActionListener(e -> generateDungeon(true));

        JButton randomSeedButton = new JButton("Random Seed");
        randomSeedButton.addActionListener(e -> {
            seedSpinner.setValue(System.currentTimeMillis());
            generateDungeon(true);
        });

        JButton saveButton = new JButton("Save Map");
        saveButton.addActionListener(e -> saveMapToFile());

        JButton exportImageButton = new JButton("Export PNG");
        exportImageButton.addActionListener(e -> exportDungeonImage());

        buttonPanel.add(generateButton);
        buttonPanel.add(animateButton);
        buttonPanel.add(randomSeedButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(exportImageButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(createLegendPanel(), BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        leftPanel.add(controls, BorderLayout.NORTH);
        leftPanel.add(bottomPanel, BorderLayout.SOUTH);
        return leftPanel;
    }

    private void registerListeners() {
        ChangeListener autoGenerate = e -> generateDungeon(false);

        roomCountSpinner.addChangeListener(autoGenerate);
        widthSpinner.addChangeListener(autoGenerate);
        heightSpinner.addChangeListener(autoGenerate);
        seedSpinner.addChangeListener(autoGenerate);
        combatChanceSpinner.addChangeListener(autoGenerate);
        treasureChanceSpinner.addChangeListener(autoGenerate);
        trapChanceSpinner.addChangeListener(autoGenerate);

        zoomSlider.addChangeListener(e -> dungeonPanel.setTileSize(zoomSlider.getValue()));
        themeComboBox.addActionListener(e -> {
            dungeonPanel.setTheme((DungeonTheme) themeComboBox.getSelectedItem());
            refreshLegendPanel();
            updateStatus();
        });
    }

    private void generateDungeon(boolean animate) {
        settings.maxRooms = getSpinnerInt(roomCountSpinner);
        settings.seed = getSpinnerLong(seedSpinner);
        settings.combatRoomChance = getSpinnerInt(combatChanceSpinner);
        settings.treasureRoomChance = getSpinnerInt(treasureChanceSpinner);
        settings.trapRoomChance = getSpinnerInt(trapChanceSpinner);

        int mapWidth = getSpinnerInt(widthSpinner);
        int mapHeight = getSpinnerInt(heightSpinner);

        dungeon = new DungeonGenerator(mapWidth, mapHeight, settings);
        dungeon.generate();

        dungeonPanel.setTheme((DungeonTheme) themeComboBox.getSelectedItem());
        dungeonPanel.setDungeon(dungeon);

        if (animate) {
            dungeonPanel.startRevealAnimation();
        }

        updateStatus();
    }

    private int getSpinnerInt(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private long getSpinnerLong(JSpinner spinner) {
        return ((Number) spinner.getValue()).longValue();
    }

    private void updateStatus() {
        if (dungeon == null) {
            statusLabel.setText("Ready");
            return;
        }

        statusLabel.setText(
                "Rooms: " + dungeon.getRoomCount()
                        + " | Seed: " + settings.seed
                        + " | Theme: " + themeComboBox.getSelectedItem()
                        + " | Exit reachable: " + dungeon.isExitReachable()
        );
    }

    private JPanel createLegendPanel() {
        legendPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));
        refreshLegendPanel();
        return legendPanel;
    }

    private void refreshLegendPanel() {
        if (legendPanel == null) {
            return;
        }

        DungeonTheme theme = (DungeonTheme) themeComboBox.getSelectedItem();
        if (theme == null) {
            theme = DungeonTheme.CLASSIC;
        }

        legendPanel.removeAll();
        legendPanel.add(createLegendItem("Wall", theme.wall));
        legendPanel.add(createLegendItem("Room Floor", theme.roomFloor));
        legendPanel.add(createLegendItem("Hallway Floor", theme.hallwayFloor));
        legendPanel.add(createLegendItem("Door", theme.door));
        legendPanel.add(createLegendItem("Locked Door", theme.lockedDoor));
        legendPanel.add(createLegendItem("Player", theme.player));
        legendPanel.add(createLegendItem("Exit", theme.exit));
        legendPanel.add(createLegendItem("Enemy", theme.enemy));
        legendPanel.add(createLegendItem("Treasure", theme.treasure));
        legendPanel.add(createLegendItem("Trap", theme.trap));
        legendPanel.add(createLegendItem("Boss", theme.boss));
        legendPanel.add(createLegendItem("Key", theme.key));
        legendPanel.add(createLegendItem("Potion", theme.potion));
        legendPanel.add(createLegendItem("Secret Door", theme.secretDoor));
        legendPanel.revalidate();
        legendPanel.repaint();
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel item = new JPanel(new BorderLayout(6, 0));

        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(18, 18));
        swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        item.add(swatch, BorderLayout.WEST);
        item.add(new JLabel(label), BorderLayout.CENTER);

        return item;
    }

    private void saveMapToFile() {
        if (dungeon == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("dungeon_map.txt"));

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                writer.write(dungeon.getMapAsString());
                statusLabel.setText("Saved map to: " + fileChooser.getSelectedFile().getName());
            } catch (Exception ex) {
                statusLabel.setText("Could not save map.");
                ex.printStackTrace();
            }
        }
    }

    private void exportDungeonImage() {
        if (dungeon == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("dungeon_map.png"));

        int result = fileChooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            TileType[][] map = dungeon.getMap();
            int tileSize = dungeonPanel.getTileSize();
            DungeonTheme theme = dungeonPanel.getTheme();

            BufferedImage image = new BufferedImage(
                    map[0].length * tileSize,
                    map.length * tileSize,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = image.createGraphics();

            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[y].length; x++) {
                    TileType tile = map[y][x];
                    g2d.setColor(TileColorHelper.getColor(tile, dungeon, x, y, theme));
                    g2d.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
                    g2d.setColor(new Color(0, 0, 0, 90));
                    g2d.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
                }
            }

            g2d.dispose();
            ImageIO.write(image, "png", fileChooser.getSelectedFile());
            statusLabel.setText("Exported PNG successfully.");
        } catch (Exception ex) {
            statusLabel.setText("Failed to export PNG.");
            ex.printStackTrace();
        }
    }
}
