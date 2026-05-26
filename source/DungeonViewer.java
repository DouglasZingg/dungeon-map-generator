import javax.swing.*;
import java.awt.*;
import javax.swing.event.ChangeListener;

public class DungeonViewer extends JFrame {
    private final DungeonSettings settings;
    private DungeonGenerator dungeon;

    private final DungeonPanel dungeonPanel;
    private final JSpinner roomCountSpinner;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner seedSpinner;

    public DungeonViewer() {
        settings = new DungeonSettings();

        setTitle("Dungeon Map Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(0, 2, 8, 8));

        roomCountSpinner = new JSpinner(new SpinnerNumberModel(settings.maxRooms, 3, 50, 1));
        widthSpinner = new JSpinner(new SpinnerNumberModel(60, 20, 150, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 80, 1));
        seedSpinner = new JSpinner(new SpinnerNumberModel(12345L, 1L, Long.MAX_VALUE, 1L));

        controls.add(new JLabel("Rooms:"));
        controls.add(roomCountSpinner);

        controls.add(new JLabel("Map Width:"));
        controls.add(widthSpinner);

        controls.add(new JLabel("Map Height:"));
        controls.add(heightSpinner);

        controls.add(new JLabel("Seed:"));
        controls.add(seedSpinner);

        JButton generateButton = new JButton("Generate Dungeon");

        ChangeListener autoGenerate = e -> generateDungeon();

        roomCountSpinner.addChangeListener(autoGenerate);
        widthSpinner.addChangeListener(autoGenerate);
        heightSpinner.addChangeListener(autoGenerate);
        seedSpinner.addChangeListener(autoGenerate);

        generateButton.addActionListener(e -> generateDungeon());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(controls, BorderLayout.NORTH);
        leftPanel.add(generateButton, BorderLayout.SOUTH);

        dungeonPanel = new DungeonPanel();

        add(leftPanel, BorderLayout.WEST);
        add(dungeonPanel, BorderLayout.CENTER);

        generateDungeon();
    }

    private void generateDungeon() {
        settings.maxRooms = ((Number) roomCountSpinner.getValue()).intValue();
        settings.seed = ((Number) seedSpinner.getValue()).longValue();

        int mapWidth = ((Number) widthSpinner.getValue()).intValue();
        int mapHeight = ((Number) heightSpinner.getValue()).intValue();

        dungeon = new DungeonGenerator(mapWidth, mapHeight, settings);
        dungeon.generate();

        dungeonPanel.setDungeon(dungeon);
    }
}