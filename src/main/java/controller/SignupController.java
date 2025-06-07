package controller;

import java.io.IOException;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Model;
import model.User;

public class SignupController {
	@FXML private TextField username;
	@FXML private PasswordField password;
	@FXML private TextField preferredName;
	@FXML private Button createUser;
	@FXML private Button back;
	@FXML private Label status;

	private final Stage stage;
	private final Model model;

	public SignupController(Stage stage, Model model) {
		this.stage = stage;
		this.model = model;
	}

	@FXML
	public void initialize() {
		createUser.setOnAction(evt -> {
			status.setText("");
			if (username.getText().isEmpty()
					|| password.getText().isEmpty()
					|| preferredName.getText().isEmpty()) {
				status.setText("All fields are required");
				status.setTextFill(Color.RED);
				return;
			}

			try {
				User user = model.getUserDao()
						.createUser(
								username.getText(),
								password.getText(),
								preferredName.getText()
						);
				status.setText("Account created for " + user.getPreferredName());
				status.setTextFill(Color.GREEN);

				// navigate back to LoginView via controller factory
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/view/LoginView.fxml")
				);
				loader.setControllerFactory(type -> {
					if (type == LoginController.class) {
						return new LoginController(stage, model);
					}
					try {
						return type.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});

				// load and swap scene
				Pane loginRoot = loader.load();
				Scene scene = new Scene(loginRoot, 700, 500);
				stage.setScene(scene);
				stage.setTitle("Login");
				stage.show();

			} catch (SQLException | IOException ex) {
				status.setText("Error: " + ex.getMessage());
				status.setTextFill(Color.RED);
				ex.printStackTrace();
			}
		});

		back.setOnAction(evt -> {
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
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});

				Pane loginRoot = loader.load();
				Scene scene = new Scene(loginRoot, 700, 500);
				stage.setScene(scene);
				stage.setTitle("Login");
				stage.show();

			} catch (IOException ex) {
				status.setText("Error: " + ex.getMessage());
				status.setTextFill(Color.RED);
				ex.printStackTrace();
			}
		});
	}


	public void showStage(Pane root) {
		Scene scene = new Scene(root, 700, 500);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Sign Up");
		stage.show();
	}
}
