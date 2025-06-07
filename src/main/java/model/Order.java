package model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents one historical order: its 4-digit order number, timestamp,
 * the items booked (event + qty), and the total.
 */
public class Order {
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int orderNumber;
    private final LocalDateTime timestamp;
    private final List<CartItem> items;
    private final double total;

    // JavaFX properties for binding to a TableView
    private final StringProperty orderNumberProp;
    private final StringProperty timestampProp;
    private final StringProperty detailsProp;
    private final DoubleProperty totalProp;

    public Order(int orderNumber,
                 LocalDateTime timestamp,
                 List<CartItem> items,
                 double total)
    {
        this.orderNumber = orderNumber;
        this.timestamp   = timestamp;
        this.items       = items;
        this.total       = total;

        this.orderNumberProp = new SimpleStringProperty(
                String.format("%04d", orderNumber)
        );
        this.timestampProp = new SimpleStringProperty(
                timestamp.format(TS_FMT)
        );
        this.detailsProp = new SimpleStringProperty(
                items.stream()
                        .map(ci -> ci.getEvent().getName() + " x" + ci.getQuantity())
                        .collect(Collectors.joining(", "))
        );
        this.totalProp = new SimpleDoubleProperty(total);
    }

    // raw getters
    public int getOrderNumber()         { return orderNumber; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<CartItem> getItems()    { return items; }
    public double getTotal()            { return total; }

    // JavaFX property getters
    public StringProperty orderNumberProperty() { return orderNumberProp; }
    public StringProperty timestampProperty()   { return timestampProp; }
    public StringProperty detailsProperty()     { return detailsProp; }
    public DoubleProperty totalProperty()       { return totalProp; }
}
