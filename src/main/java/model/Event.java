package model;

import javafx.beans.property.*;

public class Event {
    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final StringProperty  name         = new SimpleStringProperty();
    private final StringProperty  date         = new SimpleStringProperty();
    private final StringProperty  venue        = new SimpleStringProperty();
    private final DoubleProperty  price        = new SimpleDoubleProperty();
    private final IntegerProperty remaining    = new SimpleIntegerProperty();

    public Event(int id,
                 String name,
                 String date,
                 String venue,
                 double price,
                 int remainingSeats) {
        this.id.set(id);
        this.name.set(name);
        this.date.set(date);
        this.venue.set(venue);
        this.price.set(price);
        this.remaining.set(remainingSeats);
    }

    public IntegerProperty    idProperty()           { return id; }
    public StringProperty     nameProperty()         { return name; }
    public StringProperty     dateProperty()         { return date; }
    public StringProperty     venueProperty()        { return venue; }
    public DoubleProperty     priceProperty()        { return price; }
    public IntegerProperty    remainingSeatsProperty(){ return remaining; }

    public int     getId()            { return id.get(); }
    public String  getName()          { return name.get(); }
    public String  getDate()          { return date.get(); }
    public String  getVenue()         { return venue.get(); }
    public double  getPrice()         { return price.get(); }
    public int     getRemainingSeats(){ return remaining.get(); }
}
