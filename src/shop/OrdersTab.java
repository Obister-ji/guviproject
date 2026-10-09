package shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Order table. Staff (admin/seller) move orders along; a buyer can only look and cancel. */
final class OrdersTab extends Tab {
    enum Mode { STAFF, BUYER }

    private final Store store;
    private final Supplier<List<Order>> source;
    private final DefaultTableModel model = Ui.model("Order", "Placed", "Status", "Total", "Buyer", "Seller");
    private final JTable table = Ui.table(model);

    OrdersTab(Store store, Supplier<List<Order>> source, Mode mode) {
        this.store = store;
        this.source = source;
        add(new JScrollPane(table));
        add(mode == Mode.STAFF
                ? Ui.bar(Ui.button("Details", this::details), Ui.button("Update status", this::updateStatus))
                : Ui.bar(Ui.button("Details / tracking", this::details), Ui.button("Cancel order", this::cancel)),
                java.awt.BorderLayout.NORTH);
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        List<Order> list = new ArrayList<>(source.get());
        Collections.reverse(list); // newest first
        for (Order o : list) {
            model.addRow(new Object[]{o.id, Fmt.stamp(o.placedAt), o.status, Fmt.money(o.total()),
                    store.userName(o.buyerId), store.userName(o.sellerId)});
        }
    }

    private Order selected() {
        int id = Ui.selectedId(this, table);
        return id < 0 ? null : store.order(id);
    }

    private void details() {
        Order o = selected();
        if (o != null) Ui.text(this, "Order #" + o.id, describe(o));
    }

    private String describe(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Buyer:    ").append(store.userName(o.buyerId)).append('\n');
        sb.append("Seller:   ").append(store.userName(o.sellerId)).append('\n');
        sb.append("Ship to:  ").append(o.shipTo.isEmpty() ? "-" : o.shipTo).append('\n');
        sb.append("Payment:  ").append(o.payment).append('\n');
        sb.append("Status:   ").append(o.status).append("\n\n");
        for (Order.Line l : o.lines) {
            sb.append(String.format("%-24s %3d x %-12s = %s%n", Fmt.cut(l.name, 24), l.qty, Fmt.money(l.unitPrice),
                    Fmt.money(l.subtotal())));
        }
        sb.append("\nTotal: ").append(Fmt.money(o.total())).append("\n\nTimeline\n");
        for (String t : o.timeline) sb.append("  ").append(t).append('\n');
        return sb.toString();
    }

    private void updateStatus() {
        Order o = selected();
        if (o == null) return;
        List<OrderStatus> next = new ArrayList<>();
        for (OrderStatus s : OrderStatus.values()) {
            if (o.status.canMoveTo(s)) next.add(s);
        }
        if (next.isEmpty()) {
            Ui.info(this, "Order #" + o.id + " is " + o.status + " and can't change any more.");
            return;
        }
        Object pick = JOptionPane.showInputDialog(this, "Order #" + o.id + " is " + o.status + ". Move it to:",
                "Update status", JOptionPane.QUESTION_MESSAGE, null, next.toArray(), next.get(0));
        if (pick == null) return;
        String err = store.changeStatus(o, (OrderStatus) pick);
        if (err != null) Ui.error(this, err);
        refresh();
    }

    private void cancel() {
        Order o = selected();
        if (o == null) return;
        if (!o.status.canMoveTo(OrderStatus.CANCELLED)) {
            Ui.info(this, "This order is " + o.status + " and can't be cancelled.");
            return;
        }
        if (Ui.confirm(this, "Cancel order #" + o.id + "?")) {
            store.changeStatus(o, OrderStatus.CANCELLED);
            refresh();
        }
    }
}
