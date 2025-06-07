package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One row in the admin’s grouped-view of events.
 */
public class ShowSummary {
    private final StringProperty title;
    private final StringProperty options;

    public ShowSummary(String title, List<Event> variants) {
        this.title = new SimpleStringProperty(title);
        // each variant: e.getDate() + " – " + e.getVenue()
        String opts = variants.stream()
                .map(e -> e.getDate() + " – " + e.getVenue())
                .collect(Collectors.joining(", "));
        this.options = new SimpleStringProperty(opts);
    }

    public StringProperty titleProperty()   { return title; }
    public StringProperty optionsProperty() { return options; }
}
