package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
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
    @FXML private TableColumn<ShowSummary,String> titleCol;
    @FXML private TableColumn<ShowSummary,String> optionsCol;
    @FXML private Button logoutBtn;

    private final Stage stage;
    private final Model model;

    public AdminController(Stage stage, Model model) {
        this.stage = stage;
        this.model = model;
    }

    @FXML
    public void initialize() {
        // bind columns
        titleCol  .setCellValueFactory(c -> c.getValue().titleProperty());
        optionsCol.setCellValueFactory(c -> c.getValue().optionsProperty());

        // load & group events by title
        List<Event> events = model.getEvents();
        Map<String,List<Event>> byTitle = events.stream()
                .collect(Collectors.groupingBy(Event::getName));

        List<ShowSummary> rows = byTitle.entrySet().stream()
                .map(e -> new ShowSummary(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        showsTable.setItems(FXCollections.observableArrayList(rows));

        // logout → back to login
        logoutBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/LoginView.fxml")
                );
                loader.setControllerFactory(type -> {
                    if (type == LoginController.class) {
                        return new LoginController(stage, model);
                    }
                    try {
                        return type.getDeclaredConstructor().newInstance();
                    } catch (Exception ex2) {
                        throw new RuntimeException(ex2);
                    }
                });
                Parent login = loader.load();
                stage.setScene(new Scene(login, 500, 300));
                stage.setTitle("Login");
                stage.show();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to log out:\n" + ex.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        });
    }
}
