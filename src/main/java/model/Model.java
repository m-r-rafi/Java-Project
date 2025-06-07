package model;

import dao.EventDao;
import dao.EventDaoImpl;
import dao.OrderDao;
import dao.OrderDaoImpl;
import dao.UserDao;
import dao.UserDaoImpl;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.time.LocalDateTime;

public class Model {
	private final UserDao  userDao   = new UserDaoImpl();
	private final EventDao eventDao  = new EventDaoImpl();
	private final OrderDao orderDao  = new OrderDaoImpl();

	private User currentUser;
	private List<Event> events;
	private final List<CartItem> cart = new ArrayList<>();

	// next order number (1 -> "0001", etc.), computed from existing orders
	private int nextOrderNumber;

	/** Initialize tables, seed events, and determine nextOrderNumber */
	public void setup() throws SQLException, IOException {
		userDao.setup();
		eventDao.setup();
		orderDao.setup();

		// load events from DB
		events = eventDao.getAll();

		// figure out what the next order number should be
		int maxNum = 0;
		for (Order o : orderDao.getAllOrders()) {
			maxNum = Math.max(maxNum, o.getOrderNumber());
		}
		nextOrderNumber = maxNum + 1;
	}

	// --- User, Event, Cart accessors ---
	public UserDao getUserDao()           { return userDao; }
	public User   getCurrentUser()        { return currentUser; }
	public void   setCurrentUser(User u)  { this.currentUser = u; }
	public List<Event>    getEvents()     { return events; }
	public List<CartItem> getCart()       { return cart; }

	/**
	 * Return all past orders (newest first).
	 * This is the method your OrdersController needs.
	 */
	public List<Order> getOrders() throws SQLException {
		return orderDao.getAllOrders();
	}

	// --- Cart operations ---
	public void addToCart(Event e, int qty) {
		for (CartItem ci : cart) {
			if (ci.getEvent().equals(e)) {
				ci.setQuantity(ci.getQuantity() + qty);
				return;
			}
		}
		cart.add(new CartItem(e, qty));
	}

	public void updateCart(Event e, int newQty) {
		Iterator<CartItem> it = cart.iterator();
		while (it.hasNext()) {
			CartItem ci = it.next();
			if (ci.getEvent().equals(e)) {
				if (newQty <= 0) it.remove();
				else            ci.setQuantity(newQty);
				return;
			}
		}
	}

	public void removeFromCart(Event e) {
		updateCart(e, 0);
	}

	public boolean validateCart() {
		for (CartItem ci : cart) {
			if (ci.getQuantity() > ci.getEvent().getRemainingSeats()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Complete a checkout:
	 * 1) validate availability,
	 * 2) deduct seats (in memory & DB),
	 * 3) record an Order via orderDao,
	 * 4) clear the cart.
	 */
	public void checkout() {
		if (!validateCart()) {
			throw new IllegalStateException("Not enough seats for one or more items.");
		}

		// snapshot + total
		List<CartItem> snapshot = new ArrayList<>(cart);
		double total = snapshot.stream()
				.mapToDouble(ci -> ci.getEvent().getPrice() * ci.getQuantity())
				.sum();

		// deduct seats and persist to events table
		for (CartItem ci : snapshot) {
			Event e = ci.getEvent();
			int newRem = e.getRemainingSeats() - ci.getQuantity();
			e.remainingSeatsProperty().set(newRem);
			try {
				eventDao.updateRemainingSeats(e.getId(), newRem);
			} catch (SQLException ex) {
				throw new RuntimeException("Failed to persist seat update", ex);
			}
		}


		// record & persist the order
		Order order = new Order(nextOrderNumber, LocalDateTime.now(), snapshot, total);
		try {
			orderDao.saveOrder(order);
		} catch (SQLException ex) {
			throw new RuntimeException("Failed to save order", ex);
		}
		nextOrderNumber++;

		cart.clear();
	}
	/**
	 * Export all orders to a text file in the user-specified location.
	 * Format:
	 * Order Number: ####
	 * Date & Time: YYYY-MM-DD HH:MM:SS
	 * Items:
	 *   Event A x2
	 *   Event B x1
	 * Total: $123.45
	 *
	 * @param file the file to write to
	 */
	public void exportOrders(File file) throws IOException {
		List<Order> orders;
		try {
			orders = orderDao.getAllOrders();
		} catch (SQLException ex) {
			throw new IOException("Failed to load orders for export", ex);
		}

		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		try (FileWriter fw = new FileWriter(file)) {
			for (Order o : orders) {
				fw.write("Order Number: " + String.format("%04d", o.getOrderNumber()) + "\n");
				fw.write("Date & Time  : " + o.getTimestamp().format(fmt) + "\n");
				fw.write("Items:\n");
				for (CartItem ci : o.getItems()) {
					fw.write("  • "
							+ ci.getEvent().getName()
							+ " — "
							+ ci.getQuantity()
							+ " seats\n");
				}
				fw.write(String.format("Total        : $%.2f%n%n", o.getTotal()));
			}
		}
	}
}
