package shop;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Small Swing helpers so the dashboards don't repeat the same boilerplate. */
final class Ui {
    private Ui() { }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    /** Id (first column) of the selected row, or -1 after telling the user to pick one. */
    static int selectedId(Component parent, JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) {
            info(parent, "Select a row first.");
            return -1;
        }
        return (Integer) t.getValueAt(r, 0);
    }

    static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    static JPanel bar(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (JComponent c : items) p.add(c);
        return p;
    }

    static JTextArea textArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        a.setMargin(new java.awt.Insets(8, 8, 8, 8));
        return a;
    }

    static JTabbedPane tabs(String[] titles, Tab[] tabs) {
        JTabbedPane tp = new JTabbedPane();
        for (int i = 0; i < tabs.length; i++) tp.addTab(titles[i], tabs[i]);
        tp.addChangeListener(e -> {
            Component c = tp.getSelectedComponent();
            if (c instanceof Tab) ((Tab) c).refresh();
        });
        tabs[0].refresh();
        return tp;
    }

    // ---- dialogs ----

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Online Shop", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Online Shop", JOptionPane.WARNING_MESSAGE);
    }

    static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Online Shop", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    static void text(Component parent, String title, String body) {
        JTextArea a = textArea();
        a.setText(body);
        a.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(a);
        sp.setPreferredSize(new Dimension(520, 340));
        JOptionPane.showMessageDialog(parent, sp, title, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Shows a simple form. A label like "Role|A;B;C" becomes a drop-down, a label starting with
     * "Password" becomes a masked field. Returns the values, or null if the user cancelled.
     */
    static String[] form(Component parent, String title, String[] labels, String[] initial) {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        JComponent[] fields = new JComponent[labels.length];
        for (int i = 0; i < labels.length; i++) {
            String init = initial == null ? "" : initial[i];
            String[] parts = labels[i].split("\\|");
            p.add(new JLabel(parts[0]));
            if (parts.length > 1) {
                JComboBox<String> cb = new JComboBox<>(parts[1].split(";"));
                cb.setSelectedItem(init);
                fields[i] = cb;
            } else if (parts[0].startsWith("Password")) {
                fields[i] = new JPasswordField(22);
            } else {
                fields[i] = new JTextField(init, 22);
            }
            p.add(fields[i]);
        }
        int ok = JOptionPane.showConfirmDialog(parent, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;
        String[] out = new String[labels.length];
        for (int i = 0; i < out.length; i++) {
            JComponent f = fields[i];
            if (f instanceof JComboBox) out[i] = String.valueOf(((JComboBox<?>) f).getSelectedItem());
            else if (f instanceof JPasswordField) out[i] = new String(((JPasswordField) f).getPassword());
            else out[i] = ((JTextField) f).getText().trim();
        }
        return out;
    }

    static Integer whole(Component parent, String s, String what, int min) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v >= min) return v;
        } catch (NumberFormatException ignored) { }
        error(parent, what + " must be a whole number, " + min + " or more.");
        return null;
    }

    static Double amount(Component parent, String s, String what) {
        try {
            double v = Double.parseDouble(s.trim());
            if (v > 0 && v < 10_000_000) return v;
        } catch (NumberFormatException ignored) { }
        error(parent, what + " must be a positive amount.");
        return null;
    }

    static boolean blank(Component parent, String s, String what) {
        if (s.isEmpty()) {
            error(parent, what + " can't be empty.");
            return true;
        }
        return false;
    }
}
