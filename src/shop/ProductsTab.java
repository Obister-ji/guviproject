package shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Product table with add / edit / remove. Admins see everything; a seller only sees their own. */
final class ProductsTab extends Tab {
    private static final String[] FIELDS = {"Name", "Description", "Category", "Price", "Stock"};

    private final Store store;
    private final Supplier<List<Product>> source;
    private final User fixedSeller; // null for the admin, who picks a seller per product
    private final DefaultTableModel model = Ui.model("ID", "Name", "Category", "Price", "Stock", "Seller");
    private final JTable table = Ui.table(model);

    ProductsTab(Store store, Supplier<List<Product>> source, User fixedSeller) {
        this.store = store;
        this.source = source;
        this.fixedSeller = fixedSeller;
        add(new JScrollPane(table));
        add(Ui.bar(Ui.button("Add product", this::create), Ui.button("Edit", this::edit),
                Ui.button("Remove", this::remove)), java.awt.BorderLayout.NORTH);
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        for (Product p : source.get()) {
            model.addRow(new Object[]{p.id, p.name, p.category, Fmt.money(p.price), p.stock, store.userName(p.sellerId)});
        }
    }

    private void create() {
        User seller = fixedSeller != null ? fixedSeller : pickSeller();
        if (seller == null) return;
        String[] v = Ui.form(this, "New product", FIELDS, null);
        if (v == null || !valid(v)) return;
        store.addProduct(seller.id, v[0], v[1], v[2], Double.parseDouble(v[3]), Integer.parseInt(v[4]));
        refresh();
    }

    private void edit() {
        Product p = selected();
        if (p == null) return;
        String[] now = {p.name, p.description, p.category, String.valueOf(p.price), String.valueOf(p.stock)};
        String[] v = Ui.form(this, "Edit product", FIELDS, now);
        if (v == null || !valid(v)) return;
        p.name = v[0];
        p.description = v[1];
        p.category = v[2];
        p.price = Double.parseDouble(v[3]);
        p.stock = Integer.parseInt(v[4]);
        store.log("Product updated: " + p.name);
        store.save();
        refresh();
    }

    private void remove() {
        Product p = selected();
        if (p != null && Ui.confirm(this, "Remove '" + p.name + "' from the store?")) {
            store.removeProduct(p);
            refresh();
        }
    }

    private Product selected() {
        int id = Ui.selectedId(this, table);
        return id < 0 ? null : store.product(id);
    }

    private boolean valid(String[] v) {
        return !Ui.blank(this, v[0], "Name") && !Ui.blank(this, v[1], "Description") && !Ui.blank(this, v[2], "Category")
                && Ui.amount(this, v[3], "Price") != null && Ui.whole(this, v[4], "Stock", 0) != null;
    }

    private User pickSeller() {
        List<String> options = new ArrayList<>();
        for (User u : store.users) {
            if (u.role == Role.SELLER) options.add(u.id + " - " + u.name);
        }
        if (options.isEmpty()) {
            Ui.info(this, "There are no sellers yet. Add a seller account first.");
            return null;
        }
        Object pick = javax.swing.JOptionPane.showInputDialog(this, "List this product for which seller?", "Seller",
                javax.swing.JOptionPane.QUESTION_MESSAGE, null, options.toArray(), options.get(0));
        if (pick == null) return null;
        String s = pick.toString();
        return store.user(Integer.parseInt(s.substring(0, s.indexOf(' '))));
    }
}
