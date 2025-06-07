package dao;

import model.CartItem;
import model.Event;
import model.Order;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderDaoImpl implements OrderDao {
    private static final String ORDERS_TABLE = "orders";
    private static final String ITEMS_TABLE  = "order_items";
    // ISO format for storing timestamps
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void setup() throws SQLException, IOException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // create orders table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  order_number  TEXT    NOT NULL,
                  timestamp     TEXT    NOT NULL,
                  total         REAL    NOT NULL
                )
            """);

            // create order_items table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS order_items (
                  order_id    INTEGER NOT NULL REFERENCES orders(id),
                  event_id    INTEGER NOT NULL REFERENCES events(id),
                  quantity    INTEGER NOT NULL,
                  PRIMARY KEY (order_id, event_id)
                )
            """);
        }
    }

    @Override
    public void saveOrder(Order order) throws SQLException {
        String insertOrderSql = "INSERT INTO orders(order_number, timestamp, total) VALUES(?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {

            // order_number e.g. "0001"
            pst.setString(1, String.format("%04d", order.getOrderNumber()));
            pst.setString(2, order.getTimestamp().format(FMT));
            pst.setDouble(3, order.getTotal());
            pst.executeUpdate();

            // grab the generated PK
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to retrieve generated order id");
                }
                int orderId = keys.getInt(1);

                // now insert each item
                String insertItemSql =
                        "INSERT INTO order_items(order_id, event_id, quantity) VALUES(?,?,?)";
                try (PreparedStatement ipst = conn.prepareStatement(insertItemSql)) {
                    for (CartItem ci : order.getItems()) {
                        ipst.setInt(1, orderId);
                        ipst.setInt(2, ci.getEvent().getId());
                        ipst.setInt(3, ci.getQuantity());
                        ipst.addBatch();
                    }
                    ipst.executeBatch();
                }
            }
        }
    }

    @Override
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();

        // 1) load orders themselves, newest first
        String loadOrdersSql =
                "SELECT id, order_number, timestamp, total\n" +
                        "  FROM orders\n" +
                        " ORDER BY id DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(loadOrdersSql)) {

            while (rs.next()) {
                int    num = Integer.parseInt(rs.getString("order_number"));
                LocalDateTime ts =
                        LocalDateTime.parse(rs.getString("timestamp"), FMT);
                double tot = rs.getDouble("total");
                orders.add(new Order(num, ts, new ArrayList<>(), tot));
            }
        }

        if (orders.isEmpty()) {
            return orders;
        }

        // 2) load all items for these orders
        String loadItemsSql = """
            SELECT oi.order_id, oi.event_id, oi.quantity,
                   e.name, e.date, e.venue, e.price, e.remainingSeats
              FROM order_items oi
              JOIN events e ON oi.event_id = e.id
             ORDER BY oi.order_id DESC
            """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(loadItemsSql)) {

            while (rs.next()) {
                int orderId  = rs.getInt("order_id");
                int eventId  = rs.getInt("event_id");
                int quantity = rs.getInt("quantity");

                // find the matching Order in our list (by insertion order == DB id DESC)
                for (Order o : orders) {
                    // order.getOrderNumber() was the formatted 4‐digit code, not the PK,
                    // so we pull the PK via helper if needed:
                    int oNum = o.getOrderNumber();
                    // we stored string‐formatted 4‐digits in DB
                    String dbNum = String.format("%04d", oNum);
                    if (dbNum.equals(getOrderNumberById(conn, orderId))) {
                        // reconstitute Event
                        Event e = new Event(
                                eventId,
                                rs.getString("name"),
                                rs.getString("date"),
                                rs.getString("venue"),
                                rs.getDouble("price"),
                                rs.getInt("remainingSeats")
                        );
                        o.getItems().add(new CartItem(e, quantity));
                        break;
                    }
                }
            }
        }

        return orders;
    }

    /** Helper to look up the 4‐digit order_number by the PK id. */
    private String getOrderNumberById(Connection conn, int id) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT order_number FROM orders WHERE id = ?")) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("order_number");
                else throw new SQLException("No order_number for id " + id);
            }
        }
    }
}
