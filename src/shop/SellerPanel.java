package shop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

final class SellerPanel {
    private SellerPanel() { }

    static JComponent build(Store store, User me) {
        String[] titles = {"Overview", "My products", "Inventory", "Orders", "Sales performance"};
        Tab[] tabs = {
            new Overview(store, me),
            new ProductsTab(store, () -> store.productsOf(me.id), me),
            new InventoryTab(store, me),
            new OrdersTab(store, () -> store.ordersOfSeller(me.id), OrdersTab.Mode.STAFF),
            new SalesTab(store, me)
        };
        return Ui.tabs(titles, tabs);
    }

    private static final class Overview extends Tab {
        private final Store store;
        private final User me;
        private final JTextArea text = Ui.textArea();

        Overview(Store store, User me) {
            this.store = store;
            this.me = me;
            add(new JScrollPane(text));
        }

        @Override
        void refresh() {
            long low = store.productsOf(me.id).stream().filter(Product::lowOnStock).count();
            long waiting = store.ordersOfSeller(me.id).stream()
                    .filter(o -> o.status == OrderStatus.PLACED || o.status == OrderStatus.CONFIRMED).count();
            double revenue = store.ordersOfSeller(me.id).stream().filter(o -> o.status != OrderStatus.CANCELLED)
                    .mapToDouble(Order::total).sum();
            text.setText("Listings:                 " + store.productsOf(me.id).size()
                    + "\nLow on stock (<= " + Product.LOW_STOCK + "):     " + low
                    + "\nOrders waiting on you:    " + waiting
                    + "\nRevenue so far:           " + Fmt.money(revenue) + "\n");
        }
    }

    private static final class InventoryTab extends Tab {
        private final Store store;
        private final User me;
        private final DefaultTableModel model = Ui.model("ID", "Product", "In stock", "Level");
        private final JTable table = Ui.table(model);

        InventoryTab(Store store, User me) {
            this.store = store;
            this.me = me;
            table.getColumnModel().getColumn(3).setCellRenderer(new LevelRenderer());
            add(new JScrollPane(table));
            add(Ui.bar(Ui.button("Set stock level", this::setStock)), BorderLayout.NORTH);
        }

        @Override
        void refresh() {
            model.setRowCount(0);
            int max = store.productsOf(me.id).stream().mapToInt(p -> p.stock).max().orElse(0);
            for (Product p : store.productsOf(me.id)) {
                int pct = max == 0 ? 0 : (int) Math.round(100.0 * p.stock / max);
                model.addRow(new Object[]{p.id, p.name, p.stock, pct});
            }
        }

        private void setStock() {
            int id = Ui.selectedId(this, table);
            Product p = id < 0 ? null : store.product(id);
            if (p == null) return;
            String s = JOptionPane.showInputDialog(this, "New stock level for " + p.name + ":", p.stock);
            if (s == null) return;
            Integer n = Ui.whole(this, s, "Stock", 0);
            if (n == null) return;
            p.stock = n;
            store.log(me.name + " set stock of '" + p.name + "' to " + n);
            store.save();
            refresh();
        }
    }

    /** Draws the Level column as a bar, red when the product is low on stock. */
    private static final class LevelRenderer extends DefaultTableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 100);

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            int stock = (Integer) t.getValueAt(row, 2);
            boolean low = stock <= Product.LOW_STOCK;
            bar.setValue((Integer) value);
            bar.setStringPainted(true);
            bar.setString(low ? "LOW (" + stock + ")" : String.valueOf(stock));
            bar.setForeground(low ? new Color(200, 60, 50) : new Color(60, 150, 90));
            return bar;
        }
    }

    private static final class SalesTab extends Tab {
        private final Store store;
        private final User me;
        private final JLabel total = new JLabel(" ");
        private final BarChart byProduct = new BarChart();
        private final BarChart byDay = new BarChart();

        SalesTab(Store store, User me) {
            this.store = store;
            this.me = me;
            total.setFont(total.getFont().deriveFont(java.awt.Font.BOLD, 15f));
            JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
            charts.add(wrap("Revenue by product", byProduct));
            charts.add(wrap("Revenue by day", byDay));
            add(total, BorderLayout.NORTH);
            add(charts);
        }

        private static JScrollPane wrap(String title, BarChart c) {
            JScrollPane sp = new JScrollPane(c);
            sp.setBorder(BorderFactory.createTitledBorder(title));
            return sp;
        }

        @Override
        void refresh() {
            Map<String, Double> products = new TreeMap<>();
            Map<String, Double> days = new TreeMap<>();
            double sum = 0;
            for (Order o : store.ordersOfSeller(me.id)) {
                if (o.status == OrderStatus.CANCELLED) continue;
                for (Order.Line l : o.lines) products.merge(l.name, l.subtotal(), Double::sum);
                days.merge(o.placedAt.toLocalDate().toString(), o.total(), Double::sum);
                sum += o.total();
            }
            total.setText("Total revenue (cancelled orders excluded): " + Fmt.money(sum));
            byProduct.set(products);
            byDay.set(days);
        }
    }

    private static final class BarChart extends JPanel {
        private Map<String, Double> data = new TreeMap<>();

        void set(Map<String, Double> d) {
            data = d;
            setPreferredSize(new Dimension(300, Math.max(60, d.size() * 30 + 20)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (data.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No sales yet", 12, 24);
                return;
            }
            double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int labelW = 110, valueW = 100;
            int barMax = Math.max(10, getWidth() - labelW - valueW - 24);
            int y = 10;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                int w = (int) Math.max(2, barMax * e.getValue() / max);
                g.setColor(getForeground());
                g.drawString(Fmt.cut(e.getKey(), 16), 10, y + 15);
                g.setColor(new Color(52, 120, 198));
                g.fillRect(10 + labelW, y + 2, w, 16);
                g.setColor(getForeground());
                g.drawString(Fmt.money(e.getValue()), 10 + labelW + w + 6, y + 15);
                y += 30;
            }
        }
    }
}
