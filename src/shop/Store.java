package shop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All shared state plus the rules that touch more than one entity. Saved to data/store.ser. */
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Path FILE = Paths.get("data", "store.ser");

    final List<User> users = new ArrayList<>();
    final List<Product> products = new ArrayList<>();
    final List<Order> orders = new ArrayList<>();
    final List<String> activity = new ArrayList<>();
    private int nextUser = 1, nextProduct = 1, nextOrder = 1000;

    static Store open() {
        if (Files.exists(FILE)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
                return (Store) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Couldn't read " + FILE + " (" + e.getMessage() + "), starting fresh.");
            }
        }
        Store s = new Store();
        s.users.add(new User(s.nextUser++, "Administrator", "admin@shop.local", "admin123", Role.ADMIN));
        s.log("Store created with default admin account");
        s.save();
        return s;
    }

    void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(FILE))) {
                out.writeObject(this);
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    void log(String msg) {
        activity.add(Fmt.stamp(LocalDateTime.now()) + "  " + msg);
    }

    // ---- users ----

    User login(String email, String password) {
        User u = userByEmail(email);
        return (u != null && u.passwordMatches(password)) ? u : null;
    }

    User userByEmail(String email) {
        for (User u : users) {
            if (u.email.equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    User user(int id) {
        for (User u : users) {
            if (u.id == id) return u;
        }
        return null;
    }

    String userName(int id) {
        User u = user(id);
        return u == null ? "(removed)" : u.name;
    }

    /** Returns null if the email is fine, otherwise the reason it isn't. */
    String checkEmail(String email, User except) {
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) return "That doesn't look like an email address.";
        User other = userByEmail(email);
        if (other != null && other != except) return "That email is already registered.";
        return null;
    }

    User addUser(String name, String email, String password, Role role) {
        User u = new User(nextUser++, name, email, password, role);
        users.add(u);
        log(role + " account created: " + name);
        save();
        return u;
    }

    long countAdmins() {
        return users.stream().filter(u -> u.role == Role.ADMIN).count();
    }

    void deleteUser(User u) {
        products.removeIf(p -> p.sellerId == u.id);
        users.remove(u);
        log("User deleted: " + u.name);
        save();
    }

    // ---- products ----

    Product product(int id) {
        for (Product p : products) {
            if (p.id == id) return p;
        }
        return null;
    }

    Product addProduct(int sellerId, String name, String desc, String category, double price, int stock) {
        Product p = new Product(nextProduct++, sellerId, name, desc, category, price, stock);
        products.add(p);
        log(userName(sellerId) + " listed '" + name + "'");
        save();
        return p;
    }

    void removeProduct(Product p) {
        products.remove(p);
        for (User u : users) {
            u.wishlist.remove(p.id);
            u.recentlyViewed.remove((Integer) p.id);
        }
        log("Product removed: " + p.name);
        save();
    }

    List<Product> productsOf(int sellerId) {
        List<Product> out = new ArrayList<>();
        for (Product p : products) {
            if (p.sellerId == sellerId) out.add(p);
        }
        return out;
    }

    List<Product> search(String keyword, String category, double min, double max) {
        String kw = keyword.toLowerCase();
        List<Product> out = new ArrayList<>();
        for (Product p : products) {
            boolean matchesText = kw.isEmpty()
                    || p.name.toLowerCase().contains(kw)
                    || p.description.toLowerCase().contains(kw);
            boolean matchesCat = category.isEmpty() || p.category.equalsIgnoreCase(category);
            if (matchesText && matchesCat && p.price >= min && p.price <= max) out.add(p);
        }
        out.sort(Comparator.comparing(p -> p.name.toLowerCase()));
        return out;
    }

    // ---- orders ----

    Order order(int id) {
        for (Order o : orders) {
            if (o.id == id) return o;
        }
        return null;
    }

    List<Order> ordersOfBuyer(int buyerId) {
        List<Order> out = new ArrayList<>();
        for (Order o : orders) {
            if (o.buyerId == buyerId) out.add(o);
        }
        return out;
    }

    List<Order> ordersOfSeller(int sellerId) {
        List<Order> out = new ArrayList<>();
        for (Order o : orders) {
            if (o.sellerId == sellerId) out.add(o);
        }
        return out;
    }

    /**
     * Turns a cart (product id -> qty) into one order per seller.
     * Nothing is changed if any line is short on stock.
     */
    List<Order> checkout(User buyer, Map<Integer, Integer> cart) {
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Product p = product(e.getKey());
            if (p == null) throw new IllegalStateException("A product in your cart is no longer sold.");
            if (p.stock < e.getValue()) {
                throw new IllegalStateException("Only " + p.stock + " left of '" + p.name + "'.");
            }
        }
        Map<Integer, Order> perSeller = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Product p = product(e.getKey());
            Order o = perSeller.computeIfAbsent(p.sellerId,
                    sid -> new Order(nextOrder++, buyer.id, sid, buyer.address, buyer.paymentMethod));
            o.lines.add(new Order.Line(p.id, p.name, p.price, e.getValue()));
            p.stock -= e.getValue();
        }
        orders.addAll(perSeller.values());
        for (Order o : perSeller.values()) {
            log(buyer.name + " placed order #" + o.id + " (" + Fmt.money(o.total()) + ")");
        }
        save();
        return new ArrayList<>(perSeller.values());
    }

    /** Returns null on success, otherwise the reason it failed. */
    String changeStatus(Order o, OrderStatus next) {
        if (!o.status.canMoveTo(next)) {
            return "Can't go from " + o.status + " to " + next + ".";
        }
        if (next == OrderStatus.CANCELLED) {
            for (Order.Line l : o.lines) {
                Product p = product(l.productId);
                if (p != null) p.stock += l.qty;
            }
        }
        o.status = next;
        o.timeline.add(Fmt.stamp(LocalDateTime.now()) + "  " + next);
        log("Order #" + o.id + " is now " + next);
        save();
        return null;
    }
}
