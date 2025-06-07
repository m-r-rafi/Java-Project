package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CartItem {
    private final Event event;
    private final IntegerProperty quantity = new SimpleIntegerProperty();

    public CartItem(Event event, int initialQuantity) {
        this.event = event;
        this.quantity.set(initialQuantity);
    }

    public Event getEvent() {
        return event;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int q) {
        quantity.set(q);
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }
}
