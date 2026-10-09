package shop;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** One tab of a dashboard. refresh() re-reads the store; it runs whenever the tab is selected. */
abstract class Tab extends JPanel {
    Tab() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    abstract void refresh();
}
