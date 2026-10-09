package shop;

public enum OrderStatus {
    PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    /** Orders move forward one step at a time; they can be cancelled until they ship. */
    public boolean canMoveTo(OrderStatus next) {
        switch (this) {
            case PLACED:    return next == CONFIRMED || next == CANCELLED;
            case CONFIRMED: return next == SHIPPED || next == CANCELLED;
            case SHIPPED:   return next == DELIVERED;
            default:        return false;
        }
    }
}
