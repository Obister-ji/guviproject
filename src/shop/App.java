package shop;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/** The window: a login screen, then the dashboard for whichever role logged in. */
final class App {
    private final Store store;
    private final JFrame frame = new JFrame("Online Shop");

    App(Store store) {
        this.store = store;
    }

    void show() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                store.save();
            }
        });
        frame.setSize(1050, 680);
        frame.setLocationRelativeTo(null);
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        JTextField email = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        JButton login = new JButton("Log in");
        JButton signUp = new JButton("Sign up");

        JLabel title = new JLabel("Online Shop");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel hint = new JLabel("First time? Admin login is admin@shop.local / admin123");
        hint.setForeground(java.awt.Color.GRAY);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy = 0;
        p.add(title, c);
        c.gridwidth = 1;
        c.gridy = 1;
        p.add(new JLabel("Email"), c);
        c.gridx = 1;
        p.add(email, c);
        c.gridx = 0;
        c.gridy = 2;
        p.add(new JLabel("Password"), c);
        c.gridx = 1;
        p.add(password, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        JPanel buttons = new JPanel();
        buttons.add(login);
        buttons.add(signUp);
        p.add(buttons, c);
        c.gridy = 4;
        p.add(hint, c);

        login.addActionListener(e -> {
            User u = store.login(email.getText().trim(), new String(password.getPassword()));
            if (u == null) {
                Ui.error(frame, "Wrong email or password.");
                return;
            }
            store.log(u.name + " logged in");
            showDashboard(u);
        });
        signUp.addActionListener(e -> signUp());

        frame.setContentPane(p);
        frame.getRootPane().setDefaultButton(login);
        frame.revalidate();
        frame.repaint();
    }

    private void signUp() {
        String[] v = Ui.form(frame, "Create an account",
                new String[]{"Name", "Email", "Password", "I want to|Buy;Sell"}, new String[]{"", "", "", "Buy"});
        if (v == null || Ui.blank(frame, v[0], "Name") || Ui.blank(frame, v[2], "Password")) return;
        String problem = store.checkEmail(v[1], null);
        if (problem != null) {
            Ui.error(frame, problem);
            return;
        }
        store.addUser(v[0], v[1], v[2], v[3].equals("Sell") ? Role.SELLER : Role.BUYER);
        Ui.info(frame, "Account created. You can log in now.");
    }

    private void showDashboard(User u) {
        JLabel who = new JLabel("  " + u.name + "  (" + u.role.name().toLowerCase() + ")");
        who.setFont(who.getFont().deriveFont(Font.BOLD, 15f));
        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> {
            store.log(u.name + " logged out");
            store.save();
            showLogin();
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(6, 6, 6, 10));
        top.add(who, BorderLayout.WEST);
        top.add(logout, BorderLayout.EAST);

        javax.swing.JComponent body;
        switch (u.role) {
            case ADMIN:  body = AdminPanel.build(store, u); break;
            case SELLER: body = SellerPanel.build(store, u); break;
            default:     body = BuyerPanel.build(store, u); break;
        }
        JPanel root = new JPanel(new BorderLayout());
        root.add(top, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.getRootPane().setDefaultButton(null);
        frame.revalidate();
        frame.repaint();
    }
}
