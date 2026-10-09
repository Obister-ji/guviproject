package shop;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One order belongs to exactly one seller; a cart with items from two sellers becomes two orders. */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Line implements Serializable {
        private static final long serialVersionUID = 1L;
        final int productId;
        final String name;
        final double unitPrice;
        final int qty;

        Line(int productId, String name, double unitPrice, int qty) {
            this.productId = productId;
            this.name = name;
            this.unitPrice = unitPrice;
            this.qty = qty;
        }

        double subtotal() {
            return unitPrice * qty;
        }
    }

    final int id;
    final int buyerId;
    final int sellerId;
    final String shipTo;
    final String payment;
    final LocalDateTime placedAt = LocalDateTime.now();
    final List<Line> lines = new ArrayList<>();
    final List<String> timeline = new ArrayList<>();
    OrderStatus status = OrderStatus.PLACED;

    Order(int id, int buyerId, int sellerId, String shipTo, String payment) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.shipTo = shipTo;
        this.payment = payment;
        timeline.add(Fmt.stamp(placedAt) + "  PLACED");
    }

    double total() {
        double t = 0;
        for (Line l : lines) {
            t += l.subtotal();
        }
        return t;
    }
}
