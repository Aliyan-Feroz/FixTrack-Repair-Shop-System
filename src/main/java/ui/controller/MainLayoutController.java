package ui.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import ui.FixTrackApp;
import ui.context.AppContext;
import ui.util.SvgIcons;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MainLayoutController {

    @FXML private BorderPane mainRoot;
    @FXML private StackPane contentArea;

    @FXML private Label lblUserName;
    @FXML private Label lblCurrentDate;
    @FXML private Label viewTitleLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnCustomers;
    @FXML private Button btnJobCards;
    @FXML private Button btnInventory;
    @FXML private Button btnTechnicians;
    @FXML private Button btnInvoices;
    @FXML private Button btnReports;

    @FXML private Button btnThemeToggle;
    @FXML private Button btnLogout;

    private static class CachedView {
        final Node root;
        final Object controller;

        CachedView(Node root, Object controller) {
            this.root = root;
            this.controller = controller;
        }
    }

    private final Map<String, Button> navButtons = new HashMap<>();
    private final Map<String, CachedView> viewCache = new HashMap<>();

    @FXML
    public void initialize() {
        AppContext.getInstance().setMainLayoutController(this);

        lblUserName.setText(AppContext.getInstance().getCurrentUser());
        lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")));

        navButtons.put("Dashboard", btnDashboard);
        navButtons.put("Customers", btnCustomers);
        navButtons.put("JobCards", btnJobCards);
        navButtons.put("Inventory", btnInventory);
        navButtons.put("Technicians", btnTechnicians);
        navButtons.put("Invoices", btnInvoices);
        navButtons.put("Reports", btnReports);

        updateThemeButton();

        // Default to Dashboard
        navigateTo("Dashboard");
    }

    public void navigateTo(String viewName) {
        String fxmlPath;
        String title;

        switch (viewName) {
            case "Customers":
                fxmlPath = "/fxml/Customers.fxml";
                title = "Customers";
                break;
            case "JobCards":
                fxmlPath = "/fxml/JobCards.fxml";
                title = "Job Cards";
                break;
            case "Inventory":
                fxmlPath = "/fxml/Inventory.fxml";
                title = "Inventory";
                break;
            case "Technicians":
                fxmlPath = "/fxml/Technicians.fxml";
                title = "Technicians";
                break;
            case "Invoices":
                fxmlPath = "/fxml/Invoices.fxml";
                title = "Invoicing";
                break;
            case "Reports":
                fxmlPath = "/fxml/Reports.fxml";
                title = "Reports";
                break;
            case "Dashboard":
            default:
                viewName = "Dashboard";
                fxmlPath = "/fxml/Dashboard.fxml";
                title = "Dashboard";
                break;
        }

        try {
            CachedView cached = viewCache.get(viewName);
            if (cached == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Node viewNode = loader.load();
                Object controller = loader.getController();
                cached = new CachedView(viewNode, controller);
                viewCache.put(viewName, cached);
            } else {
                refreshViewData(cached.controller);
            }

            contentArea.getChildren().setAll(cached.root);

            // Snappy <= 50ms transition (40ms subtle fade)
            FadeTransition fade = new FadeTransition(Duration.millis(40), cached.root);
            fade.setFromValue(0.9);
            fade.setToValue(1.0);
            fade.play();

            viewTitleLabel.setText(title);

            // Highlight active button
            for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(viewName)) {
                    if (!entry.getValue().getStyleClass().contains("active-nav")) {
                        entry.getValue().getStyleClass().add("active-nav");
                    }
                } else {
                    entry.getValue().getStyleClass().remove("active-nav");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshViewData(Object controller) {
        if (controller instanceof DashboardController c) {
            c.loadDashboardData();
        } else if (controller instanceof CustomersController c) {
            c.loadCustomers();
        } else if (controller instanceof JobCardsController c) {
            c.loadJobCards();
        } else if (controller instanceof InventoryController c) {
            c.loadInventory();
        } else if (controller instanceof TechniciansController c) {
            c.loadTechnicians();
        } else if (controller instanceof InvoicesController c) {
            c.loadInvoices();
        } else if (controller instanceof ReportsController c) {
            c.loadReportsData();
        }
    }

    @FXML private void handleNavDashboard()   { navigateTo("Dashboard"); }
    @FXML private void handleNavCustomers()   { navigateTo("Customers"); }
    @FXML private void handleNavJobCards()    { navigateTo("JobCards"); }
    @FXML private void handleNavInventory()   { navigateTo("Inventory"); }
    @FXML private void handleNavTechnicians() { navigateTo("Technicians"); }
    @FXML private void handleNavInvoices()    { navigateTo("Invoices"); }
    @FXML private void handleNavReports()     { navigateTo("Reports"); }

    @FXML
    private void handleThemeToggle() {
        boolean currentlyDark = AppContext.getInstance().isDarkMode();
        AppContext.getInstance().setDarkMode(!currentlyDark);
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (btnThemeToggle == null) return;
        if (AppContext.getInstance().isDarkMode()) {
            btnThemeToggle.setText("Light Mode");
            try {
                SVGPath sun = SvgIcons.icon(SvgIcons.SUN);
                btnThemeToggle.setGraphic(sun);
            } catch (Throwable t) {
                SVGPath fallbackSun = new SVGPath();
                fallbackSun.setContent("M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 M18.36 18.36l1.42 1.42 M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42 M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z");
                fallbackSun.getStyleClass().add("icon-svg");
                btnThemeToggle.setGraphic(fallbackSun);
            }
        } else {
            btnThemeToggle.setText("Dark Mode");
            try {
                SVGPath moon = SvgIcons.icon(SvgIcons.MOON);
                btnThemeToggle.setGraphic(moon);
            } catch (Throwable t) {
                SVGPath fallbackMoon = new SVGPath();
                fallbackMoon.setContent("M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z");
                fallbackMoon.getStyleClass().add("icon-svg");
                btnThemeToggle.setGraphic(fallbackMoon);
            }
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Sign out of current session?");
        alert.setContentText("Active changes are persisted in the database.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FixTrackApp.loadLoginView();
        }
    }
}
