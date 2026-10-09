package shop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

final class BuyerPanel {
    private static final String PAYMENT = "Payment|Cash on delivery;UPI;Card";

    private final Store store;
    private final User me;
    private final Map<Integer, Integer> cart = new LinkedHashMap<>(); // product id -> qty, lives for this login

    private BuyerPanel(Store store, User me) {
        this.store = store;
        this.me = me;
    }

    static JComponent build(Store store, User me) {
        BuyerPanel b = new BuyerPanel(store, me);
        String[] titles = {"Overview", "Browse", "Cart", "My orders", "Wishlist", "Recently viewed", "Account"};
        Tab[] tabs = {
            b.new Overview(),
            b.new Browse(),
            b.new CartTab(),
            new OrdersTab(store, () -> store.ordersOfBuyer(me.id), OrdersTab.Mode.BUYER),
            b.new Wishlist(),
            b.new Recent(),
            b.new Account()
        };
        return Ui.tabs(titles, tabs);
    }

    // ---- actions shared by several tabs ----

    private Object[] productRow(Product p) {
        return new Object[]{p.id, p.name, p.category, Fmt.money(p.price), p.stock == 0 ? "out" : p.stock,
                store.userName(p.sellerId)};
    }

    private void addToCart(Component parent, Product p) {
        int room = p.stock - cart.getOrDefault(p.id, 0);
        if (p.stock == 0) {
            Ui.error(parent, "Sorry, '" + p.name + "' is out of stock.");
            return;
        }
        if (room <= 0) {
            Ui.error(parent, "Your cart already holds all " + p.stock + " we have.");
            return;
        }
        String s = JOptionPane.showInputDialog(parent, "Quantity of " + p.name + " (up to " + room + "):", "1");
        if (s == null) return;
        Integer q = Ui.whole(parent, s, "Quantity", 1);
        if (q == null) return;
        if (q > room) {
            Ui.error(parent, "Only " + room + " more can be added.");
            return;
        }
        cart.merge(p.id, q, Integer::sum);
        Ui.info(parent, "Added " + q + " x " + p.name + " to your cart.");
    }

    private void addToWishlist(Component parent, Product p) {
        if (me.wishlist.putIfAbsent(p.id, p.price) == null) {
            store.save();
            Ui.info(parent, "Saved to your wishlist at " + Fmt.money(p.price) + ".");
        } else {
            Ui.info(parent, "It's already on your wishlist.");
        }
    }

    private void view(Component parent, Product p) {
        me.viewed(p.id);
        store.save();
        String body = p.name + "\n\n" + p.description + "\n\nCategory: " + p.category + "\nSeller:   "
                + store.userName(p.sellerId) + "\nPrice:    " + Fmt.money(p.price) + "\nStock:    "
                + (p.stock == 0 ? "out of stock" : p.stock + " available");
        String[] options = {"Add to cart", "Add to wishlist", "Close"};
        int c = JOptionPane.showOptionDialog(parent, body, p.name, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
        if (c == 0) addToCart(parent, p);
        if (c == 1) addToWishlist(parent, p);
    }

    private Product pick(Component parent, JTable t) {
        int id = Ui.selectedId(parent, t);
        return id < 0 ? null : store.product(id);
    }

    // ---- tabs ----

    private final class Overview extends Tab {
        private final JTextArea text = Ui.textArea();

        Overview() {
            add(new JScrollPane(text));
        }

        @Override
        void refresh() {
            long onTheWay = store.ordersOfBuyer(me.id).stream()
                    .filter(o -> o.status != OrderStatus.DELIVERED && o.status != OrderStatus.CANCELLED).count();
            text.setText("Welcome, " + me.name + "\n\nOrders on the way:  " + onTheWay
                    + "\nItems in cart:     " + cart.size()
                    + "\nWishlist:          " + me.wishlist.size() + "\n");
        }
    }

    private final class Browse extends Tab {
        private final JTextField keyword = new JTextField(14);
        private final JTextField category = new JTextField(10);
        private final JTextField min = new JTextField(6);
        private final JTextField max = new JTextField(6);
        private final DefaultTableModel model = Ui.model("ID", "Name", "Category", "Price", "Stock", "Seller");
        private final JTable table = Ui.table(model);

        Browse() {
            JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            filters.add(new JLabel("Search"));
            filters.add(keyword);
            filters.add(new JLabel("Category"));
            filters.add(category);
            filters.add(new JLabel("Price"));
            filters.add(min);
            filters.add(new JLabel("to"));
            filters.add(max);
            filters.add(Ui.button("Search", this::refresh));
            filters.add(Ui.button("Clear", () -> {
                keyword.setText("");
                category.setText("");
                min.setText("");
                max.setText("");
                refresh();
            }));
            JPanel actions = Ui.bar(Ui.button("View details", () -> {
                Product p = pick(this, table);
                if (p != null) view(this, p);
                refresh();
            }), Ui.button("Add to cart", () -> {
                Product p = pick(this, table);
                if (p != null) addToCart(this, p);
            }), Ui.button("Add to wishlist", () -> {
                Product p = pick(this, table);
                if (p != null) addToWishlist(this, p);
            }));
            JPanel top = new JPanel(new BorderLayout());
            top.add(filters, BorderLayout.NORTH);
            top.add(actions, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table));
        }

        @Override
        void refresh() {
            double lo = 0, hi = Double.MAX_VALUE;
            try {
                if (!min.getText().trim().isEmpty()) lo = Double.parseDouble(min.getText().trim());
                if (!max.getText().trim().isEmpty()) hi = Double.parseDouble(max.getText().trim());
            } catch (NumberFormatException e) {
                Ui.error(this, "Price limits must be numbers.");
                return;
            }
            model.setRowCount(0);
            for (Product p : store.search(keyword.getText().trim(), category.getText().trim(), lo, hi)) {
                model.addRow(productRow(p));
            }
        }
    }

    private final class CartTab extends Tab {
        private final DefaultTableModel model = Ui.model("ID", "Product", "Qty", "Price", "Subtotal");
        private final JTable table = Ui.table(model);
        private final JLabel total = new JLabel(" ");

        CartTab() {
            add(Ui.bar(Ui.button("Remove item", () -> {
                int id = Ui.selectedId(this, table);
                if (id >= 0) {
                    cart.remove(id);
                    refresh();
                }
            }), Ui.button("Check out", this::checkout)), BorderLayout.NORTH);
            add(new JScrollPane(table));
            add(total, BorderLayout.SOUTH);
        }

        @Override
        void refresh() {
            cart.keySet().removeIf(id -> store.product(id) == null); // a seller may have pulled it
            model.setRowCount(0);
            double sum = 0;
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                Product p = store.product(e.getKey());
                double sub = p.price * e.getValue();
                sum += sub;
                model.addRow(new Object[]{p.id, p.name, e.getValue(), Fmt.money(p.price), Fmt.money(sub)});
            }
            total.setText("Total: " + Fmt.money(sum));
        }

        private void checkout() {
            if (cart.isEmpty()) {
                Ui.info(this, "Your cart is empty.");
                return;
            }
            double sum = 0;
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) sum += store.product(e.getKey()).price * e.getValue();
            String[] v = Ui.form(this, "Check out - " + Fmt.money(sum), new String[]{"Delivery address", PAYMENT},
                    new String[]{me.address, me.paymentMethod});
            if (v == null || Ui.blank(this, v[0], "Delivery address")) return;
            me.address = v[0];
            me.paymentMethod = v[1];
            try {
                List<Order> placed = store.checkout(me, cart);
                cart.clear();
                StringBuilder sb = new StringBuilder("Order confirmed!\n\n");
                for (Order o : placed) {
                    sb.append("Order #").append(o.id).append(" from ").append(store.userName(o.sellerId))
                            .append(": ").append(Fmt.money(o.total())).append('\n');
                }
                Ui.info(this, sb.toString());
            } catch (IllegalStateException e) {
                Ui.error(this, "Couldn't place the order: " + e.getMessage());
            }
            refresh();
        }
    }

    private final class Wishlist extends Tab {
        private final DefaultTableModel model = Ui.model("ID", "Product", "Price now", "Price when saved", "Change");
        private final JTable table = Ui.table(model);

        Wishlist() {
            add(Ui.bar(Ui.button("Add to cart", () -> {
                Product p = pick(this, table);
                if (p != null) addToCart(this, p);
            }), Ui.button("Remove", () -> {
                int id = Ui.selectedId(this, table);
                if (id >= 0) {
                    me.wishlist.remove(id);
                    store.save();
                    refresh();
                }
            })), BorderLayout.NORTH);
            add(new JScrollPane(table));
        }

        @Override
        void refresh() {
            model.setRowCount(0);
            for (Map.Entry<Integer, Double> e : me.wishlist.entrySet()) {
                Product p = store.product(e.getKey());
                if (p == null) continue;
                double was = e.getValue();
                String change = p.price < was ? "Dropped" : p.price > was ? "Went up" : "-";
                model.addRow(new Object[]{p.id, p.name, Fmt.money(p.price), Fmt.money(was), change});
            }
        }
    }

    private final class Recent extends Tab {
        private final DefaultTableModel model = Ui.model("ID", "Name", "Category", "Price", "Stock", "Seller");
        private final JTable table = Ui.table(model);

        Recent() {
            add(Ui.bar(Ui.button("Open", () -> {
                Product p = pick(this, table);
                if (p != null) view(this, p);
                refresh();
            })), BorderLayout.NORTH);
            add(new JScrollPane(table));
        }

        @Override
        void refresh() {
            model.setRowCount(0);
            for (int id : me.recentlyViewed) {
                Product p = store.product(id);
                if (p != null) model.addRow(productRow(p));
            }
        }
    }

    private final class Account extends Tab {
        private final JTextField name = new JTextField(24);
        private final JTextField address = new JTextField(24);
        private final JComboBox<String> payment = new JComboBox<>(new String[]{"Cash on delivery", "UPI", "Card"});
        private final JLabel email = new JLabel();

        Account() {
            JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 10));
            form.add(new JLabel("Email"));
            form.add(email);
            form.add(new JLabel("Name"));
            form.add(name);
            form.add(new JLabel("Delivery address"));
            form.add(address);
            form.add(new JLabel("Preferred payment"));
            form.add(payment);
            JPanel holder = new JPanel(new BorderLayout(0, 10));
            holder.add(form, BorderLayout.NORTH);
            holder.add(Ui.bar(Ui.button("Save changes", this::save)), BorderLayout.CENTER);
            JPanel narrow = new JPanel(new BorderLayout());
            narrow.add(holder, BorderLayout.WEST);
            add(narrow, BorderLayout.NORTH);
        }

        @Override
        void refresh() {
            email.setText(me.email);
            name.setText(me.name);
            address.setText(me.address);
            payment.setSelectedItem(me.paymentMethod);
        }

        private void save() {
            if (Ui.blank(this, name.getText().trim(), "Name")) return;
            me.name = name.getText().trim();
            me.address = address.getText().trim();
            me.paymentMethod = String.valueOf(payment.getSelectedItem());
            store.save();
            Ui.info(this, "Saved.");
        }
    }
}
