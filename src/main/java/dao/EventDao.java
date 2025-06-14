package dao;

import model.Event;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface EventDao {
    /** Ensure the table exists and is populated from events.dat if empty */
    void setup() throws SQLException, IOException;

    /** Fetch all events (with current remainingSeats) */
    List<Event> getAll() throws SQLException;

    /** Update the remainingSeats for a single event */
    void updateRemainingSeats(int eventId, int seatsLeft) throws SQLException;
    void updateDisabled(int eventId, boolean disabled) throws SQLException;
    // dao/EventDao.java
    void insertEvent(Event e) throws SQLException;
    void deleteEvent(int eventId) throws SQLException;
    void updateEvent(Event e) throws SQLException;


}
