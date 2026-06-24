import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;

public class DungeonViewer extends JFrame {
    private final DungeonSettings settings;
    private DungeonGenerator dungeon;

    private final DungeonPanel dungeonPanel;
    private final JSpinner roomCountSpinner;
    private final JComboBox<MapSizePreset> mapSizeComboBox;
    private final JSpinner seedSpinner;
    private final JSpinner combatChanceSpinner;
    private final JSpinner treasureChanceSpinner;
    private final JSpinner trapChanceSpinner;
    private final JSpinner mainPathPercentSpinner;
    private final JSpinner deadEndChanceSpinner;
    private final JSpinner sideBranchLengthSpinner;
    private final JSlider zoomSlider;
    private final JComboBox<DungeonTheme> themeComboBox;
    private final JLabel statusLabel;
    private JPanel legendPanel;

    public DungeonViewer() {
        settings = new DungeonSettings();
        dungeonPanel = new DungeonPanel();

        setTitle("Dungeon Map Generator - Single Floor Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(8, 8));

        mapSizeComboBox = new JComboBox<>(MapSizePreset.values());
        mapSizeComboBox.setSelectedItem(MapSizePreset.MEDIUM_36x48);

        roomCountSpinner = new JSpinner(new SpinnerNumberModel(MapSizePreset.MEDIUM_36x48.getRecommendedRooms(), 3, 100, 1));
        seedSpinner = new JSpinner(new SpinnerNumberModel(12345L, 1L, Long.MAX_VALUE, 1L));
        combatChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.combatRoomChance, 0, 100, 5));
        treasureChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.treasureRoomChance, 0, 100, 5));
        trapChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.trapRoomChance, 0, 100, 5));
        mainPathPercentSpinner = new JSpinner(new SpinnerNumberModel(settings.mainPathPercent, 20, 80, 5));
        deadEndChanceSpinner = new JSpinner(new SpinnerNumberModel(settings.deadEndChance, 0, 100, 5));
        sideBranchLengthSpinner = new JSpinner(new SpinnerNumberModel(settings.sideBranchMaxLength, 1, 6, 1));
        zoomSlider = new JSlider(4, 40, dungeonPanel.getTileSize());
        zoomSlider.setMajorTickSpacing(12);
        zoomSlider.setMinorTickSpacing(4);
        zoomSlider.setPaintTicks(true);
        themeComboBox = new JComboBox<>(DungeonTheme.values());
        statusLabel = new JLabel("Ready");

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createControlPanel(), BorderLayout.WEST);
        add(createMapAreaPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        registerListeners();
        generateDungeon(false, true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        JLabel title = new JLabel("Dungeon Map Generator");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Single-floor packed dungeon generator with themes, room shapes, paths, branches, dead ends, and PNG export.");
        subtitle.setForeground(new Color(90, 90, 90));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(subtitle);

        header.add(titleStack, BorderLayout.WEST);
        return header;
    }

    private JPanel createMapAreaPanel() {
        JPanel mapArea = new JPanel(new BorderLayout(8, 8));
        mapArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.add(new JLabel("Map Preview"), BorderLayout.WEST);
        topBar.add(createTopLegendPanel(), BorderLayout.EAST);

        mapArea.add(topBar, BorderLayout.NORTH);
        mapArea.add(new JScrollPane(dungeonPanel), BorderLayout.CENTER);
        return mapArea;
    }

    private JScrollPane createControlPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        leftPanel.setPreferredSize(new Dimension(360, 700));

        leftPanel.add(createActionsSection());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createMapSettingsSection());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createGenerationSection());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(createDisplaySection());
        leftPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(leftPanel);
        scrollPane.setPreferredSize(new Dimension(380, 750));
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));
        return scrollPane;
    }

    private JPanel createActionsSection() {
        JPanel section = createSection("Actions");
        section.setLayout(new GridLayout(0, 1, 6, 6));

        JButton generateButton = new JButton("Generate Dungeon");
        generateButton.setFont(generateButton.getFont().deriveFont(Font.BOLD, 14f));
        generateButton.addActionListener(e -> generateDungeon(true, true));

        JButton seedButton = new JButton("Regenerate From Seed");
        seedButton.addActionListener(e -> generateDungeon(true, false));

        JButton exportImageButton = new JButton("Save PNG");
        exportImageButton.addActionListener(e -> exportDungeonImage());

        section.add(generateButton);
        section.add(seedButton);
        section.add(exportImageButton);
        return section;
    }

    private JPanel createMapSettingsSection() {
        JPanel section = createFormSection("Dungeon Settings");
        addSettingRow(section, 0, "Map Size", mapSizeComboBox);
        addSettingRow(section, 1, "Rooms", roomCountSpinner);
        addSettingRow(section, 2, "Seed", seedSpinner);
        return section;
    }

    private JPanel createGenerationSection() {
        JPanel section = createFormSection("Map Generation");
        addSettingRow(section, 0, "Combat %", combatChanceSpinner);
        addSettingRow(section, 1, "Treasure %", treasureChanceSpinner);
        addSettingRow(section, 2, "Trap %", trapChanceSpinner);
        addSettingRow(section, 3, "Main Path %", mainPathPercentSpinner);
        addSettingRow(section, 4, "Dead End %", deadEndChanceSpinner);
        addSettingRow(section, 5, "Side Branch Max", sideBranchLengthSpinner);
        return section;
    }

    private JPanel createDisplaySection() {
        JPanel section = createFormSection("Display");
        addSettingRow(section, 0, "Theme", themeComboBox);
        addSettingRow(section, 1, "Zoom", zoomSlider);
        return section;
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel createFormSection(String title) {
        JPanel panel = createSection(title);
        panel.setLayout(new GridBagLayout());
        return panel;
    }

    private void addSettingRow(JPanel panel, int row, String labelText, java.awt.Component control) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(3, 2, 3, 8);

        GridBagConstraints controlConstraints = new GridBagConstraints();
        controlConstraints.gridx = 1;
        controlConstraints.gridy = row;
        controlConstraints.weightx = 1.0;
        controlConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlConstraints.insets = new Insets(3, 2, 3, 2);

        panel.add(new JLabel(labelText + ":"), labelConstraints);
        panel.add(control, controlConstraints);
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void registerListeners() {
        mapSizeComboBox.addActionListener(e -> applyMapSizePreset());
        zoomSlider.addChangeListener(e -> dungeonPanel.setTileSize(zoomSlider.getValue()));
        themeComboBox.addActionListener(e -> {
            dungeonPanel.setTheme((DungeonTheme) themeComboBox.getSelectedItem());
            refreshLegendPanel();
            updateStatus();
        });
        applyMapSizePreset();
    }

    private void applyMapSizePreset() {
        MapSizePreset preset = (MapSizePreset) mapSizeComboBox.getSelectedItem();
        if (preset == null) {
            return;
        }
        roomCountSpinner.setValue(preset.getRecommendedRooms());
        statusLabel.setText("Map size selected: " + preset + ". Press Generate Dungeon to apply.");
    }

    private void generateDungeon(boolean animate, boolean createNewSeed) {
        settings.maxRooms = getSpinnerInt(roomCountSpinner);
        settings.seed = createNewSeed ? System.currentTimeMillis() : getSpinnerLong(seedSpinner);
        seedSpinner.setValue(settings.seed);

        settings.combatRoomChance = getSpinnerInt(combatChanceSpinner);
        settings.treasureRoomChance = getSpinnerInt(treasureChanceSpinner);
        settings.trapRoomChance = getSpinnerInt(trapChanceSpinner);
        settings.mainPathPercent = getSpinnerInt(mainPathPercentSpinner);
        settings.deadEndChance = getSpinnerInt(deadEndChanceSpinner);
        settings.sideBranchMaxLength = getSpinnerInt(sideBranchLengthSpinner);

        MapSizePreset preset = (MapSizePreset) mapSizeComboBox.getSelectedItem();
        if (preset == null) {
            preset = MapSizePreset.MEDIUM_36x48;
        }

        dungeon = new DungeonGenerator(preset.getWidth(), preset.getHeight(), settings);
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
                "Algorithm: Main + Side Paths | Size: " + dungeon.getWidth() + "x" + dungeon.getHeight()
                        + " | Rooms: " + dungeon.getRoomCount()
                        + " | Seed: " + settings.seed
                        + " | Theme: " + themeComboBox.getSelectedItem()
                        + " | Exit reachable: " + dungeon.isExitReachable()
        );
    }

    private JPanel createTopLegendPanel() {
        legendPanel = new JPanel(new GridLayout(0, 7, 8, 3));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));
        legendPanel.setPreferredSize(new Dimension(660, 78));
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
        JPanel item = new JPanel(new BorderLayout(4, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(20, 16));
        swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JLabel textLabel = new JLabel(label);
        textLabel.setFont(textLabel.getFont().deriveFont(10f));

        item.add(swatch, BorderLayout.WEST);
        item.add(textLabel, BorderLayout.CENTER);
        return item;
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
