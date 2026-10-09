# Veni, Vidi, Vici

The whole app runs on three beats, and the menus follow them:

| | Meaning here | What happens |
|---|---|---|
| **Veni** (I came) | Sign up / log in | Buyers and sellers register themselves. The admin account already exists. Your role decides which dashboard you land on. |
| **Vidi** (I saw) | Browse and monitor | Buyers search and filter products. Sellers watch stock and sales. The admin watches users, products, orders and the activity feed. |
| **Vici** (I conquered) | Buy, process, fulfil | A buyer checks out, the seller moves the order along (placed → confirmed → shipped → delivered), the buyer tracks it. |

---

# guviproject - Online E-commerce Platform

College project for GUVI. A console-based shop written in plain Java with no libraries, where an admin, sellers and buyers each get their own dashboard.

## Running it

You need JDK 11 or newer.

```bash
javac -d out src/shop/*.java
java -cp out shop.Main
```

First run creates a `data/` folder and saves everything to `data/store.ser`, so your products and orders are still there next time. Delete that folder to start over.

Default admin: `admin@shop.local` / `admin123`. Change it from the admin menu once you're in (add a new admin, delete this one).

## What each role can do

**Admin**
- Add, edit and delete users and change roles (it won't let you delete yourself or the last admin)
- See every product, add one on behalf of a seller, edit or remove it
- See every order and change its status
- Dashboard shows totals and the latest activity; the full log is one menu away

**Seller**
- List, edit and remove their own products
- Inventory screen with bars for stock levels, and a `LOW` flag at 5 units or fewer
- See orders for their products and push them through the status steps
- Sales report per product and per day (cancelled orders are left out)

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
  Main.java          login and sign-up
  AdminMenu.java     admin dashboard
  SellerMenu.java    seller dashboard
  BuyerMenu.java     buyer dashboard
  Store.java         data, saving, and the rules that span several objects (checkout, status changes)
  User, Product, Order, Role, OrderStatus    the model
  Views, Fmt, In     tables, formatting, input helpers
```

## Known limits

- Console only, single user at a time. The "real-time" activity feed is the log refreshed each time a dashboard is drawn.
- Storage is Java serialization to one file, not a database. Changing the model classes can make an old `data/store.ser` unreadable; delete it if that happens.
- Prices use `double`, which is fine for a demo but `BigDecimal` would be the right choice for real money.
