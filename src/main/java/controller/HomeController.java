package controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import model.Event;
import model.Model;
import model.User;

public class HomeController {
	@FXML private Label welcomeLabel;
	@FXML private TableView<Event> eventsTable;
	@FXML private TableColumn<Event, Integer> idCol;
	@FXML private TableColumn<Event, String>  nameCol;
	@FXML private TableColumn<Event, String>  dateCol;
	@FXML private TableColumn<Event, String>  venueCol;
	@FXML private TableColumn<Event, Double>  priceCol;
	@FXML private TableColumn<Event, Integer> seatsCol;

	@FXML private Button viewCartBtn;
	@FXML private Button viewOrdersBtn;// ← new
	@FXML private Button bookSeatsBtn;
	@FXML private Button logoutBtn;

	private final Model model;
	private final Stage stage;

	public HomeController(Stage stage, Model model) {
		this.stage = stage;
		this.model = model;
	}

	@FXML
	public void initialize() {
		// 1) Personalized greeting
		User me = model.getCurrentUser();
		welcomeLabel.setText("Welcome, " + me.getPreferredName() + "!");

		// 2) Table columns
		idCol   .setCellValueFactory(c -> c.getValue().idProperty().asObject());
		nameCol .setCellValueFactory(c -> c.getValue().nameProperty());
		dateCol .setCellValueFactory(c -> c.getValue().dateProperty());
		venueCol.setCellValueFactory(c -> c.getValue().venueProperty());
		priceCol.setCellValueFactory(c -> c.getValue().priceProperty().asObject());
		seatsCol.setCellValueFactory(c -> c.getValue().remainingSeatsProperty().asObject());

		// 3) Load events into table
		List<Event> events = model.getEvents();
		eventsTable.setItems(FXCollections.observableArrayList(events));

		// 4) View Cart button
		viewCartBtn.setOnAction(e -> {
			try {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/view/CartView.fxml")
				);
				loader.setControllerFactory(type -> {
					if (type == controller.CartController.class) {
						return new controller.CartController(stage, model);
					}
					try {
						return type.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});
				Parent cartRoot = loader.load();
				stage.setScene(new Scene(cartRoot, 600, 400));
				stage.setTitle("Your Cart");
				stage.show();
			} catch (IOException ex) {
				new Alert(Alert.AlertType.ERROR,
						"Failed to open cart: " + ex.getMessage(),
						ButtonType.OK)
						.showAndWait();
			}
		});

		viewOrdersBtn.setOnAction(e -> {
			try {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/view/OrdersView.fxml")
				);
				loader.setControllerFactory(type -> {
					if (type == OrdersController.class) {
						return new OrdersController(stage, model);
					}
					// fall-back for other controllers:
					try {
						return type.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});
				Parent ordersRoot = loader.load();
				stage.setScene(new Scene(ordersRoot, 700, 500));
				stage.setTitle("Order History");
				stage.show();
			} catch (IOException ex) {
				new Alert(Alert.AlertType.ERROR,
						"Failed to load order history:\n" + ex.getMessage(),
						ButtonType.OK).showAndWait();
			}
		});

		// 5) Book Seats button handler
		bookSeatsBtn.setOnAction(e -> {
			Event selected = eventsTable.getSelectionModel().getSelectedItem();
			if (selected == null) {
				new Alert(Alert.AlertType.WARNING,
						"Please select an event first.",
						ButtonType.OK)
						.showAndWait();
				return;
			}

			TextInputDialog dlg = new TextInputDialog("1");
			dlg.setTitle("Book Seats");
			dlg.setHeaderText("Booking: " + selected.getName());
			dlg.setContentText("Enter number of seats:");
			Optional<String> res = dlg.showAndWait();
			if (!res.isPresent()) return;

			int qty;
			try {
				qty = Integer.parseInt(res.get().trim());
				if (qty < 1) throw new NumberFormatException();
			} catch (NumberFormatException exQty) {
				new Alert(Alert.AlertType.ERROR,
						"Please enter a valid positive number.",
						ButtonType.OK)
						.showAndWait();
				return;
			}

			// check what's already in the cart too
			int already = model.getCart().stream()
					.filter(ci -> ci.getEvent().equals(selected))
					.mapToInt(ci -> ci.getQuantity())
					.sum();
			if (qty + already > selected.getRemainingSeats()) {
				new Alert(Alert.AlertType.ERROR,
						"Only " + selected.getRemainingSeats()
								+ " seats available (you already have "
								+ already + " in your cart).",
						ButtonType.OK)
						.showAndWait();
				return;
			}

			model.addToCart(selected, qty);

			// then immediately show the cart
			viewCartBtn.fire();
		});

		// 6) Log Out button handler
		logoutBtn.setOnAction(e -> {
			try {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/view/LoginView.fxml")
				);
				loader.setControllerFactory(type -> {
					if (type == controller.LoginController.class) {
						return new controller.LoginController(stage, model);
					}
					try {
						return type.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});
				Parent loginRoot = loader.load();
				stage.setScene(new Scene(loginRoot, 500, 300));
				stage.setTitle("Login");
				stage.show();
			} catch (IOException ex) {
				new Alert(Alert.AlertType.ERROR,
						"Failed to load login: " + ex.getMessage(),
						ButtonType.OK)
						.showAndWait();
			}
		});
	}
}
