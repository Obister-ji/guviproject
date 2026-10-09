package shop;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // falls back to the default Swing look
        }
        Store store = Store.open();
        SwingUtilities.invokeLater(() -> new App(store).show());
    }
}
