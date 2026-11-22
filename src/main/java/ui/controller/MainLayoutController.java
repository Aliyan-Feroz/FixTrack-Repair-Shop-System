package ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import ui.FixTrackApp;
import ui.context.AppContext;

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
    @FXML private Label viewSubtitleLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnCustomers;
    @FXML private Button btnJobCards;
    @FXML private Button btnInventory;
    @FXML private Button btnTechnicians;
    @FXML private Button btnInvoices;
    @FXML private Button btnReports;

    @FXML private Button btnThemeToggle;
    @FXML private Button btnLogout;

    private final Map<String, Button> navButtons = new HashMap<>();

    @FXML
    public void initialize() {
        AppContext.getInstance().setMainLayoutController(this);

        lblUserName.setText(AppContext.getInstance().getCurrentUser());
        lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));

        navButtons.put("Dashboard", btnDashboard);
        navButtons.put("Customers", btnCustomers);
        navButtons.put("JobCards", btnJobCards);
        navButtons.put("Inventory", btnInventory);
        navButtons.put("Technicians", btnTechnicians);
        navButtons.put("Invoices", btnInvoices);
        navButtons.put("Reports", btnReports);

        updateThemeButtonText();

        // Default to Dashboard
        navigateTo("Dashboard");
    }

    public void navigateTo(String viewName) {
        String fxmlPath;
        String title;
        String subtitle;

        switch (viewName) {
            case "Customers":
                fxmlPath = "/fxml/Customers.fxml";
                title = "Customers Management";
                subtitle = "Manage registered customer profiles and service history";
                break;
            case "JobCards":
                fxmlPath = "/fxml/JobCards.fxml";
                title = "Job Cards & Repairs";
                subtitle = "Track device intake, diagnosis, active repairs, and completion";
                break;
            case "Inventory":
                fxmlPath = "/fxml/Inventory.fxml";
                title = "Inventory & Spare Parts";
                subtitle = "Track stock levels, replenish parts, and monitor unit pricing";
                break;
            case "Technicians":
                fxmlPath = "/fxml/Technicians.fxml";
                title = "Technicians Roster";
                subtitle = "Manage technical specialists, assignments, and active workloads";
                break;
            case "Invoices":
                fxmlPath = "/fxml/Invoices.fxml";
                title = "Invoicing & Billing";
                subtitle = "Generate customer receipts, apply discounts, and record payments";
                break;
            case "Reports":
                fxmlPath = "/fxml/Reports.fxml";
                title = "System Analytics & Reports";
                subtitle = "Operational performance, revenue metrics, and inventory health";
                break;
            case "Dashboard":
            default:
                viewName = "Dashboard";
                fxmlPath = "/fxml/Dashboard.fxml";
                title = "Dashboard Overview";
                subtitle = "Live operational status, revenue metrics, and workload summary";
                break;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node viewNode = loader.load();

            contentArea.getChildren().setAll(viewNode);

            viewTitleLabel.setText(title);
            viewSubtitleLabel.setText(subtitle);

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
        updateThemeButtonText();
    }

    private void updateThemeButtonText() {
        if (AppContext.getInstance().isDarkMode()) {
            btnThemeToggle.setText("☀️  Light Mode");
        } else {
            btnThemeToggle.setText("🌙  Dark Mode");
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText("Are you sure you want to log out?");
        alert.setContentText("Your changes have been saved to the database.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FixTrackApp.loadLoginView();
        }
    }
}

