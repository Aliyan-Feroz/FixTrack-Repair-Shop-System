package ui.controller;

import business.Invoice;
import business.JobCard;
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
    @FXML private TableColumn<JobCard, Double> colJobTotal;

    // Low Stock Alert Table
    @FXML private TableView<Part> tblLowStock;
    @FXML private TableColumn<Part, Integer> colPartId;
    @FXML private TableColumn<Part, String> colPartName;
    @FXML private TableColumn<Part, Integer> colPartStock;
    @FXML private TableColumn<Part, Double> colPartPrice;
    @FXML private TableColumn<Part, String> colPartStatus;

    @FXML
    public void initialize() {
        setupTables();
        loadDashboardData();
    }

    private void setupTables() {
        // Recent Jobs columns
        colJobId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getJobId()).asObject());
        colJobCustomer.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCustomer() != null ? data.getValue().getCustomer().getName() : "Unknown"));
        colJobDevice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeviceDetails()));
        colJobTech.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAssignedTech() != null ? data.getValue().getAssignedTech().getName() : "Unassigned"));
        colJobStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colJobTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().calculateTotal()).asObject());

        colJobStatus.setCellFactory(column -> new TableCell<JobCard, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.replace("_", " "));
                    badge.getStyleClass().add("badge");
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

        colJobTotal.setCellFactory(col -> new TableCell<JobCard, Double>() {
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

        // Low Stock columns
        colPartId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPartId()).asObject());
        colPartName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPartName()));
        colPartStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockQuantity()).asObject());
        colPartPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());
        colPartStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().checkLowStock() ? "LOW STOCK" : "OK"));

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

        colPartStatus.setCellFactory(col -> new TableCell<Part, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("LOW STOCK".equals(item)) {
                        badge.getStyleClass().add("badge-low-stock");
                    } else {
                        badge.getStyleClass().add("badge-in-stock");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    public void loadDashboardData() {
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

        lblOpenJobs.setText(String.valueOf(openJobs));
        lblCompletedJobs.setText(String.valueOf(completedJobs));
        lblPendingPayments.setText(String.valueOf(pendingPay));
        lblTotalRevenue.setText(String.format("$%.2f", totalRevenue));
        lblTotalCustomers.setText(String.valueOf(customerCount));
        lblActiveTechnicians.setText(String.valueOf(techCount));

        // Load recent jobs
        tblRecentJobs.setItems(FXCollections.observableArrayList(allJobs));

        // Load low stock items (<= 5 units)
        List<Part> lowStockParts = AppContext.getInstance().getInventoryDAO().getAllParts().stream()
                .filter(Part::checkLowStock)
                .collect(Collectors.toList());
        tblLowStock.setItems(FXCollections.observableArrayList(lowStockParts));
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

    @FXML
    private void handleQuickInvoices() {
        AppContext.getInstance().getMainLayoutController().navigateTo("Invoices");
    }
}

