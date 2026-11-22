package ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ui.FixTrackApp;
import ui.context.AppContext;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button btnLogin;

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (ADMIN_USER.equalsIgnoreCase(username) && ADMIN_PASS.equals(password)) {
            errorLabel.setVisible(false);
            AppContext.getInstance().setCurrentUser("Admin");
            try {
                Stage stage = (Stage) btnLogin.getScene().getWindow();
                if (stage == null) {
                    stage = AppContext.getInstance().getPrimaryStage();
                }
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
                Parent root = loader.load();

                Scene scene = new Scene(root, 1280, 800);
                FixTrackApp.applyTheme(scene, root);

                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
                errorLabel.setText("Login transition failed: " + e.getMessage());
                errorLabel.setVisible(true);
            }
        } else {
            errorLabel.setText("Invalid username or password");
            errorLabel.setVisible(true);
        }
    }

    @FXML
    private void handleQuickDemoFill() {
        usernameField.setText(ADMIN_USER);
        passwordField.setText(ADMIN_PASS);
        handleLogin();
    }
}
