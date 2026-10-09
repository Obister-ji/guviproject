# Veni, Vidi, Vici

The whole app runs on three beats, and the screens follow them:

| | Meaning here | What happens |
|---|---|---|
| **Veni** (I came) | Sign up / log in | Buyers and sellers register themselves. The admin account already exists. Your role decides which dashboard you land on. |
| **Vidi** (I saw) | Browse and monitor | Buyers search and filter products. Sellers watch stock and sales. The admin watches users, products, orders and the activity feed. |
| **Vici** (I conquered) | Buy, process, fulfil | A buyer checks out, the seller moves the order along (placed → confirmed → shipped → delivered), the buyer tracks it. |

---

# guviproject - Online E-commerce Platform

College project for GUVI. A desktop shop written in plain Java (Swing, no external libraries), where an admin, sellers and buyers each get their own dashboard.

## Running it

You need JDK 11 or newer.

```bash
javac -d out src/shop/*.java
java -cp out shop.Main
```

First run creates a `data/` folder and saves everything to `data/store.ser`, so your products and orders are still there next time. Delete that folder to start over.

Default admin: `admin@shop.local` / `admin123`. Change it from the Users tab once you're in (add a new admin, delete this one).

## What each role can do

**Admin**
- Add, edit and delete users and change roles (it won't let you delete yourself or the last admin)
- See every product, add one on behalf of a seller, edit or remove it
- See every order and change its status
- Overview tab shows totals and the latest activity; the full log has its own tab

**Seller**
- List, edit and remove their own products
- Inventory tab with a bar per product for stock level, turning red and flagged `LOW` at 5 units or fewer
- See orders for their products and push them through the status steps
- Sales charts: revenue per product and per day (cancelled orders are left out)

**Buyer**
- Search by keyword, category and price range
- Cart and checkout. A cart with items from two sellers is split into two orders, one per seller
- Order history with a timeline per order, and cancelling while it hasn't shipped
- Wishlist that remembers the price you saw and tells you if it went up or down
- Recently viewed list, saved address and payment preference

## Rules worth knowing

- Stock drops when an order is placed and comes back if it's cancelled.
- Status can only move forward: PLACED → CONFIRMED → SHIPPED → DELIVERED. Cancel is allowed up to CONFIRMED.
- Passwords are stored as salted SHA-256 hashes.
- "Payment method" is just a label (cash on delivery, UPI, card). No real payments and no card numbers are stored.

## Layout

```
src/shop/
  Main.java, App.java            window, login and sign-up
  AdminPanel / SellerPanel / BuyerPanel    one dashboard per role, one tab per feature
  ProductsTab, OrdersTab         tabs shared by admin and seller (and buyer for orders)
  Store.java         data, saving, and the rules that span several objects (checkout, status changes)
  User, Product, Order, Role, OrderStatus    the model
  Tab, Ui, Fmt       small Swing and formatting helpers
```

## Known limits

- Desktop app, one user at a time. The "real-time" activity feed refreshes whenever you open a tab, not on a timer.
- Storage is Java serialization to one file, not a database. Changing the model classes can make an old `data/store.ser` unreadable; delete it if that happens.
- Prices use `double`, which is fine for a demo but `BigDecimal` would be the right choice for real money.
