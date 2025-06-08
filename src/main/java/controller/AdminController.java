package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import model.Event;
import model.Model;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {
    @FXML private TreeTableView<Event> showsTree;
    @FXML private TreeTableColumn<Event, String> titleCol;
    @FXML private TreeTableColumn<Event, String> dateCol;
    @FXML private TreeTableColumn<Event, Void> actionCol;
    @FXML private Button logoutBtn;
    @FXML private Button btnAdd, btnEdit, btnDelete;

    private final Stage stage;
    private final Model model;

    public AdminController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    public void initialize() {
        // (1) Title column on parent nodes only
        titleCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

        // (2) Date–Venue on child nodes only
        dateCol.setCellValueFactory(cell -> {
            Event ev = cell.getValue().getValue();
            return new SimpleStringProperty(ev.getDate() + " – " + ev.getVenue());
        });
        dateCol.setCellFactory(col -> {
            TreeTableCell<Event,String> cell = new TreeTableCell<>();
            Text text = new Text();
            text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
            cell.itemProperty().addListener((obs, o, n) -> text.setText(n == null ? "" : n));
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            return cell;
        });

        // (3) Disable/Enable button on child nodes
        actionCol.setCellFactory(col -> new TreeTableCell<Event,Void>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(evt -> {
                    Event ev = getTreeTableRow().getItem();
                    boolean now = !ev.isDisabled();
                    model.setEventDisabled(ev, now);
                    updateItem(null, false);
                });
            }
            @Override
            protected void updateItem(Void unused, boolean empty) {
                super.updateItem(unused, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                TreeItem<Event> ti = getTreeTableRow().getTreeItem();
                if (ti == null || ti.getValue().getId() == 0) {
                    setGraphic(null);
                } else {
                    Event ev = ti.getValue();
                    btn.setText(ev.isDisabled() ? "Enable" : "Disable");
                    setGraphic(btn);
                }
            }
        });

        // (4) Build the grouped tree
        refreshTree();

        // (5) Hide expand arrow on parent rows
        showsTree.setRowFactory(tv -> {
            TreeTableRow<Event> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((obs, oldTI, newTI) -> {
                if (newTI != null && newTI.getValue().getId() == 0) {
                    row.setDisclosureNode(null);
                }
            });
            return row;
        });

        // (6) Add / Edit / Delete handlers
        btnAdd.setOnAction(e -> showEventDialog(null));
        btnEdit.setOnAction(e -> {
            var sel = showsTree.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getValue().getId() == 0) return;
            showEventDialog(sel.getValue());
        });
        btnDelete.setOnAction(e -> {
            var sel = showsTree.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getValue().getId() == 0) return;
            if (confirm("Delete “" + sel.getValue().getName() + "”?")) {
                model.deleteEvent(sel.getValue());
                refreshTree();
            }
        });

        // (7) Logout
        logoutBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/LoginView.fxml")
                );
                loader.setControllerFactory(type -> new LoginController(stage, model));
                Parent login = loader.load();
                stage.setScene(new Scene(login, 500, 300));
                stage.setTitle("Login");
                stage.show();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to load login:\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        });
    }

    private void showEventDialog(Event toEdit) {
        Dialog<Event> dlg = new Dialog<>();
        dlg.setTitle(toEdit == null ? "Add Event" : "Edit Event");
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField tfName = new TextField(),
                tfVenue = new TextField(),
                tfDate = new TextField(),
                tfPrice = new TextField(),
                tfCap = new TextField();

        if (toEdit != null) {
            tfName.setText(toEdit.getName());
            tfVenue.setText(toEdit.getVenue());
            tfDate.setText(toEdit.getDate());
            tfPrice.setText(Double.toString(toEdit.getPrice()));
            tfCap.setText(Integer.toString(toEdit.getRemainingSeats()));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Name:"), tfName);
        grid.addRow(1, new Label("Venue:"), tfVenue);
        grid.addRow(2, new Label("Date:"), tfDate);
        grid.addRow(3, new Label("Price:"), tfPrice);
        grid.addRow(4, new Label("Capacity:"), tfCap);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(btnType -> {
            if (btnType == ok) {
                try {
                    double price = Double.parseDouble(tfPrice.getText());
                    int cap     = Integer.parseInt(tfCap.getText());
                    return new Event(
                            toEdit == null ? 0 : toEdit.getId(),
                            tfName.getText(), tfDate.getText(),
                            tfVenue.getText(), price, cap
                    );
                } catch (NumberFormatException ex) {
                    showAlert("Price and Capacity must be numbers");
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(e -> {
            try {
                if (toEdit == null) model.addEvent(e);
                else                model.editEvent(e);
                refreshTree();
            } catch (IllegalArgumentException ex) {
                showAlert(ex.getMessage());
            }
        });
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void refreshTree() {
        List<Event> all = model.getAllEventsIncludingDisabled();
        Map<String,List<Event>> byTitle = all.stream()
                .collect(Collectors.groupingBy(Event::getName));

        TreeItem<Event> root = new TreeItem<>(new Event(0,"","", "",0,0,false));
        root.setExpanded(true);
        byTitle.forEach((title, evs) -> {
            TreeItem<Event> parent = new TreeItem<>(new Event(0,title,"","",0,0,false));
            parent.setExpanded(true);
            for (Event e : evs) {
                parent.getChildren().add(new TreeItem<>(e));
            }
            root.getChildren().add(parent);
        });

        showsTree.setRoot(root);
        showsTree.setShowRoot(false);
    }
}
