package shop;

import java.util.List;

/** Tables and detail screens shared by more than one role. */
final class Views {
    private Views() { }

    static void productTable(Store store, List<Product> list, boolean showSeller) {
        if (list.isEmpty()) {
            System.out.println("  (nothing here)");
            return;
        }
        System.out.printf("%-5s %-24s %-12s %12s %6s%s%n", "ID", "Name", "Category", "Price", "Stock",
                showSeller ? "  Seller" : "");
        Fmt.rule();
        for (Product p : list) {
            System.out.printf("%-5d %-24s %-12s %12s %6s%s%n", p.id, Fmt.cut(p.name, 24), Fmt.cut(p.category, 12),
                    Fmt.money(p.price), p.stock == 0 ? "out" : String.valueOf(p.stock),
                    showSeller ? "  " + store.userName(p.sellerId) : "");
        }
    }

    static void orderTable(Store store, List<Order> list) {
        if (list.isEmpty()) {
            System.out.println("  (no orders)");
            return;
        }
        System.out.printf("%-6s %-17s %-10s %-14s %-12s %s%n", "Order", "Placed", "Status", "Total", "Buyer", "Items");
        Fmt.rule();
        for (Order o : list) {
            int items = 0;
            for (Order.Line l : o.lines) items += l.qty;
            System.out.printf("%-6d %-17s %-10s %-14s %-12s %d%n", o.id, Fmt.stamp(o.placedAt), o.status,
                    Fmt.money(o.total()), Fmt.cut(store.userName(o.buyerId), 12), items);
        }
    }

    static void orderDetail(Store store, Order o) {
        Fmt.title("Order #" + o.id);
        System.out.println("Buyer:    " + store.userName(o.buyerId));
        System.out.println("Seller:   " + store.userName(o.sellerId));
        System.out.println("Ship to:  " + (o.shipTo.isEmpty() ? "-" : o.shipTo));
        System.out.println("Payment:  " + o.payment);
        System.out.println("Status:   " + o.status);
        System.out.println();
        for (Order.Line l : o.lines) {
            System.out.printf("  %-26s %3d x %12s = %s%n", Fmt.cut(l.name, 26), l.qty, Fmt.money(l.unitPrice),
                    Fmt.money(l.subtotal()));
        }
        System.out.println("  Total: " + Fmt.money(o.total()));
        System.out.println();
        System.out.println("Timeline:");
        for (String t : o.timeline) System.out.println("  " + t);
    }

    /** Lets the caller push an order to a new status; used by both admin and seller. */
    static void updateStatus(Store store, Order o) {
        System.out.println("Current status: " + o.status);
        OrderStatus[] all = OrderStatus.values();
        boolean any = false;
        for (int i = 0; i < all.length; i++) {
            if (o.status.canMoveTo(all[i])) {
                System.out.println("  " + (i + 1) + ") " + all[i]);
                any = true;
            }
        }
        if (!any) {
            System.out.println("This order is closed; no further changes.");
            return;
        }
        int pick = In.integer("Move to (0 to leave it)", 0, all.length);
        if (pick == 0) return;
        String err = store.changeStatus(o, all[pick - 1]);
        System.out.println(err == null ? "Order #" + o.id + " is now " + o.status + "." : err);
    }

    static Order pickOrder(Store store, List<Order> allowed) {
        int id = In.integer("Order number (0 to go back)", 0, Integer.MAX_VALUE);
        if (id == 0) return null;
        for (Order o : allowed) {
            if (o.id == id) return o;
        }
        System.out.println("No such order in this list.");
        return null;
    }
}
