package shop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BuyerMenu {
    private static final String[] PAYMENT = {"Cash on delivery", "UPI", "Card"};

    private final Store store;
    private final User me;
    private final Map<Integer, Integer> cart = new LinkedHashMap<>();

    BuyerMenu(Store store, User me) {
        this.store = store;
        this.me = me;
    }

    void run() {
        while (true) {
            dashboard();
            System.out.println("1) Browse / search   2) Cart (" + cart.size() + ")   3) My orders   4) Wishlist");
            System.out.println("5) Recently viewed   6) Account   0) Log out");
            switch (In.integer("Choose", 0, 6)) {
                case 1: browse(); break;
                case 2: cart(); break;
                case 3: orders(); break;
                case 4: wishlist(); break;
                case 5: recent(); break;
                case 6: account(); break;
                default: return;
            }
        }
    }

    private void dashboard() {
        long active = store.ordersOfBuyer(me.id).stream()
                .filter(o -> o.status != OrderStatus.DELIVERED && o.status != OrderStatus.CANCELLED).count();
        Fmt.title("Welcome, " + me.name);
        System.out.println("Orders on the way: " + active + "   Cart items: " + cart.size()
                + "   Wishlist: " + me.wishlist.size());
        System.out.println();
    }

    // ---- browsing ----

    private void browse() {
        Fmt.title("Browse products");
        String kw = In.line("Search words (blank for all)");
        String cat = In.line("Category (blank for all)");
        double min = 0, max = Double.MAX_VALUE;
        if (In.yes("Filter by price")) {
            min = In.integer("Minimum price", 0, 10_000_000);
            max = In.integer("Maximum price", (int) min, 10_000_000);
        }
        while (true) {
            List<Product> found = store.search(kw, cat, min, max);
            Views.productTable(store, found, true);
            if (found.isEmpty()) {
                In.pause();
                return;
            }
            System.out.println("1) View a product   0) Back");
            if (In.integer("Choose", 0, 1) == 0) return;
            Product p = store.product(In.integer("Product ID", 0, Integer.MAX_VALUE));
            if (p == null) {
                System.out.println("No product with that ID.");
                continue;
            }
            viewProduct(p);
        }
    }

    private void viewProduct(Product p) {
        me.viewed(p.id);
        store.save();
        Fmt.title(p.name);
        System.out.println(p.description);
        System.out.println("Category: " + p.category + "   Seller: " + store.userName(p.sellerId));
        System.out.println("Price: " + Fmt.money(p.price) + "   " + (p.stock == 0 ? "Out of stock" : p.stock + " in stock"));
        System.out.println("1) Add to cart   2) Add to wishlist   0) Back");
        int c = In.integer("Choose", 0, 2);
        if (c == 1) addToCart(p);
        if (c == 2) {
            me.wishlist.putIfAbsent(p.id, p.price);
            store.save();
            System.out.println("Saved to wishlist.");
        }
    }

    private void addToCart(Product p) {
        if (p.stock == 0) {
            System.out.println("Sorry, that one is out of stock.");
            return;
        }
        int already = cart.getOrDefault(p.id, 0);
        int room = p.stock - already;
        if (room <= 0) {
            System.out.println("Your cart already holds all " + p.stock + " we have.");
            return;
        }
        int qty = In.integer("Quantity (max " + room + ")", 1, room);
        cart.merge(p.id, qty, Integer::sum);
        System.out.println("Added to cart.");
    }

    // ---- cart ----

    private void cart() {
        while (true) {
            Fmt.title("Your cart");
            cart.keySet().removeIf(id -> store.product(id) == null); // seller may have pulled it
            if (cart.isEmpty()) {
                System.out.println("  Empty.");
                In.pause();
                return;
            }
            double total = 0;
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                Product p = store.product(e.getKey());
                double sub = p.price * e.getValue();
                total += sub;
                System.out.printf("%-5d %-24s %3d x %12s = %s%n", p.id, Fmt.cut(p.name, 24), e.getValue(),
                        Fmt.money(p.price), Fmt.money(sub));
            }
            System.out.println("Total: " + Fmt.money(total));
            System.out.println("1) Check out   2) Remove an item   0) Back");
            int c = In.integer("Choose", 0, 2);
            if (c == 0) return;
            if (c == 2) {
                cart.remove(In.integer("Product ID to remove", 0, Integer.MAX_VALUE));
            } else if (checkout(total)) {
                return;
            }
        }
    }

    private boolean checkout(double total) {
        if (me.address.isEmpty()) {
            me.address = In.required("Delivery address");
        } else {
            System.out.println("Delivering to: " + me.address);
        }
        pickPayment();
        if (!In.yes("Place order for " + Fmt.money(total) + " via " + me.paymentMethod)) return false;
        try {
            List<Order> placed = store.checkout(me, cart);
            cart.clear();
            System.out.println();
            System.out.println("Order confirmed!");
            for (Order o : placed) {
                System.out.println("  Order #" + o.id + " from " + store.userName(o.sellerId) + ": " + Fmt.money(o.total()));
            }
            In.pause();
            return true;
        } catch (IllegalStateException e) {
            System.out.println("Couldn't place the order: " + e.getMessage());
            return false;
        }
    }

    private void pickPayment() {
        for (int i = 0; i < PAYMENT.length; i++) System.out.println("  " + (i + 1) + ") " + PAYMENT[i]);
        me.paymentMethod = PAYMENT[In.integer("Payment method", 1, PAYMENT.length) - 1];
    }

    // ---- orders ----

    private void orders() {
        while (true) {
            Fmt.title("My orders");
            List<Order> mine = store.ordersOfBuyer(me.id);
            Views.orderTable(store, mine);
            if (mine.isEmpty()) {
                In.pause();
                return;
            }
            Order o = Views.pickOrder(store, mine);
            if (o == null) return;
            Views.orderDetail(store, o);
            if (o.status.canMoveTo(OrderStatus.CANCELLED) && In.yes("Cancel this order")) {
                store.changeStatus(o, OrderStatus.CANCELLED);
                System.out.println("Order cancelled.");
            }
        }
    }

    // ---- wishlist / history / account ----

    private void wishlist() {
        Fmt.title("Wishlist");
        if (me.wishlist.isEmpty()) {
            System.out.println("  Nothing saved yet.");
            In.pause();
            return;
        }
        for (Map.Entry<Integer, Double> e : me.wishlist.entrySet()) {
            Product p = store.product(e.getKey());
            double was = e.getValue();
            String change = p.price < was ? "  price DROPPED from " + Fmt.money(was)
                    : p.price > was ? "  price went UP from " + Fmt.money(was) : "";
            System.out.printf("%-5d %-24s %12s%s%n", p.id, Fmt.cut(p.name, 24), Fmt.money(p.price), change);
        }
        System.out.println("1) Add one to cart   2) Remove one   0) Back");
        int c = In.integer("Choose", 0, 2);
        if (c == 0) return;
        int id = In.integer("Product ID", 0, Integer.MAX_VALUE);
        if (!me.wishlist.containsKey(id)) {
            System.out.println("That isn't on your wishlist.");
        } else if (c == 1) {
            addToCart(store.product(id));
        } else {
            me.wishlist.remove(id);
            store.save();
        }
    }

    private void recent() {
        Fmt.title("Recently viewed");
        if (me.recentlyViewed.isEmpty()) {
            System.out.println("  You haven't opened any products yet.");
            In.pause();
            return;
        }
        List<Product> list = new java.util.ArrayList<>();
        for (int id : me.recentlyViewed) list.add(store.product(id));
        Views.productTable(store, list, true);
        System.out.println("1) Open one   0) Back");
        if (In.integer("Choose", 0, 1) == 1) {
            Product p = store.product(In.integer("Product ID", 0, Integer.MAX_VALUE));
            if (p != null) viewProduct(p);
        }
    }

    private void account() {
        Fmt.title("Account");
        System.out.println("Name:    " + me.name);
        System.out.println("Email:   " + me.email);
        System.out.println("Address: " + (me.address.isEmpty() ? "-" : me.address));
        System.out.println("Payment: " + me.paymentMethod);
        System.out.println("1) Change name   2) Change address   3) Change payment method   0) Back");
        switch (In.integer("Choose", 0, 3)) {
            case 1: me.name = In.required("New name"); break;
            case 2: me.address = In.required("New address"); break;
            case 3: pickPayment(); break;
            default: return;
        }
        store.save();
        System.out.println("Saved.");
    }
}
