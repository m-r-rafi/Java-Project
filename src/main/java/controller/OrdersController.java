package controller;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Order;
import model.Model;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class OrdersController {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String>  orderNumCol;
    @FXML private TableColumn<Order, String>  timestampCol;
    @FXML private TableColumn<Order,String>   eventCol;
    @FXML private TableColumn<Order,Number>   seatsCol;
    @FXML private TableColumn<Order, Number>  totalCol;
    @FXML private Button backBtn;
    @FXML private Button exportBtn;

    private final Stage stage;
    private final Model model;

    public OrdersController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    protected void initialize() {
        // bind columns to Order properties
        orderNumCol .setCellValueFactory(c -> c.getValue().orderNumberProperty());
        timestampCol.setCellValueFactory(c -> c.getValue().timestampProperty());
        eventCol.setCellValueFactory(c -> {
            String allNames = c.getValue().getItems().stream()
                    .map(ci -> ci.getEvent().getName())
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(allNames);
        });
        seatsCol.setCellValueFactory(c -> {
            int totalSeats = c.getValue().getItems().stream()
                    .mapToInt(ci -> ci.getQuantity())
                    .sum();
            return new ReadOnlyIntegerWrapper(totalSeats);
        });

        totalCol    .setCellValueFactory(c -> c.getValue().totalProperty());

        // load all orders (reverse‐chronological)
        try {
            List<Order> orders = model.getOrders();
            ordersTable.setItems(FXCollections.observableArrayList(orders));
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load order history:\n" + ex.getMessage(),
                    ButtonType.OK)
                    .showAndWait();
        }

        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            try {
                model.exportOrders(file);
                new Alert(Alert.AlertType.INFORMATION,
                        "Orders exported to:\n" + file.getAbsolutePath(),
                        ButtonType.OK).showAndWait();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export orders:\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        });

        // back to dashboard
        backBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/HomeView.fxml")
                );
                loader.setControllerFactory(type -> {
                    if (type == HomeController.class) {
                        return new HomeController(stage, model);
                    }
                    try {
                        return type.getDeclaredConstructor().newInstance();
                    } catch (Exception ex2) {
                        throw new RuntimeException(ex2);
                    }
                });
                Parent root = loader.load();
                stage.setScene(new Scene(root, 700, 500));
                stage.setTitle("Dashboard");
            } catch (IOException ex2) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to load dashboard:\n" + ex2.getMessage(),
                        ButtonType.OK)
                        .showAndWait();
            }
        });
    }
}
