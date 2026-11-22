package ui;

import database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ui.context.AppContext;

import java.io.IOException;

/**
 * Main JavaFX Application class for FixTrack Repair Shop System.
 * Initializes the SQLite database inside init() and provides root scene management.
 */
public class FixTrackApp extends Application {

    private static final String APP_TITLE = "FixTrack - Repair Shop Management System";
    private static final String THEME_CSS = "/css/theme.css";

    @Override
    public void init() throws Exception {
        // Ensure DatabaseManager resolves the DB file and creates tables before any FXML is loaded
        DatabaseManager.initializeDatabase();
        AppContext.getInstance().seedDemoDataIfEmpty();
    }

    @Override
    public void start(Stage primaryStage) {
        AppContext.getInstance().setPrimaryStage(primaryStage);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(1120);
        primaryStage.setMinHeight(720);

        loadLoginView();
        primaryStage.show();
    }

    /**
     * Loads and displays the Login view.
     */
    public static void loadLoginView() {
        Stage stage = AppContext.getInstance().getPrimaryStage();
        try {
            FXMLLoader loader = new FXMLLoader(FixTrackApp.class.getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1150, 750);
            applyTheme(scene, root);

            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transitions from login to the main dashboard layout shell.
     */
    public static void loadMainView() {
        Stage stage = AppContext.getInstance().getPrimaryStage();
        try {
            FXMLLoader loader = new FXMLLoader(FixTrackApp.class.getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            applyTheme(scene, root);

            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attaches theme stylesheet and binds dark/light mode class toggles.
     */
    public static void applyTheme(Scene scene, Parent root) {
        scene.getStylesheets().clear();
        String css = FixTrackApp.class.getResource(THEME_CSS).toExternalForm();
        scene.getStylesheets().add(css);

        updateThemeClass(root, AppContext.getInstance().isDarkMode());

        AppContext.getInstance().darkModeProperty().addListener((obs, oldVal, isDark) -> {
            updateThemeClass(root, isDark);
        });
    }

    private static void updateThemeClass(Parent root, boolean isDark) {
        if (root == null) return;
        if (isDark) {
            root.getStyleClass().remove("light-theme");
        } else {
            if (!root.getStyleClass().contains("light-theme")) {
                root.getStyleClass().add("light-theme");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

