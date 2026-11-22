package ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (ADMIN_USER.equalsIgnoreCase(username) && ADMIN_PASS.equals(password)) {
            errorLabel.setText("");
            AppContext.getInstance().setCurrentUser("Admin");
            FixTrackApp.loadMainView();
        } else {
            errorLabel.setText("Invalid credentials. Please enter admin / 1234.");
        }
    }

    @FXML
    private void handleQuickDemoFill() {
        usernameField.setText(ADMIN_USER);
        passwordField.setText(ADMIN_PASS);
        handleLogin();
    }
}

