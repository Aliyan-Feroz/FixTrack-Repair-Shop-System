package ui.controller;

import business.Invoice;
import business.JobCard;
import dao.InventoryDAO;
import dao.InvoiceDAO;
import dao.JobCardDAO;
import dao.TechnicianDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import model.Part;
import model.Technician;
import ui.context.AppContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReportsController {

    // Financial KPI Cards
    @FXML private Label lblRealizedRevenue;
    @FXML private Label lblPendingReceivables;
    @FXML private Label lblTotalInvoiced;
    @FXML private Label lblDiscountsGiven;
    @FXML private Label lblAverageTicket;

    // Workflow State Counters
    @FXML private Label lblCountIntake;
    @FXML private Label lblCountDiag;
    @FXML private Label lblCountApproval;
    @FXML private Label lblCountRepair;
    @FXML private Label lblCountQuality;
    @FXML private Label lblCountReady;
    @FXML private Label lblCountCompleted;
    @FXML private Label lblCountCancelled;

    // Donut Gauge Total
    @FXML private Label lblActiveRepairsTotal;

    // Technician Productivity Table
    @FXML private TableView<TechnicianStats> tblTechStats;
    @FXML private TableColumn<TechnicianStats, String> colTechName;
    @FXML private TableColumn<TechnicianStats, String> colTechSpecialty;
    @FXML private TableColumn<TechnicianStats, Integer> colTechActiveJobs;
    @FXML private TableColumn<TechnicianStats, Integer> colTechCompletedJobs;

    // Inventory Health
    @FXML private Label lblTotalSKUs;
    @FXML private Label lblTotalInventoryValue;
    @FXML private Label lblLowStockAlertCount;

    public static class TechnicianStats {
        private final String name;
        private final String specialty;
        private final int activeJobs;
        private final int completedJobs;

        public TechnicianStats(String name, String specialty, int activeJobs, int completedJobs) {
            this.name = name;
            this.specialty = specialty;
            this.activeJobs = activeJobs;
            this.completedJobs = completedJobs;
        }

        public String getName() { return name; }
        public String getSpecialty() { return specialty; }
        public int getActiveJobs() { return activeJobs; }
        public int getCompletedJobs() { return completedJobs; }
    }

    private static class ReportsSnapshot {
        final double realizedRevenue;
        final double pendingReceivables;
        final double totalInvoiced;
        final double totalDiscounts;
        final double avgTicket;
        final int intake, diag, approval, repair, quality, ready, completed, cancelled;
        final int activeRepairs;
        final List<TechnicianStats> techStats;
        final int totalSKUs;
        final double totalInventoryValue;
        final long lowStockCount;

        ReportsSnapshot(double realizedRevenue, double pendingReceivables, double totalInvoiced,
                        double totalDiscounts, double avgTicket,
                        int intake, int diag, int approval, int repair, int quality, int ready, int completed, int cancelled,
                        int activeRepairs, List<TechnicianStats> techStats,
                        int totalSKUs, double totalInventoryValue, long lowStockCount) {
            this.realizedRevenue = realizedRevenue;
            this.pendingReceivables = pendingReceivables;
            this.totalInvoiced = totalInvoiced;
            this.totalDiscounts = totalDiscounts;
            this.avgTicket = avgTicket;
            this.intake = intake;
            this.diag = diag;
            this.approval = approval;
            this.repair = repair;
            this.quality = quality;
            this.ready = ready;
            this.completed = completed;
            this.cancelled = cancelled;
            this.activeRepairs = activeRepairs;
            this.techStats = techStats;
            this.totalSKUs = totalSKUs;
            this.totalInventoryValue = totalInventoryValue;
            this.lowStockCount = lowStockCount;
        }
    }

    @FXML
    public void initialize() {
        setupTechTable();
        loadReportsData();
    }

    private void setupTechTable() {
        colTechName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colTechSpecialty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialty()));
        colTechActiveJobs.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getActiveJobs()).asObject());
        colTechCompletedJobs.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCompletedJobs()).asObject());

        // Capsule progress styling for Active Jobs
        colTechActiveJobs.setCellFactory(col -> new TableCell<TechnicianStats, Integer>() {
            private final Label capsule = new Label();
            {
                capsule.getStyleClass().add("tech-active-capsule");
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item > 0) {
                    capsule.setText(String.valueOf(item));
                    setGraphic(capsule);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText("0");
                }
            }
        });

        // Compl column with emerald indicator dot
        colTechCompletedJobs.setCellFactory(col -> new TableCell<TechnicianStats, Integer>() {
            private final HBox container = new HBox(6);
            private final Label lblCount = new Label();
            private final Circle dot = new Circle(3.5, Color.web("#10B981"));
            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(lblCount, dot);
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item > 0) {
                    lblCount.setText(String.valueOf(item));
                    setGraphic(container);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText("0");
                }
            }
        });
    }

    public void loadReportsData() {
        CompletableFuture.supplyAsync(() -> {
            JobCardDAO jobDAO = AppContext.getInstance().getJobCardDAO();
            InvoiceDAO invDAO = AppContext.getInstance().getInvoiceDAO();
            TechnicianDAO techDAO = AppContext.getInstance().getTechnicianDAO();
            InventoryDAO invenDAO = AppContext.getInstance().getInventoryDAO();

            List<JobCard> allJobs = jobDAO.getAllJobCards();
            List<Invoice> allInvoices = invDAO.getAllInvoices();
            List<Technician> allTechs = techDAO.getAllTechnicians();
            List<Part> allParts = invenDAO.getAllParts();

            // 1. Financial Metrics
            double realizedRevenue = allInvoices.stream()
                    .filter(i -> i.getPaymentStatus() != Invoice.PaymentStatus.PENDING)
                    .mapToDouble(Invoice::getGrandTotal).sum();

            double pendingReceivables = allInvoices.stream()
                    .filter(i -> i.getPaymentStatus() == Invoice.PaymentStatus.PENDING)
                    .mapToDouble(Invoice::getGrandTotal).sum();

            double totalDiscounts = allInvoices.stream()
                    .mapToDouble(Invoice::getDiscount).sum();

            double totalInvoiced = realizedRevenue + pendingReceivables;
            double avgTicket = allInvoices.isEmpty() ? 0.0 : totalInvoiced / allInvoices.size();

            // 2. Job Status Counts
            int intake = 0, diag = 0, approval = 0, repair = 0, quality = 0, ready = 0, completed = 0, cancelled = 0;
            for (JobCard jc : allJobs) {
                switch (jc.getStatus()) {
                    case INTAKE: intake++; break;
                    case DIAGNOSIS: diag++; break;
                    case AWAITING_APPROVAL: approval++; break;
                    case REPAIRING: repair++; break;
                    case QUALITY_CHECK: quality++; break;
                    case READY: ready++; break;
                    case COMPLETED: completed++; break;
                    case CANCELLED: cancelled++; break;
                }
            }
            int activeRepairs = intake + diag + approval + repair + quality + ready;

            // 3. Technician Stats Table
            List<TechnicianStats> statsList = allTechs.stream().map(tech -> {
                int active = tech.getActiveJobsCount();
                int comp = (int) allJobs.stream()
                        .filter(j -> j.getAssignedTech() != null &&
                                j.getAssignedTech().getTechId() == tech.getTechId() &&
                                j.getStatus() == JobCard.RepairStatus.COMPLETED)
                        .count();
                return new TechnicianStats(tech.getName(), tech.getSpecialty(), active, comp);
            }).toList();

            // 4. Inventory Health
            int totalSKUs = allParts.size();
            double totalInventoryValue = allParts.stream()
                    .mapToDouble(p -> p.getStockQuantity() * p.getPrice()).sum();
            long lowStockCount = allParts.stream().filter(Part::checkLowStock).count();

            return new ReportsSnapshot(realizedRevenue, pendingReceivables, totalInvoiced, totalDiscounts, avgTicket,
                    intake, diag, approval, repair, quality, ready, completed, cancelled,
                    activeRepairs, statsList, totalSKUs, totalInventoryValue, lowStockCount);
        }).thenAccept(snap -> Platform.runLater(() -> {
            lblRealizedRevenue.setText(String.format("$%.2f", snap.realizedRevenue));
            lblPendingReceivables.setText(String.format("$%.2f", snap.pendingReceivables));
            lblTotalInvoiced.setText(String.format("$%.2f", snap.totalInvoiced));
            lblDiscountsGiven.setText(String.format("$%.2f", snap.totalDiscounts));
            lblAverageTicket.setText(String.format("$%.2f", snap.avgTicket));

            lblCountIntake.setText(String.valueOf(snap.intake));
            lblCountDiag.setText(String.valueOf(snap.diag));
            lblCountApproval.setText(String.valueOf(snap.approval));
            lblCountRepair.setText(String.valueOf(snap.repair));
            lblCountQuality.setText(String.valueOf(snap.quality));
            lblCountReady.setText(String.valueOf(snap.ready));
            lblCountCompleted.setText(String.valueOf(snap.completed));
            lblCountCancelled.setText(String.valueOf(snap.cancelled));

            if (lblActiveRepairsTotal != null) {
                lblActiveRepairsTotal.setText(String.valueOf(snap.activeRepairs));
            }

            tblTechStats.setItems(FXCollections.observableArrayList(snap.techStats));

            lblTotalSKUs.setText(String.valueOf(snap.totalSKUs));
            lblTotalInventoryValue.setText(String.format("$%.2f", snap.totalInventoryValue));
            lblLowStockAlertCount.setText(String.valueOf(snap.lowStockCount));
        }));
    }

    @FXML
    private void handleRefresh() {
        loadReportsData();
    }
}
