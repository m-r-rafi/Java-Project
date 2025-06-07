package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One row in the admin’s table: the show title, and each date/venue on its own line.
 */
public class ShowSummary {
    private final StringProperty titleProperty;
    private final StringProperty optionsProperty;

    public ShowSummary(String title, List<Event> datesAndVenues) {
        this.titleProperty   = new SimpleStringProperty(title);
        // build a list of lines like "Mon – Theatre Nova"
        List<String> lines = datesAndVenues.stream()
                .map(e -> e.getDate() + " – " + e.getVenue())
                .collect(Collectors.toList());
        // join with newline
        String joined = String.join("\n", lines);
        this.optionsProperty = new SimpleStringProperty(joined);
    }

    public StringProperty titleProperty()   { return titleProperty; }
    public StringProperty optionsProperty() { return optionsProperty; }
}
