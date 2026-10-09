package shop;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

final class AdminPanel {
    private AdminPanel() { }

    static JComponent build(Store store, User me) {
        String[] titles = {"Overview", "Users", "Products", "Orders", "Activity"};
        Tab[] tabs = {
            new Overview(store),
            new UsersTab(store, me),
            new ProductsTab(store, () -> store.products, null),
            new OrdersTab(store, () -> store.orders, OrdersTab.Mode.STAFF),
            new ActivityTab(store)
        };
        return Ui.tabs(titles, tabs);
    }

    private static final class Overview extends Tab {
        private final Store store;
        private final JTextArea text = Ui.textArea();

        Overview(Store store) {
            this.store = store;
            add(new JScrollPane(text));
        }

        @Override
        void refresh() {
            long buyers = store.users.stream().filter(u -> u.role == Role.BUYER).count();
            long sellers = store.users.stream().filter(u -> u.role == Role.SELLER).count();
            long open = store.orders.stream()
                    .filter(o -> o.status != OrderStatus.DELIVERED && o.status != OrderStatus.CANCELLED).count();
            double revenue = store.orders.stream().filter(o -> o.status != OrderStatus.CANCELLED)
                    .mapToDouble(Order::total).sum();
            StringBuilder sb = new StringBuilder();
            sb.append("Users:     ").append(store.users.size()).append("  (").append(sellers).append(" sellers, ")
                    .append(buyers).append(" buyers)\n");
            sb.append("Products:  ").append(store.products.size()).append('\n');
            sb.append("Orders:    ").append(store.orders.size()).append("  (").append(open).append(" still open)\n");
            sb.append("Revenue:   ").append(Fmt.money(revenue)).append("\n\nLatest activity\n");
            int from = Math.max(0, store.activity.size() - 10);
            for (int i = store.activity.size() - 1; i >= from; i--) sb.append("  ").append(store.activity.get(i)).append('\n');
            text.setText(sb.toString());
        }
    }

    private static final class ActivityTab extends Tab {
        private final Store store;
        private final JTextArea text = Ui.textArea();

        ActivityTab(Store store) {
            this.store = store;
            add(new JScrollPane(text));
            add(Ui.bar(Ui.button("Refresh", this::refresh)), BorderLayout.NORTH);
        }

        @Override
        void refresh() {
            StringBuilder sb = new StringBuilder();
            for (int i = store.activity.size() - 1; i >= 0; i--) sb.append(store.activity.get(i)).append('\n');
            text.setText(sb.toString());
            text.setCaretPosition(0);
        }
    }

    private static final class UsersTab extends Tab {
        private static final String ROLES = "Role|ADMIN;SELLER;BUYER";

        private final Store store;
        private final User me;
        private final DefaultTableModel model = Ui.model("ID", "Name", "Email", "Role");
        private final JTable table = Ui.table(model);

        UsersTab(Store store, User me) {
            this.store = store;
            this.me = me;
            add(new JScrollPane(table));
            add(Ui.bar(Ui.button("Add user", this::create), Ui.button("Edit", this::edit),
                    Ui.button("Delete", this::delete)), BorderLayout.NORTH);
        }

        @Override
        void refresh() {
            model.setRowCount(0);
            for (User u : store.users) model.addRow(new Object[]{u.id, u.name, u.email, u.role});
        }

        private void create() {
            String[] v = Ui.form(this, "New user", new String[]{"Name", "Email", "Password", ROLES},
                    new String[]{"", "", "", "BUYER"});
            if (v == null || Ui.blank(this, v[0], "Name") || Ui.blank(this, v[2], "Password")) return;
            String problem = store.checkEmail(v[1], null);
            if (problem != null) {
                Ui.error(this, problem);
                return;
            }
            store.addUser(v[0], v[1], v[2], Role.valueOf(v[3]));
            refresh();
        }

        private void edit() {
            User u = selected();
            if (u == null) return;
            String[] v = Ui.form(this, "Edit user", new String[]{"Name", "Email", ROLES},
                    new String[]{u.name, u.email, u.role.name()});
            if (v == null || Ui.blank(this, v[0], "Name")) return;
            String problem = store.checkEmail(v[1], u);
            if (problem != null) {
                Ui.error(this, problem);
                return;
            }
            Role role = Role.valueOf(v[2]);
            if (u.role == Role.ADMIN && role != Role.ADMIN && store.countAdmins() == 1) {
                Ui.error(this, "That's the only admin, so the role stays as it is.");
                role = Role.ADMIN;
            }
            u.name = v[0];
            u.email = v[1].toLowerCase();
            u.role = role;
            store.log("User updated: " + u.name);
            store.save();
            refresh();
        }

        private void delete() {
            User u = selected();
            if (u == null) return;
            if (u == me) {
                Ui.error(this, "You can't delete the account you're logged in with.");
            } else if (u.role == Role.ADMIN && store.countAdmins() == 1) {
                Ui.error(this, "You can't delete the only admin.");
            } else if (Ui.confirm(this, "Delete " + u.name + (u.role == Role.SELLER ? " and all their products?" : "?"))) {
                store.deleteUser(u);
                refresh();
            }
        }

        private User selected() {
            int id = Ui.selectedId(this, table);
            return id < 0 ? null : store.user(id);
        }
    }
}
