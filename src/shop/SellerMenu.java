package shop;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class SellerMenu {
    private final Store store;
    private final User me;

    SellerMenu(Store store, User me) {
        this.store = store;
        this.me = me;
    }

    void run() {
        while (true) {
            dashboard();
            System.out.println("1) My products   2) Add product   3) Edit product   4) Remove product");
            System.out.println("5) Inventory     6) Orders        7) Sales performance   0) Log out");
            switch (In.integer("Choose", 0, 7)) {
                case 1: Fmt.title("My products"); Views.productTable(store, store.productsOf(me.id), false); In.pause(); break;
                case 2: createProduct(store, me); break;
                case 3: withOwnProduct("Edit", p -> editProduct(store, p)); break;
                case 4: withOwnProduct("Remove", this::remove); break;
                case 5: inventory(); break;
                case 6: orders(); break;
                case 7: sales(); break;
                default: return;
            }
        }
    }

    private void dashboard() {
        List<Product> mine = store.productsOf(me.id);
        long low = mine.stream().filter(Product::lowOnStock).count();
        long toProcess = store.ordersOfSeller(me.id).stream()
                .filter(o -> o.status == OrderStatus.PLACED || o.status == OrderStatus.CONFIRMED).count();
        Fmt.title("Seller dashboard - " + me.name);
        System.out.println("Listings: " + mine.size() + "   Low on stock: " + low + "   Orders waiting on you: " + toProcess);
        System.out.println();
    }

    private void withOwnProduct(String verb, java.util.function.Consumer<Product> action) {
        Product p = store.product(In.integer(verb + " which product ID (0 to cancel)", 0, Integer.MAX_VALUE));
        if (p == null) return;
        if (p.sellerId != me.id) {
            System.out.println("That product isn't yours.");
            return;
        }
        action.accept(p);
    }

    private void remove(Product p) {
        if (In.yes("Remove '" + p.name + "' from the store")) {
            store.removeProduct(p);
            System.out.println("Product removed.");
        }
    }

    static void createProduct(Store store, User seller) {
        String name = In.required("Product name");
        String desc = In.required("Description");
        String cat = In.required("Category");
        double price = In.price("Price");
        int stock = In.integer("Units in stock", 0, 1_000_000);
        Product p = store.addProduct(seller.id, name, desc, cat, price, stock);
        System.out.println("Listed '" + p.name + "' with ID " + p.id + ".");
    }

    static void editProduct(Store store, Product p) {
        String v = In.line("Name [" + p.name + "]");
        if (!v.isEmpty()) p.name = v;
        v = In.line("Description [" + Fmt.cut(p.description, 40) + "]");
        if (!v.isEmpty()) p.description = v;
        v = In.line("Category [" + p.category + "]");
        if (!v.isEmpty()) p.category = v;
        if (In.yes("Change price (now " + Fmt.money(p.price) + ")")) p.price = In.price("New price");
        if (In.yes("Change stock (now " + p.stock + ")")) p.stock = In.integer("New stock", 0, 1_000_000);
        store.log("Product updated: " + p.name);
        store.save();
        System.out.println("Product updated.");
    }

    // ---- inventory ----

    private void inventory() {
        Fmt.title("Inventory");
        List<Product> mine = store.productsOf(me.id);
        if (mine.isEmpty()) {
            System.out.println("  Nothing listed yet.");
            In.pause();
            return;
        }
        int max = mine.stream().mapToInt(p -> p.stock).max().orElse(0);
        for (Product p : mine) {
            System.out.printf("%-5d %-22s [%s] %4d %s%n", p.id, Fmt.cut(p.name, 22), Fmt.bar(p.stock, max, 25),
                    p.stock, p.lowOnStock() ? " <- LOW" : "");
        }
        System.out.println();
        if (In.yes("Update a stock level")) {
            withOwnProduct("Restock", p -> {
                p.stock = In.integer("New stock level", 0, 1_000_000);
                store.log(me.name + " set stock of '" + p.name + "' to " + p.stock);
                store.save();
                System.out.println("Stock updated.");
            });
        }
    }

    // ---- orders ----

    private void orders() {
        while (true) {
            Fmt.title("Orders for my products");
            List<Order> mine = store.ordersOfSeller(me.id);
            Views.orderTable(store, mine);
            if (mine.isEmpty()) {
                In.pause();
                return;
            }
            Order o = Views.pickOrder(store, mine);
            if (o == null) return;
            Views.orderDetail(store, o);
            Views.updateStatus(store, o);
        }
    }

    // ---- sales ----

    private void sales() {
        Fmt.title("Sales performance");
        Map<String, Integer> units = new TreeMap<>();
        Map<String, Double> revenue = new TreeMap<>();
        Map<LocalDate, Double> perDay = new TreeMap<>();
        double total = 0;
        for (Order o : store.ordersOfSeller(me.id)) {
            if (o.status == OrderStatus.CANCELLED) continue;
            for (Order.Line l : o.lines) {
                units.merge(l.name, l.qty, Integer::sum);
                revenue.merge(l.name, l.subtotal(), Double::sum);
            }
            perDay.merge(o.placedAt.toLocalDate(), o.total(), Double::sum);
            total += o.total();
        }
        if (units.isEmpty()) {
            System.out.println("  No sales yet.");
            In.pause();
            return;
        }
        System.out.println("Total revenue (cancelled orders excluded): " + Fmt.money(total));
        System.out.println();
        System.out.println("By product:");
        int maxUnits = units.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (String name : units.keySet()) {
            System.out.printf("  %-22s [%s] %3d sold  %s%n", Fmt.cut(name, 22), Fmt.bar(units.get(name), maxUnits, 20),
                    units.get(name), Fmt.money(revenue.get(name)));
        }
        System.out.println();
        System.out.println("By day:");
        double maxDay = perDay.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        for (Map.Entry<LocalDate, Double> e : perDay.entrySet()) {
            System.out.printf("  %s [%s] %s%n", e.getKey(), Fmt.bar((int) Math.round(e.getValue()), (int) Math.round(maxDay), 20),
                    Fmt.money(e.getValue()));
        }
        In.pause();
    }
}
