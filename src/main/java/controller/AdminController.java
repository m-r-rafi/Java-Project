package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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

    private final Stage stage;
    private final Model model;

    public AdminController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    public void initialize() {
        // 1) Parent‐only “Show Title”
        titleCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

        // 2) Child‐only “Date – Venue”
        dateCol.setCellValueFactory(cell -> {
            Event ev = cell.getValue().getValue();
            return new SimpleStringProperty(ev.getDate() + " – " + ev.getVenue());
        });
        dateCol.setCellFactory(col -> {
            TreeTableCell<Event,String> cell = new TreeTableCell<>();
            Text text = new Text();
            text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
            cell.itemProperty().addListener((obs, old, nw) -> text.setText(nw == null ? "" : nw));
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            return cell;
        });

        // 3) Child‐only “Disable/Enable” button
        actionCol.setCellFactory(col -> new TreeTableCell<Event,Void>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(evt -> {
                    Event ev = getTreeTableRow().getItem();
                    boolean now = !ev.isDisabled();
                    model.setEventDisabled(ev, now);
                    // next redraw via updateItem() will re‐label
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
                    // parent or dummy → no button
                    setGraphic(null);
                } else {
                    Event ev = ti.getValue();
                    btn.setText(ev.isDisabled() ? "Enable" : "Disable");
                    setGraphic(btn);
                }
            }
        });

        // 4) Build the grouped tree
        List<Event> all = model.getAllEventsIncludingDisabled();
        Map<String, List<Event>> byTitle = all.stream()
                .collect(Collectors.groupingBy(Event::getName));

        TreeItem<Event> root = new TreeItem<>(new Event(0, "", "", "", 0, 0, false));
        root.setExpanded(true);
        byTitle.forEach((title, evs) -> {
            TreeItem<Event> parent = new TreeItem<>(new Event(0, title, "", "", 0, 0, false));
            parent.setExpanded(true);
            for (Event e : evs) {
                parent.getChildren().add(new TreeItem<>(e));
            }
            root.getChildren().add(parent);
        });

        showsTree.setRoot(root);
        showsTree.setShowRoot(false);

        // 5) Hide the little expand/collapse arrow on every parent row
        showsTree.setRowFactory(tv -> {
            TreeTableRow<Event> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((obs, oldTI, newTI) -> {
                if (newTI != null && newTI.getValue().getId() == 0) {
                    // this is one of our dummy “parent” nodes → remove its disclosure node
                    row.setDisclosureNode(null);
                }
            });
            return row;
        });

        // 6) Log out
        logoutBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
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
                        ButtonType.OK).showAndWait();
            }
        });
    }
}
