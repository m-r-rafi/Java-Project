package model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.DoubleProperty;

/**
 * Represents one event instance, with its own “disabled” flag for admin control.
 */
public class Event {
    private final IntegerProperty id             = new SimpleIntegerProperty();
    private final StringProperty  name           = new SimpleStringProperty();
    private final StringProperty  date           = new SimpleStringProperty();
    private final StringProperty  venue          = new SimpleStringProperty();
    private final DoubleProperty  price          = new SimpleDoubleProperty();
    private final IntegerProperty remainingSeats = new SimpleIntegerProperty();
    private final BooleanProperty disabled       = new SimpleBooleanProperty(false);

    /**
     * Full constructor including disabled state.
     *
     * @param id               unique event ID
     * @param name             event name
     * @param date             date string (e.g. "Mon")
     * @param venue            venue name
     * @param price            ticket price
     * @param remainingSeats   seats still available
     * @param isDisabled       initial disabled flag
     */
    public Event(int id,
                 String name,
                 String date,
                 String venue,
                 double price,
                 int remainingSeats,
                 boolean isDisabled) {
        this.id.set(id);
        this.name.set(name);
        this.date.set(date);
        this.venue.set(venue);
        this.price.set(price);
        this.remainingSeats.set(remainingSeats);
        this.disabled.set(isDisabled);
    }

    /**
     * Convenience constructor for non‐disabled events.
     */
    public Event(int id,
                 String name,
                 String date,
                 String venue,
                 double price,
                 int remainingSeats) {
        this(id, name, date, venue, price, remainingSeats, false);
    }

    // ─── Properties ────────────────────────────────────────────────────────────

    public IntegerProperty    idProperty()             { return id; }
    public StringProperty     nameProperty()           { return name; }
    public StringProperty     dateProperty()           { return date; }
    public StringProperty     venueProperty()          { return venue; }
    public DoubleProperty     priceProperty()          { return price; }
    public IntegerProperty    remainingSeatsProperty() { return remainingSeats; }
    public BooleanProperty    disabledProperty()       { return disabled; }

    // ─── Raw getters and setters ──────────────────────────────────────────────

    public int     getId()              { return id.get(); }
    public String  getName()            { return name.get(); }
    public String  getDate()            { return date.get(); }
    public String  getVenue()           { return venue.get(); }
    public double  getPrice()           { return price.get(); }
    public int     getRemainingSeats()  { return remainingSeats.get(); }

    /** @return true if this event is currently disabled by the admin */
    public boolean isDisabled()         { return disabled.get(); }

    /** Enable or disable this event */
    public void setDisabled(boolean v)  { disabled.set(v); }
}
