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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Model {
	private final UserDao  userDao   = new UserDaoImpl();
	private final EventDao eventDao  = new EventDaoImpl();
	private final OrderDao orderDao  = new OrderDaoImpl();

	private User currentUser;
	private List<Event> events = new ArrayList<>();
	private final List<CartItem> cart = new ArrayList<>();

	// next order number (1 -> "0001", etc.), computed from existing orders
	private int nextOrderNumber;

	/** Initialize tables, seed events, and determine nextOrderNumber */
	public void setup() throws SQLException, IOException {
		userDao.setup();
		eventDao.setup();
		orderDao.setup();

		// load ALL events (including disabled) from the database
		events = eventDao.getAll();

		// compute next order number
		int maxNum = 0;
		for (Order o : orderDao.getAllOrders()) {
			maxNum = Math.max(maxNum, o.getOrderNumber());
		}
		nextOrderNumber = maxNum + 1;
	}

	// --- User & Event accessors ---

	public UserDao getUserDao()           { return userDao;      }
	public User   getCurrentUser()        { return currentUser;  }
	public void   setCurrentUser(User u)  { this.currentUser = u;}

	/** For normal users: only _enabled_ events */
	public List<Event> getEvents() {
		return events.stream()
				.filter(e -> !e.isDisabled())
				.collect(Collectors.toList());
	}

	/** For admin: all events, including disabled ones */
	public List<Event> getAllEventsIncludingDisabled() {
		return new ArrayList<>(events);
	}

	// --- Cart operations ---

	public List<CartItem> getCart() { return cart; }

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
	 * Persist all orders to a text file.
	 */
	public void exportOrders(File file) throws IOException {
		List<Order> orders;
		try {
			orders = orderDao.getAllOrders();
		} catch (SQLException ex) {
			throw new IOException("Failed to load orders", ex);
		}
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		try (FileWriter fw = new FileWriter(file)) {
			for (Order o : orders) {
				fw.write("Order Number: " + String.format("%04d", o.getOrderNumber()) + "\n");
				fw.write("Date & Time  : " + o.getTimestamp().format(fmt) + "\n");
				fw.write("Items:\n");
				for (CartItem ci : o.getItems()) {
					fw.write("  • " + ci.getEvent().getName()
							+ " — " + ci.getQuantity() + " seats\n");
				}
				fw.write(String.format("Total        : $%.2f%n%n", o.getTotal()));
			}
		}
	}

	// ─── Admin-only methods ────────────────────────────────────────────────────

	/**
	 * Toggle a specific event’s disabled flag and persist to the DB immediately.
	 */
	public void setEventDisabled(Event e, boolean disabled) {
		e.setDisabled(disabled);
		try {
			eventDao.updateDisabled(e.getId(), disabled);
		} catch (SQLException ex) {
			throw new RuntimeException("Failed to persist disabled flag", ex);
		}
	}

	/** Return all past orders (newest first). */
	public List<Order> getOrders() throws SQLException {
		return orderDao.getAllOrders();
	}
	// model/Model.java

	/** Add a brand-new event (check for dupes first). */
	public void addEvent(Event e) {
		// duplicate check: same name/venue/date?
		boolean dup = events.stream()
				.anyMatch(o -> o.getName().equals(e.getName())
						&& o.getVenue().equals(e.getVenue())
						&& o.getDate().equals(e.getDate()));
		if (dup) throw new IllegalArgumentException("That event already exists");
		try {
			eventDao.insertEvent(e);
			events.add(e);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** Remove an event (will wipe historical bookings in DB!). */
	public void deleteEvent(Event e) {
		try {
			eventDao.deleteEvent(e.getId());
			events.removeIf(o -> o.getId() == e.getId());
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** Edit an existing event’s details. */
	public void editEvent(Event updated) {
		// duplicate check against others
		boolean dup = events.stream()
				.anyMatch(o -> o.getId() != updated.getId()
						&& o.getName().equals(updated.getName())
						&& o.getVenue().equals(updated.getVenue())
						&& o.getDate().equals(updated.getDate()));
		if (dup) throw new IllegalArgumentException("That event would collide with an existing one");
		try {
			eventDao.updateEvent(updated);
			// update in‐memory
			for (int i=0; i<events.size(); i++) {
				if (events.get(i).getId() == updated.getId()) {
					events.set(i, updated);
					break;
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

}
