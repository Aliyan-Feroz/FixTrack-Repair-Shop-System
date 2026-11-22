package ui.controller;

import business.JobCard;
import dao.CustomerDAO;
import dao.InventoryDAO;
import dao.JobCardDAO;
import dao.TechnicianDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import model.Customer;
import model.Part;
import model.Technician;
import ui.context.AppContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JobCardsController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TableView<JobCard> tblJobCards;

    @FXML private TableColumn<JobCard, Integer> colJobId;
    @FXML private TableColumn<JobCard, String> colCustomer;
    @FXML private TableColumn<JobCard, String> colDevice;
    @FXML private TableColumn<JobCard, String> colSerial;
    @FXML private TableColumn<JobCard, String> colTech;
    @FXML private TableColumn<JobCard, String> colStatus;
    @FXML private TableColumn<JobCard, Double> colGrandTotal;
    @FXML private TableColumn<JobCard, String> colDate;

    // Contextual Detail Drawer
    @FXML private VBox drawerEmptyState;
    @FXML private VBox drawerContentState;
    @FXML private Label lblDrawerJobTitle;
    @FXML private Label lblDrawerStatusBadge;
    @FXML private Label lblDrawerCustomer;
    @FXML private Label lblDrawerDevice;
    @FXML private Label lblDrawerSerial;
    @FXML private Label lblDrawerTech;
    @FXML private Label lblDrawerComplaint;
    @FXML private Label lblDrawerDiagnosis;
    @FXML private Label lblDrawerLabor;
    @FXML private Label lblDrawerParts;
    @FXML private Label lblDrawerTotal;

    @FXML private Button btnAdvanceStatus;
    @FXML private Button btnAssignTech;
    @FXML private Button btnUpdateDiagnosis;
    @FXML private Button btnAddParts;
    @FXML private Button btnGenerateInvoice;

    private final ObservableList<JobCard> jobList = FXCollections.observableArrayList();
    private FilteredList<JobCard> filteredJobs;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadJobCards();

        tblJobCards.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            displayJobDetails(selected);
        });
    }

    private void setupTable() {
        colJobId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getJobId()).asObject());
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCustomer() != null ? data.getValue().getCustomer().getName() : "Unknown"));
        colDevice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeviceDetails()));
        colSerial.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSerialNo()));
        colTech.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAssignedTech() != null ? data.getValue().getAssignedTech().getName() : "Unassigned"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colGrandTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().calculateTotal()).asObject());
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateCreated()));

        // Translucent pill status badges
        colStatus.setCellFactory(col -> new TableCell<JobCard, String>() {
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

        colGrandTotal.setCellFactory(col -> new TableCell<JobCard, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        });
    }

    private void setupFilters() {
        cmbStatusFilter.getItems().addAll("ALL", "INTAKE", "DIAGNOSIS", "AWAITING_APPROVAL",
                "REPAIRING", "QUALITY_CHECK", "READY", "COMPLETED", "CANCELLED");
        cmbStatusFilter.setValue("ALL");

        filteredJobs = new FilteredList<>(jobList, p -> true);

        Runnable applyFilter = () -> {
            String search = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
            String status = cmbStatusFilter.getValue();

            filteredJobs.setPredicate(job -> {
                boolean matchesStatus = "ALL".equalsIgnoreCase(status) || job.getStatus().name().equalsIgnoreCase(status);
                if (!matchesStatus) return false;

                if (search.isEmpty()) return true;

                String idStr = String.valueOf(job.getJobId());
                String cust = job.getCustomer() != null ? job.getCustomer().getName().toLowerCase() : "";
                String dev = job.getDeviceDetails() != null ? job.getDeviceDetails().toLowerCase() : "";
                String sn = job.getSerialNo() != null ? job.getSerialNo().toLowerCase() : "";

                return idStr.contains(search) || cust.contains(search) || dev.contains(search) || sn.contains(search);
            });
        };

        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter.run());
        cmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        tblJobCards.setItems(filteredJobs);
    }

    public void loadJobCards() {
        CompletableFuture.supplyAsync(() -> AppContext.getInstance().getJobCardDAO().getAllJobCards())
                .thenAccept(list -> Platform.runLater(() -> {
                    jobList.setAll(list);
                    if (!jobList.isEmpty()) {
                        tblJobCards.getSelectionModel().selectFirst();
                    } else {
                        displayJobDetails(null);
                    }
                }));
    }

    private void displayJobDetails(JobCard jc) {
        if (jc == null) {
            if (drawerEmptyState != null) drawerEmptyState.setVisible(true);
            if (drawerContentState != null) drawerContentState.setVisible(false);
            return;
        }

        if (drawerEmptyState != null) drawerEmptyState.setVisible(false);
        if (drawerContentState != null) drawerContentState.setVisible(true);

        lblDrawerJobTitle.setText("Job #" + jc.getJobId());
        lblDrawerStatusBadge.setText(jc.getStatus().name().replace("_", " "));
        lblDrawerStatusBadge.getStyleClass().clear();
        lblDrawerStatusBadge.getStyleClass().add("badge");
        switch (jc.getStatus()) {
            case INTAKE: lblDrawerStatusBadge.getStyleClass().add("badge-intake"); break;
            case DIAGNOSIS: lblDrawerStatusBadge.getStyleClass().add("badge-diagnosis"); break;
            case AWAITING_APPROVAL: lblDrawerStatusBadge.getStyleClass().add("badge-awaiting"); break;
            case REPAIRING: lblDrawerStatusBadge.getStyleClass().add("badge-repairing"); break;
            case QUALITY_CHECK: lblDrawerStatusBadge.getStyleClass().add("badge-quality"); break;
            case READY: lblDrawerStatusBadge.getStyleClass().add("badge-ready"); break;
            case COMPLETED: lblDrawerStatusBadge.getStyleClass().add("badge-completed"); break;
            case CANCELLED: lblDrawerStatusBadge.getStyleClass().add("badge-cancelled"); break;
        }

        lblDrawerCustomer.setText(jc.getCustomer() != null ? jc.getCustomer().getName() + " (" + jc.getCustomer().getPhone() + ")" : "-");
        lblDrawerDevice.setText(jc.getDeviceDetails());
        lblDrawerSerial.setText(jc.getSerialNo());
        lblDrawerTech.setText(jc.getAssignedTech() != null ? jc.getAssignedTech().getName() : "Unassigned");
        lblDrawerComplaint.setText(jc.getComplaint() == null || jc.getComplaint().isBlank() ? "None recorded" : jc.getComplaint());
        lblDrawerDiagnosis.setText(jc.getDiagnosis() == null || jc.getDiagnosis().isBlank() ? "Pending" : jc.getDiagnosis());
        lblDrawerLabor.setText(String.format("$%.2f", jc.getLaborCharge()));
        lblDrawerParts.setText(String.format("$%.2f", jc.getPartsTotal()));
        lblDrawerTotal.setText(String.format("$%.2f", jc.calculateTotal()));

        btnAdvanceStatus.setDisable(jc.getStatus() == JobCard.RepairStatus.COMPLETED || jc.getStatus() == JobCard.RepairStatus.CANCELLED);
        if (btnGenerateInvoice != null) {
            btnGenerateInvoice.setDisable(jc.getStatus() != JobCard.RepairStatus.READY && jc.getStatus() != JobCard.RepairStatus.COMPLETED);
        }
    }

    @FXML
    private void handleNewJobCard() {
        CustomerDAO customerDAO = AppContext.getInstance().getCustomerDAO();
        TechnicianDAO techDAO = AppContext.getInstance().getTechnicianDAO();

        List<Customer> customers = customerDAO.getAllCustomers();
        List<Technician> techs = techDAO.getAllTechnicians();

        if (customers.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Register a customer before creating a job card.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<JobCard> dialog = new Dialog<>();
        dialog.setTitle("New Job Card");
        dialog.setHeaderText("Intake Device Details");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        ComboBox<Customer> cmbCustomer = new ComboBox<>(FXCollections.observableArrayList(customers));
        cmbCustomer.setConverter(new StringConverter<Customer>() {
            @Override public String toString(Customer c) { return c != null ? c.getName() + " (" + c.getPhone() + ")" : ""; }
            @Override public Customer fromString(String string) { return null; }
        });
        cmbCustomer.getSelectionModel().selectFirst();

        TextField txtDevice = new TextField();
        txtDevice.setPromptText("Device model");

        TextField txtSerial = new TextField();
        txtSerial.setPromptText("Serial number");

        TextArea txtComplaint = new TextArea();
        txtComplaint.setPromptText("Reported fault");
        txtComplaint.setPrefRowCount(2);

        ComboBox<Technician> cmbTech = new ComboBox<>(FXCollections.observableArrayList(techs));
        cmbTech.setConverter(new StringConverter<Technician>() {
            @Override public String toString(Technician t) { return t != null ? t.getName() + " (" + t.getSpecialty() + ")" : "Unassigned"; }
            @Override public Technician fromString(String string) { return null; }
        });

        grid.add(new Label("Customer:"), 0, 0);
        grid.add(cmbCustomer, 1, 0);
        grid.add(new Label("Device:"), 0, 1);
        grid.add(txtDevice, 1, 1);
        grid.add(new Label("Serial No:"), 0, 2);
        grid.add(txtSerial, 1, 2);
        grid.add(new Label("Complaint:"), 0, 3);
        grid.add(txtComplaint, 1, 3);
        grid.add(new Label("Technician:"), 0, 4);
        grid.add(cmbTech, 1, 4);

        dialogPane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Customer cust = cmbCustomer.getValue();
                String dev = txtDevice.getText() == null ? "" : txtDevice.getText().trim();
                String sn = txtSerial.getText() == null ? "" : txtSerial.getText().trim();
                String comp = txtComplaint.getText() == null ? "" : txtComplaint.getText().trim();

                if (cust != null && !dev.isEmpty()) {
                    JobCard jc = new JobCard(0, cust, dev, sn, comp);
                    Technician assigned = cmbTech.getValue();
                    if (assigned != null) {
                        jc.assignTechnician(assigned);
                    }
                    return jc;
                }
            }
            return null;
        });

        Optional<JobCard> result = dialog.showAndWait();
        result.ifPresent(jc -> {
            AppContext.getInstance().getJobCardDAO().addJobCard(jc);
            loadJobCards();
        });
    }

    @FXML
    private void handleAdvanceStatus() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ChoiceDialog<JobCard.RepairStatus> dialog = new ChoiceDialog<>(
                getNextSuggestedStatus(selected.getStatus()),
                JobCard.RepairStatus.values()
        );
        dialog.setTitle("Update Status");
        dialog.setHeaderText("Advance Job #" + selected.getJobId());
        dialog.setContentText("Select new state:");

        Optional<JobCard.RepairStatus> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            selected.updateStatus(newStatus);
            AppContext.getInstance().getJobCardDAO().updateJobCard(selected);
            tblJobCards.refresh();
            displayJobDetails(selected);
        });
    }

    private JobCard.RepairStatus getNextSuggestedStatus(JobCard.RepairStatus current) {
        switch (current) {
            case INTAKE: return JobCard.RepairStatus.DIAGNOSIS;
            case DIAGNOSIS: return JobCard.RepairStatus.AWAITING_APPROVAL;
            case AWAITING_APPROVAL: return JobCard.RepairStatus.REPAIRING;
            case REPAIRING: return JobCard.RepairStatus.QUALITY_CHECK;
            case QUALITY_CHECK: return JobCard.RepairStatus.READY;
            case READY: return JobCard.RepairStatus.COMPLETED;
            default: return current;
        }
    }

    @FXML
    private void handleAssignTech() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        List<Technician> techs = AppContext.getInstance().getTechnicianDAO().getAllTechnicians();
        if (techs.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No technicians registered.", ButtonType.OK).showAndWait();
            return;
        }

        ChoiceDialog<Technician> dialog = new ChoiceDialog<>(techs.get(0), techs);
        dialog.setTitle("Assign Technician");
        dialog.setHeaderText("Assign specialist to Job #" + selected.getJobId());
        dialog.setContentText("Technician:");

        Optional<Technician> result = dialog.showAndWait();
        result.ifPresent(tech -> {
            selected.assignTechnician(tech);
            AppContext.getInstance().getJobCardDAO().updateJobCard(selected);
            tblJobCards.refresh();
            displayJobDetails(selected);
        });
    }

    @FXML
    private void handleUpdateDiagnosis() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Diagnosis & Labor");
        dialog.setHeaderText("Job #" + selected.getJobId() + " — " + selected.getDeviceDetails());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextArea txtDiag = new TextArea(selected.getDiagnosis());
        txtDiag.setPrefRowCount(3);
        TextField txtLabor = new TextField(String.format("%.2f", selected.getLaborCharge()));

        grid.add(new Label("Diagnosis Notes:"), 0, 0);
        grid.add(txtDiag, 1, 0);
        grid.add(new Label("Labor Charge ($):"), 0, 1);
        grid.add(txtLabor, 1, 1);

        pane.setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            selected.setDiagnosis(txtDiag.getText());
            try {
                double labor = Double.parseDouble(txtLabor.getText().trim());
                selected.setLaborCharge(labor);
            } catch (NumberFormatException ignored) {}

            AppContext.getInstance().getJobCardDAO().updateJobCard(selected);
            tblJobCards.refresh();
            displayJobDetails(selected);
        }
    }

    @FXML
    private void handleAddParts() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        InventoryDAO inventoryDAO = AppContext.getInstance().getInventoryDAO();
        List<Part> parts = inventoryDAO.getAllParts();
        if (parts.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No parts in inventory catalogue.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Consume Inventory Part");
        dialog.setHeaderText("Add Part to Job #" + selected.getJobId());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        ComboBox<Part> cmbPart = new ComboBox<>(FXCollections.observableArrayList(parts));
        cmbPart.setConverter(new StringConverter<Part>() {
            @Override public String toString(Part p) {
                return p != null ? p.getPartName() + " (" + p.getStockQuantity() + " left, $" + p.getPrice() + ")" : "";
            }
            @Override public Part fromString(String string) { return null; }
        });
        cmbPart.getSelectionModel().selectFirst();

        Spinner<Integer> spnQty = new Spinner<>(1, 100, 1);

        grid.add(new Label("Part:"), 0, 0);
        grid.add(cmbPart, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(spnQty, 1, 1);

        pane.setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            Part part = cmbPart.getValue();
            int qty = spnQty.getValue();
            if (part != null) {
                boolean success = selected.addPart(part, qty);
                if (success) {
                    AppContext.getInstance().getJobCardDAO().saveJobParts(selected);
                    inventoryDAO.updatePart(part);
                    tblJobCards.refresh();
                    displayJobDetails(selected);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Insufficient stock for " + part.getPartName(), ButtonType.OK).showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleGenerateInvoiceForJob() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AppContext.getInstance().getMainLayoutController().navigateTo("Invoices");
        }
    }

    @FXML
    private void handleRefresh() {
        loadJobCards();
    }
}
