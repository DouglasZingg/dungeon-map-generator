import javax.swing.SwingUtilities;

// App entry point. SwingUtilities makes sure the UI starts on the Swing event thread.
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DungeonViewer viewer = new DungeonViewer();
            viewer.setVisible(true);
        });
    }
}