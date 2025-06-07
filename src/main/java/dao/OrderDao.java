package dao;

import model.Order;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO for persisting and loading order history.
 */
public interface OrderDao {
    /** Create tables if they don’t already exist. */
    void setup() throws SQLException, IOException;

    /** Save a single order (and its items) to the database. */
    void saveOrder(Order order) throws SQLException;

    /** Load all orders (with their items) in reverse‐chronological order. */
    List<Order> getAllOrders() throws SQLException;
}
