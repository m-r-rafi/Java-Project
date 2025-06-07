package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.Event;
import model.Model;
import model.ShowSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {
    @FXML private TableView<ShowSummary> showsTable;
    @FXML private TableColumn<ShowSummary, String> titleCol;
    @FXML private TableColumn<ShowSummary, String> optionsCol;
    @FXML private Button logoutBtn;

    private final Stage stage;
    private final Model model;

    public AdminController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(c -> c.getValue().titleProperty());
        optionsCol.setCellValueFactory(c -> c.getValue().optionsProperty());

        // cell factory that wraps text and honors newlines
        optionsCol.setCellFactory(col -> {
            TableCell<ShowSummary,String> cell = new TableCell<>();
            Text text = new Text();
            // bind wrapping width to the column’s width minus padding
            text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
            // whenever the item changes, update the text
            cell.itemProperty().addListener((obs, old, nw) -> {
                text.setText(nw == null ? "" : nw);
            });
            // show the Text node as the graphic
            cell.setGraphic(text);
            // let the row height expand to fit the text
            cell.setPrefHeight( TableView.USE_COMPUTED_SIZE );
            return cell;
        });

        // load & group as before…
        List<Event> events = model.getEvents();
        Map<String,List<Event>> byTitle = events.stream()
                .collect(Collectors.groupingBy(Event::getName));

        var rows = byTitle.entrySet().stream()
                .map(e -> new ShowSummary(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        showsTable.setItems(FXCollections.observableArrayList(rows));
        // 3) logout button → back to login
        logoutBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/LoginView.fxml")
                );
                loader.setControllerFactory(type -> {
                    if (type == controller.LoginController.class) {
                        return new controller.LoginController(stage, model);
                    }
                    throw new IllegalStateException("Unexpected controller: " + type);
                });
                Parent login = loader.load();
                stage.setScene(new Scene(login, 500, 300));
                stage.setTitle("Login");
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to log out:\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        });
    }
}
