package shop;

import java.util.ArrayList;
import java.util.List;

final class AdminMenu {
    private final Store store;
    private final User me;

    AdminMenu(Store store, User me) {
        this.store = store;
        this.me = me;
    }

    void run() {
        while (true) {
            dashboard();
            System.out.println("1) Users   2) Products   3) Orders   4) Full activity log   0) Log out");
            switch (In.integer("Choose", 0, 4)) {
                case 1: users(); break;
                case 2: products(); break;
                case 3: orders(); break;
                case 4: store.activity.forEach(a -> System.out.println("  " + a)); In.pause(); break;
                default: return;
            }
        }
    }

    private void dashboard() {
        long buyers = store.users.stream().filter(u -> u.role == Role.BUYER).count();
        long sellers = store.users.stream().filter(u -> u.role == Role.SELLER).count();
        long open = store.orders.stream()
                .filter(o -> o.status != OrderStatus.DELIVERED && o.status != OrderStatus.CANCELLED).count();
        double revenue = store.orders.stream().filter(o -> o.status != OrderStatus.CANCELLED)
                .mapToDouble(Order::total).sum();
        Fmt.title("Admin dashboard - " + me.name);
        System.out.println("Users: " + store.users.size() + " (" + sellers + " sellers, " + buyers + " buyers)"
                + "   Products: " + store.products.size());
        System.out.println("Orders: " + store.orders.size() + " (" + open + " open)   Revenue: " + Fmt.money(revenue));
        System.out.println();
        System.out.println("Recent activity:");
        int from = Math.max(0, store.activity.size() - 5);
        for (int i = store.activity.size() - 1; i >= from; i--) {
            System.out.println("  " + store.activity.get(i));
        }
        System.out.println();
    }

    // ---- users ----

    private void users() {
        while (true) {
            Fmt.title("Users");
            System.out.printf("%-4s %-20s %-28s %s%n", "ID", "Name", "Email", "Role");
            Fmt.rule();
            for (User u : store.users) {
                System.out.printf("%-4d %-20s %-28s %s%n", u.id, Fmt.cut(u.name, 20), Fmt.cut(u.email, 28), u.role);
            }
            System.out.println("1) Add   2) Edit   3) Delete   0) Back");
            int c = In.integer("Choose", 0, 3);
            if (c == 0) return;
            if (c == 1) addUser();
            else {
                User u = store.user(In.integer("User ID", 0, Integer.MAX_VALUE));
                if (u == null) System.out.println("No user with that ID.");
                else if (c == 2) editUser(u);
                else deleteUser(u);
            }
        }
    }

    private Role askRole() {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) System.out.println("  " + (i + 1) + ") " + roles[i]);
        return roles[In.integer("Role", 1, roles.length) - 1];
    }

    private String askEmail(User except) {
        while (true) {
            String email = In.required("Email");
            String problem = store.checkEmail(email, except);
            if (problem == null) return email;
            System.out.println("  " + problem);
        }
    }

    private void addUser() {
        String name = In.required("Name");
        String email = askEmail(null);
        String pw = In.required("Temporary password");
        Role role = askRole();
        store.addUser(name, email, pw, role);
        System.out.println("User created.");
    }

    private void editUser(User u) {
        String name = In.line("Name [" + u.name + "]");
        if (!name.isEmpty()) u.name = name;
        if (In.yes("Change email (now " + u.email + ")")) u.email = askEmail(u).toLowerCase();
        if (In.yes("Change role (now " + u.role + ")")) {
            Role r = askRole();
            if (u.role == Role.ADMIN && r != Role.ADMIN && store.countAdmins() == 1) {
                System.out.println("That's the only admin; role not changed.");
            } else {
                u.role = r;
            }
        }
        store.log("User updated: " + u.name);
        store.save();
        System.out.println("User updated.");
    }

    private void deleteUser(User u) {
        if (u == me) {
            System.out.println("You can't delete the account you're logged in with.");
            return;
        }
        if (u.role == Role.ADMIN && store.countAdmins() == 1) {
            System.out.println("Can't delete the only admin.");
            return;
        }
        if (In.yes("Delete " + u.name + (u.role == Role.SELLER ? " and all their products" : ""))) {
            store.deleteUser(u);
            System.out.println("User deleted.");
        }
    }

    // ---- products ----

    private void products() {
        while (true) {
            Fmt.title("Products");
            Views.productTable(store, store.products, true);
            System.out.println("1) Add   2) Edit   3) Remove   0) Back");
            int c = In.integer("Choose", 0, 3);
            if (c == 0) return;
            if (c == 1) {
                addProduct();
                continue;
            }
            Product p = store.product(In.integer("Product ID", 0, Integer.MAX_VALUE));
            if (p == null) {
                System.out.println("No product with that ID.");
            } else if (c == 2) {
                SellerMenu.editProduct(store, p);
            } else if (In.yes("Remove '" + p.name + "'")) {
                store.removeProduct(p);
                System.out.println("Product removed.");
            }
        }
    }

    private void addProduct() {
        List<User> sellers = new ArrayList<>();
        for (User u : store.users) if (u.role == Role.SELLER) sellers.add(u);
        if (sellers.isEmpty()) {
            System.out.println("There are no sellers yet; add a seller account first.");
            return;
        }
        for (User s : sellers) System.out.println("  " + s.id + ") " + s.name);
        User seller = store.user(In.integer("Seller ID", 0, Integer.MAX_VALUE));
        if (seller == null || seller.role != Role.SELLER) {
            System.out.println("That isn't a seller.");
            return;
        }
        SellerMenu.createProduct(store, seller);
    }

    // ---- orders ----

    private void orders() {
        while (true) {
            Fmt.title("Orders");
            Views.orderTable(store, store.orders);
            System.out.println("1) Open an order / change status   0) Back");
            if (In.integer("Choose", 0, 1) == 0) return;
            Order o = Views.pickOrder(store, store.orders);
            if (o == null) continue;
            Views.orderDetail(store, o);
            Views.updateStatus(store, o);
        }
    }
}
