package controller;

import java.io.IOException;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Model;
import model.User;

public class LoginController {
	@FXML private TextField      name;
	@FXML private PasswordField  password;
	@FXML private Label          message;
	@FXML private Button         login;
	@FXML private Button         signup;

	private final Model model;
	private final Stage stage;

	public LoginController(Stage stage, Model model) {
		this.stage = stage;
		this.model = model;
	}

	@FXML
	public void initialize() {
		login.setOnAction(evt -> {
			message.setText("");
			String u = name.getText().trim();
			String p = password.getText().trim();

			// 1) Admin login
			if ("admin".equals(u) && "Admin321".equals(p)) {
				try {
					FXMLLoader loader = new FXMLLoader(
							getClass().getResource("/view/AdminView.fxml")
					);
					loader.setControllerFactory(type -> {
						if (type == AdminController.class) {
							return new AdminController(stage, model);
						}
						try {
							return type.getDeclaredConstructor().newInstance();
						} catch (Exception ex) {
							throw new RuntimeException(ex);
						}
					});
					Parent root = loader.load();
					stage.setScene(new Scene(root, 700, 500));
					stage.setTitle("Admin Dashboard");
				} catch (IOException ex) {
					new Alert(Alert.AlertType.ERROR,
							"Failed to load Admin dashboard:\n" + ex.getMessage(),
							ButtonType.OK)
							.showAndWait();
				} finally {
					name.clear();
					password.clear();
				}
				return;
			}

			// 2) Normal user login
			if (u.isEmpty() || p.isEmpty()) {
				message.setText("Empty username or password");
				message.setTextFill(Color.RED);
			} else {
				try {
					User user = model.getUserDao().getUser(u, p);
					if (user != null) {
						model.setCurrentUser(user);

						FXMLLoader loader = new FXMLLoader(
								getClass().getResource("/view/HomeView.fxml")
						);
						loader.setControllerFactory(type -> {
							if (type == HomeController.class) {
								return new HomeController(stage, model);
							}
							try {
								return type.getDeclaredConstructor().newInstance();
							} catch (Exception ex) {
								throw new RuntimeException(ex);
							}
						});

						Parent root = loader.load();
						stage.setScene(new Scene(root, 700, 500));
						stage.setTitle("Dashboard");
					} else {
						message.setText("Wrong username or password");
						message.setTextFill(Color.RED);
					}
				} catch (SQLException | IOException ex) {
					message.setText("Error: " + ex.getMessage());
					message.setTextFill(Color.RED);
					ex.printStackTrace();
				} finally {
					name.clear();
					password.clear();
				}
			}
		});

		signup.setOnAction(evt -> {
			try {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/view/SignupView.fxml")
				);
				loader.setControllerFactory(type -> {
					if (type == SignupController.class) {
						return new SignupController(stage, model);
					}
					try {
						return type.getDeclaredConstructor().newInstance();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});
				Parent root = loader.load();
				stage.setScene(new Scene(root, 700, 500));
				stage.setTitle("Sign Up");
			} catch (IOException ex) {
				message.setText("Error: " + ex.getMessage());
				message.setTextFill(Color.RED);
				ex.printStackTrace();
			}
		});
	}

	public void showStage(Pane root) {
		Scene scene = new Scene(root, 700, 500);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Welcome");
		stage.show();
	}
}
