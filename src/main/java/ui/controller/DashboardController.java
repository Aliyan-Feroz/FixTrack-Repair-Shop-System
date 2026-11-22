package ui.controller;

import business.Invoice;
import business.JobCard;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Part;
import ui.context.AppContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblOpenJobs;
    @FXML private Label lblCompletedJobs;
    @FXML private Label lblPendingPayments;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTotalCustomers;
    @FXML private Label lblActiveTechnicians;

    // Recent Jobs Table
    @FXML private TableView<JobCard> tblRecentJobs;
    @FXML private TableColumn<JobCard, Integer> colJobId;
    @FXML private TableColumn<JobCard, String> colJobCustomer;
    @FXML private TableColumn<JobCard, String> colJobDevice;
    @FXML private TableColumn<JobCard, String> colJobTech;
    @FXML private TableColumn<JobCard, String> colJobStatus;

    // Low Stock Alert Table
    @FXML private TableView<Part> tblLowStock;
    @FXML private TableColumn<Part, String> colPartName;
    @FXML private TableColumn<Part, Integer> colPartStock;
    @FXML private TableColumn<Part, Double> colPartPrice;

    private static class DashboardSnapshot {
        final long openJobs;
        final long completedJobs;
        final long pendingPay;
        final double totalRevenue;
        final int customerCount;
        final int techCount;
        final List<JobCard> recentJobs;
        final List<Part> lowStockParts;

        DashboardSnapshot(long openJobs, long completedJobs, long pendingPay, double totalRevenue,
                          int customerCount, int techCount, List<JobCard> recentJobs, List<Part> lowStockParts) {
            this.openJobs = openJobs;
            this.completedJobs = completedJobs;
            this.pendingPay = pendingPay;
            this.totalRevenue = totalRevenue;
            this.customerCount = customerCount;
            this.techCount = techCount;
            this.recentJobs = recentJobs;
            this.lowStockParts = lowStockParts;
        }
    }

    @FXML
    public void initialize() {
        setupTables();
        loadDashboardData();
    }

    private void setupTables() {
        colJobId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getJobId()).asObject());
        colJobCustomer.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCustomer() != null ? data.getValue().getCustomer().getName() : "Unknown"));
        colJobDevice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeviceDetails()));
        colJobTech.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAssignedTech() != null ? data.getValue().getAssignedTech().getName() : "Unassigned"));
        colJobStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        colJobStatus.setCellFactory(column -> new TableCell<JobCard, String>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    badge.setText(item.replace("_", " "));
                    badge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                    switch (item) {
                        case "INTAKE": badge.getStyleClass().add("badge-intake"); break;
                        case "DIAGNOSIS": badge.getStyleClass().add("badge-diagnosis"); break;
                        case "AWAITING_APPROVAL": badge.getStyleClass().add("badge-awaiting"); break;
                        case "REPAIRING": badge.getStyleClass().add("badge-repairing"); break;
                        case "QUALITY_CHECK": badge.getStyleClass().add("badge-quality"); break;
                        case "READY": badge.getStyleClass().add("badge-ready"); break;
                        case "COMPLETED": badge.getStyleClass().add("badge-completed"); break;
                        case "CANCELLED": badge.getStyleClass().add("badge-cancelled"); break;
                        default: badge.getStyleClass().add("badge-intake"); break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Low Stock Table
        colPartName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPartName()));
        colPartStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockQuantity()).asObject());
        colPartPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());

        colPartPrice.setCellFactory(col -> new TableCell<Part, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                }
            }
        });
    }

    public void loadDashboardData() {
        CompletableFuture.supplyAsync(() -> {
            List<JobCard> allJobs = AppContext.getInstance().getJobCardDAO().getAllJobCards();
            List<Invoice> allInvoices = AppContext.getInstance().getInvoiceDAO().getAllInvoices();

            long openJobs = allJobs.stream().filter(j -> j.getStatus() != JobCard.RepairStatus.COMPLETED &&
                    j.getStatus() != JobCard.RepairStatus.CANCELLED).count();
            long completedJobs = allJobs.stream().filter(j -> j.getStatus() == JobCard.RepairStatus.COMPLETED).count();
            long pendingPay = allInvoices.stream().filter(i -> i.getPaymentStatus() == Invoice.PaymentStatus.PENDING).count();
            double totalRevenue = allInvoices.stream()
                    .filter(i -> i.getPaymentStatus() != Invoice.PaymentStatus.PENDING)
                    .mapToDouble(Invoice::getGrandTotal).sum();

            int customerCount = AppContext.getInstance().getCustomerDAO().getAllCustomers().size();
            int techCount = AppContext.getInstance().getTechnicianDAO().getAllTechnicians().size();

            List<Part> lowStockParts = AppContext.getInstance().getInventoryDAO().getAllParts().stream()
                    .filter(Part::checkLowStock)
                    .collect(Collectors.toList());

            return new DashboardSnapshot(openJobs, completedJobs, pendingPay, totalRevenue,
                    customerCount, techCount, allJobs, lowStockParts);
        }).thenAccept(snap -> Platform.runLater(() -> {
            lblOpenJobs.setText(String.valueOf(snap.openJobs));
            lblCompletedJobs.setText(String.valueOf(snap.completedJobs));
            lblPendingPayments.setText(String.valueOf(snap.pendingPay));
            lblTotalRevenue.setText(String.format("$%.2f", snap.totalRevenue));
            lblTotalCustomers.setText(String.valueOf(snap.customerCount));
            lblActiveTechnicians.setText(String.valueOf(snap.techCount));

            tblRecentJobs.setItems(FXCollections.observableArrayList(snap.recentJobs));
            tblLowStock.setItems(FXCollections.observableArrayList(snap.lowStockParts));
        }));
    }

    @FXML
    private void handleQuickNewJob() {
        AppContext.getInstance().getMainLayoutController().navigateTo("JobCards");
    }

    @FXML
    private void handleQuickNewCustomer() {
        AppContext.getInstance().getMainLayoutController().navigateTo("Customers");
    }

    @FXML
    private void handleQuickRestock() {
        AppContext.getInstance().getMainLayoutController().navigateTo("Inventory");
    }
}
