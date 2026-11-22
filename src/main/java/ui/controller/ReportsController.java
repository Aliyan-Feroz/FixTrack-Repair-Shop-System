package ui.controller;

import business.Invoice;
import business.JobCard;
import dao.InventoryDAO;
import dao.InvoiceDAO;
import dao.JobCardDAO;
import dao.TechnicianDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Part;
import model.Technician;
import ui.context.AppContext;

import java.util.List;

public class ReportsController {

    // Financial KPI Cards
    @FXML private Label lblRealizedRevenue;
    @FXML private Label lblPendingReceivables;
    @FXML private Label lblTotalInvoiced;
    @FXML private Label lblDiscountsGiven;
    @FXML private Label lblAverageTicket;

    // Workflow State Counters & Progress Bars
    @FXML private Label lblCountIntake;
    @FXML private Label lblCountDiag;
    @FXML private Label lblCountApproval;
    @FXML private Label lblCountRepair;
    @FXML private Label lblCountQuality;
    @FXML private Label lblCountReady;
    @FXML private Label lblCountCompleted;
    @FXML private Label lblCountCancelled;

    @FXML private ProgressBar pbIntake;
    @FXML private ProgressBar pbDiag;
    @FXML private ProgressBar pbApproval;
    @FXML private ProgressBar pbRepair;
    @FXML private ProgressBar pbQuality;
    @FXML private ProgressBar pbReady;
    @FXML private ProgressBar pbCompleted;
    @FXML private ProgressBar pbCancelled;

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
    }

    public void loadReportsData() {
        JobCardDAO jobDAO = AppContext.getInstance().getJobCardDAO();
        InvoiceDAO invDAO = AppContext.getInstance().getInvoiceDAO();
        TechnicianDAO techDAO = AppContext.getInstance().getTechnicianDAO();
        InventoryDAO invenDAO = AppContext.getInstance().getInventoryDAO();

        List<JobCard> allJobs = jobDAO.getAllJobCards();
        List<Invoice> allInvoices = invDAO.getAllInvoices();
        List<Technician> allTechs = techDAO.getAllTechnicians();
        List<Part> allParts = invenDAO.getAllParts();

        // ── 1. Financial Metrics ──
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

        lblRealizedRevenue.setText(String.format("$%.2f", realizedRevenue));
        lblPendingReceivables.setText(String.format("$%.2f", pendingReceivables));
        lblTotalInvoiced.setText(String.format("$%.2f", totalInvoiced));
        lblDiscountsGiven.setText(String.format("$%.2f", totalDiscounts));
        lblAverageTicket.setText(String.format("$%.2f", avgTicket));

        // ── 2. Job Status Counts ──
        int totalJobs = allJobs.size();
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

        lblCountIntake.setText(String.valueOf(intake));
        lblCountDiag.setText(String.valueOf(diag));
        lblCountApproval.setText(String.valueOf(approval));
        lblCountRepair.setText(String.valueOf(repair));
        lblCountQuality.setText(String.valueOf(quality));
        lblCountReady.setText(String.valueOf(ready));
        lblCountCompleted.setText(String.valueOf(completed));
        lblCountCancelled.setText(String.valueOf(cancelled));

        double div = (totalJobs == 0) ? 1.0 : totalJobs;
        pbIntake.setProgress(intake / div);
        pbDiag.setProgress(diag / div);
        pbApproval.setProgress(approval / div);
        pbRepair.setProgress(repair / div);
        pbQuality.setProgress(quality / div);
        pbReady.setProgress(ready / div);
        pbCompleted.setProgress(completed / div);
        pbCancelled.setProgress(cancelled / div);

        // ── 3. Technician Stats Table ──
        List<TechnicianStats> statsList = allTechs.stream().map(tech -> {
            int active = tech.getActiveJobsCount();
            int comp = (int) allJobs.stream()
                    .filter(j -> j.getAssignedTech() != null &&
                            j.getAssignedTech().getTechId() == tech.getTechId() &&
                            j.getStatus() == JobCard.RepairStatus.COMPLETED)
                    .count();
            return new TechnicianStats(tech.getName(), tech.getSpecialty(), active, comp);
        }).toList();

        tblTechStats.setItems(FXCollections.observableArrayList(statsList));

        // ── 4. Inventory Health ──
        lblTotalSKUs.setText(String.valueOf(allParts.size()));
        double totalInventoryValue = allParts.stream()
                .mapToDouble(p -> p.getStockQuantity() * p.getPrice()).sum();
        lblTotalInventoryValue.setText(String.format("$%.2f", totalInventoryValue));
        long lowStockCount = allParts.stream().filter(Part::checkLowStock).count();
        lblLowStockAlertCount.setText(String.valueOf(lowStockCount));
    }

    @FXML
    private void handleRefresh() {
        loadReportsData();
    }
}

