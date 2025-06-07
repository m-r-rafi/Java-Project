

import java.io.IOException;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;

import controller.LoginController;
import dao.Database;
import model.Model;

public class Main extends Application {
	private Model model;

	@Override
	public void init() throws Exception {
		Database.init();

		model = new Model();
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			model.setup();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));

			// Customize controller instance
			LoginController loginController = new LoginController(primaryStage, model);

			loader.setControllerFactory(type -> {
				if (type == LoginController.class) {
					return new LoginController(primaryStage, model);
				}
				// default no-arg constructor for other controllers
				try {
					return type.getDeclaredConstructor().newInstance();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
			Parent root = loader.load();
			Scene scene = new Scene(root, 700, 500);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Login");
			primaryStage.setResizable(false);
			primaryStage.show();
		} catch (IOException | SQLException | RuntimeException e) {
			Scene scene = new Scene(new Label(e.getMessage()), 200, 100);
			primaryStage.setTitle("Error");
			primaryStage.setScene(scene);
			primaryStage.show();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
