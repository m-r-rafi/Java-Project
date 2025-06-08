package dao;

import model.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDaoImpl implements EventDao {
    private static final String TABLE = "events";

    @Override
    public void setup() throws SQLException, IOException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create table if needed (with disabled column)
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                            + "id              INTEGER PRIMARY KEY, "
                            + "name            TEXT    NOT NULL, "
                            + "date            TEXT    NOT NULL, "
                            + "venue           TEXT    NOT NULL, "
                            + "price           REAL    NOT NULL, "
                            + "remainingSeats  INTEGER NOT NULL, "
                            + "disabled        INTEGER NOT NULL DEFAULT 0"
                            + ")"
            );
            // 2. In case we upgraded from an older schema, try to add the column
            try {
                stmt.executeUpdate(
                        "ALTER TABLE " + TABLE
                                + " ADD COLUMN disabled INTEGER NOT NULL DEFAULT 0"
                );
            } catch (SQLException ex) {
                // already exists, ignore
            }
        }

        // 3. Seed from events.dat if empty
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (InputStream in = getClass().getResourceAsStream("/events.dat");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                     PreparedStatement ins = conn.prepareStatement(
                             "INSERT INTO " + TABLE +
                                     "(id,name,date,venue,price,remainingSeats) VALUES (?,?,?,?,?,?)"
                     )) {
                    String line;
                    int id = 1;
                    while ((line = reader.readLine()) != null) {
                        String[] p = line.split(";");
                        String name    = p[0];
                        String venue   = p[1];
                        String day     = p[2];
                        double price   = Double.parseDouble(p[3]);
                        int sold       = Integer.parseInt(p[4]);
                        int capacity   = Integer.parseInt(p[5]);
                        int remaining  = capacity - sold;

                        ins.setInt   (1, id++);
                        ins.setString(2, name);
                        ins.setString(3, day);
                        ins.setString(4, venue);
                        ins.setDouble(5, price);
                        ins.setInt   (6, remaining);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public List<Event> getAll() throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE)) {
            while (rs.next()) {
                list.add(new Event(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("date"),
                        rs.getString("venue"),
                        rs.getDouble("price"),
                        rs.getInt("remainingSeats"),
                        rs.getInt("disabled") != 0      // pass disabled flag
                ));
            }
        }
        return list;
    }

    @Override
    public void updateRemainingSeats(int eventId, int seatsLeft) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE " + TABLE + " SET remainingSeats = ? WHERE id = ?"
             )) {
            pst.setInt(1, seatsLeft);
            pst.setInt(2, eventId);
            pst.executeUpdate();
        }
    }

    @Override
    public void updateDisabled(int eventId, boolean disabled) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE " + TABLE + " SET disabled = ? WHERE id = ?"
             )) {
            pst.setInt(1, disabled ? 1 : 0);
            pst.setInt(2, eventId);
            pst.executeUpdate();
        }
    }
}
