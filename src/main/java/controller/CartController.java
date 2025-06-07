package controller;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import model.CartItem;
import model.Model;

public class CartController {
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem,String>  evtNameCol;
    @FXML private TableColumn<CartItem,String>  evtDateCol;
    @FXML private TableColumn<CartItem,String>  evtVenueCol;
    @FXML private TableColumn<CartItem,Number>  evtPriceCol;
    @FXML private TableColumn<CartItem,Integer> qtyCol;
    @FXML private TableColumn<CartItem,Number>  subtotalCol;

    @FXML private Label  msgLabel;
    @FXML private Button removeBtn;
    @FXML private Button updateBtn;
    @FXML private Button checkoutBtn;
    @FXML private Button backBtn;

    private final Stage stage;
    private final Model model;

    // map "Mon"→MONDAY, etc.
    private static final Map<String,DayOfWeek> DAY_MAP = new HashMap<>();
    static {
        DAY_MAP.put("Mon", DayOfWeek.MONDAY);
        DAY_MAP.put("Tue", DayOfWeek.TUESDAY);
        DAY_MAP.put("Wed", DayOfWeek.WEDNESDAY);
        DAY_MAP.put("Thu", DayOfWeek.THURSDAY);
        DAY_MAP.put("Fri", DayOfWeek.FRIDAY);
        DAY_MAP.put("Sat", DayOfWeek.SATURDAY);
        DAY_MAP.put("Sun", DayOfWeek.SUNDAY);
    }

    public CartController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    public void initialize() {
        // 1) bind non-editable columns
        evtNameCol .setCellValueFactory(c -> c.getValue().getEvent().nameProperty());
        evtDateCol .setCellValueFactory(c -> c.getValue().getEvent().dateProperty());
        evtVenueCol.setCellValueFactory(c -> c.getValue().getEvent().venueProperty());
        evtPriceCol.setCellValueFactory(c -> c.getValue().getEvent().priceProperty());

        // 2) make qty editable in-place
        qtyCol.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        cartTable.setEditable(true);
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        qtyCol.setOnEditCommit(evt -> {
            CartItem ci    = evt.getRowValue();
            int     newQty = evt.getNewValue();

            if (newQty < 0) {
                new Alert(Alert.AlertType.ERROR,
                        "Quantity must be ≥ 0", ButtonType.OK).showAndWait();
            } else if (newQty > ci.getEvent().getRemainingSeats()) {
                new Alert(Alert.AlertType.ERROR,
                        "Only " + ci.getEvent().getRemainingSeats() + " seats available.",
                        ButtonType.OK).showAndWait();
            } else {
                model.updateCart(ci.getEvent(), newQty);
            }
            refreshTable();
        });

        // 3) subtotal = price * qty
        subtotalCol.setCellValueFactory(c ->
                new ReadOnlyDoubleWrapper(
                        c.getValue().getEvent().getPrice() * c.getValue().getQuantity()
                )
        );

        // 4) wire up buttons
        refreshTable();
        removeBtn .setOnAction(this::onRemove);
        updateBtn .setOnAction(this::onUpdate);
        checkoutBtn.setOnAction(this::onCheckout);
        backBtn   .setOnAction(this::onBack);
    }

    private void refreshTable() {
        cartTable.setItems(FXCollections.observableArrayList(model.getCart()));
        msgLabel.setText("");
    }

    private void onRemove(ActionEvent e) {
        CartItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            model.removeFromCart(sel.getEvent());
            refreshTable();
        }
    }

    private void onUpdate(ActionEvent e) {
        CartItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Please select a row first.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        TextInputDialog dlg = new TextInputDialog(String.valueOf(sel.getQuantity()));
        dlg.setTitle("Update Quantity");
        dlg.setHeaderText("Event: " + sel.getEvent().getName());
        dlg.setContentText("Enter new quantity:");

        Optional<String> ans = dlg.showAndWait();
        if (ans.isEmpty()) return;

        int newQty;
        try {
            newQty = Integer.parseInt(ans.get().trim());
            if (newQty < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Please enter a valid non‐negative integer.",
                    ButtonType.OK).showAndWait();
            return;
        }

        if (newQty > sel.getEvent().getRemainingSeats()) {
            new Alert(Alert.AlertType.ERROR,
                    "Only " + sel.getEvent().getRemainingSeats() + " seats available.",
                    ButtonType.OK).showAndWait();
            return;
        }

        model.updateCart(sel.getEvent(), newQty);
        refreshTable();
    }

    private void onCheckout(ActionEvent e) {
        var cart = model.getCart();
        if (cart.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Your cart is empty.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        // 1) Confirm total
        double total = cart.stream()
                .mapToDouble(ci -> ci.getEvent().getPrice() * ci.getQuantity())
                .sum();
        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Your total is $"+total+".\nProceed to payment?",
                ButtonType.OK, ButtonType.CANCEL)
                .showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.OK) return;

        // 2) Six-digit code
        TextInputDialog codeDlg = new TextInputDialog();
        codeDlg.setTitle("Payment");
        codeDlg.setHeaderText("Enter 6-digit confirmation code:");
        Optional<String> code = codeDlg.showAndWait();
        if (code.isEmpty()) return;
        if (!code.get().matches("\\d{6}")) {
            new Alert(Alert.AlertType.ERROR,
                    "Invalid code; must be exactly 6 digits.",
                    ButtonType.OK).showAndWait();
            return;
        }

        // 3) Day-of-week check
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        for (CartItem ci : cart) {
            String d = ci.getEvent().getDate();
            DayOfWeek evDay = DAY_MAP.get(d);
            if (evDay == null || evDay.getValue() < today.getValue()) {
                new Alert(Alert.AlertType.ERROR,
                        "Cannot book event on " + d +
                                "; only " + today.name() + " onward.",
                        ButtonType.OK).showAndWait();
                return;
            }
        }

        // 4) All good → finalize
        try {
            model.checkout();
            new Alert(Alert.AlertType.INFORMATION,
                    "Payment successful! You paid $" + total,
                    ButtonType.OK).showAndWait();
            refreshTable();
        } catch (IllegalStateException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Not enough seats for one or more items.",
                    ButtonType.OK).showAndWait();
        }
    }

    private void onBack(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/HomeView.fxml")
            );
            loader.setControllerFactory(type -> {
                if (type == controller.HomeController.class) {
                    return new controller.HomeController(stage, model);
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            Parent home = loader.load();
            stage.setScene(new Scene(home, 700, 500));
            stage.setTitle("Dashboard");
        } catch (IOException ex) {
            msgLabel.setText("Error loading Dashboard");
            msgLabel.setStyle("-fx-text-fill: red;");
            ex.printStackTrace();
        }
    }
}
