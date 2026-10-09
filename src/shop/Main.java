package shop;

public class Main {
    public static void main(String[] args) {
        Store store = Store.open();
        System.out.println("Online shop (console edition)");
        while (true) {
            System.out.println();
            System.out.println("1) Log in   2) Sign up   0) Exit");
            switch (In.integer("Choose", 0, 2)) {
                case 1: login(store); break;
                case 2: signUp(store); break;
                default:
                    store.save();
                    System.out.println("Bye.");
                    return;
            }
        }
    }

    private static void login(Store store) {
        String email = In.required("Email");
        String pw = In.required("Password");
        User u = store.login(email, pw);
        if (u == null) {
            System.out.println("Wrong email or password.");
            return;
        }
        store.log(u.name + " logged in");
        switch (u.role) {
            case ADMIN:  new AdminMenu(store, u).run(); break;
            case SELLER: new SellerMenu(store, u).run(); break;
            default:     new BuyerMenu(store, u).run(); break;
        }
        store.log(u.name + " logged out");
        store.save();
    }

    private static void signUp(Store store) {
        System.out.println("Sign up as: 1) Buyer   2) Seller   0) Cancel");
        int c = In.integer("Choose", 0, 2);
        if (c == 0) return;
        String name = In.required("Name");
        String email;
        while (true) {
            email = In.required("Email");
            String problem = store.checkEmail(email, null);
            if (problem == null) break;
            System.out.println("  " + problem);
        }
        String pw = In.required("Password");
        store.addUser(name, email, pw, c == 1 ? Role.BUYER : Role.SELLER);
        System.out.println("Account created. You can log in now.");
    }
}
