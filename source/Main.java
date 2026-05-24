import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DungeonViewer viewer = new DungeonViewer();
            viewer.setVisible(true);
        });
    }
}