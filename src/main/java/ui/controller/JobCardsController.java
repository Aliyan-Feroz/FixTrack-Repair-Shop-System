package ui.controller;

import business.JobCard;
import dao.CustomerDAO;
import dao.InventoryDAO;
import dao.JobCardDAO;
import dao.TechnicianDAO;
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
    @FXML private TableColumn<JobCard, Double> colLabor;
    @FXML private TableColumn<JobCard, Double> colPartsTotal;
    @FXML private TableColumn<JobCard, Double> colGrandTotal;
    @FXML private TableColumn<JobCard, String> colDate;

    // Action Panel Controls
    @FXML private Button btnAdvanceStatus;
    @FXML private Button btnAssignTech;
    @FXML private Button btnUpdateDiagnosis;
    @FXML private Button btnAddParts;
    @FXML private Button btnViewSummary;
    @FXML private Label lblSelectedJobSummary;

    private final ObservableList<JobCard> jobList = FXCollections.observableArrayList();
    private FilteredList<JobCard> filteredJobs;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadJobCards();

        tblJobCards.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            updateActionButtons(selected);
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
        colLabor.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getLaborCharge()).asObject());
        colPartsTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPartsTotal()).asObject());
        colGrandTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().calculateTotal()).asObject());
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateCreated()));

        // Styled Status Badges
        colStatus.setCellFactory(col -> new TableCell<JobCard, String>() {
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

        // Price Formatters
        colLabor.setCellFactory(col -> formatCurrencyCell());
        colPartsTotal.setCellFactory(col -> formatCurrencyCell());
        colGrandTotal.setCellFactory(col -> formatCurrencyCell());
    }

    private TableCell<JobCard, Double> formatCurrencyCell() {
        return new TableCell<JobCard, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("$%.2f", item));
            }
        };
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
                String comp = job.getComplaint() != null ? job.getComplaint().toLowerCase() : "";

                return idStr.contains(search) || cust.contains(search) || dev.contains(search) ||
                        sn.contains(search) || comp.contains(search);
            });
        };

        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter.run());
        cmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        tblJobCards.setItems(filteredJobs);
    }

    public void loadJobCards() {
        JobCardDAO dao = AppContext.getInstance().getJobCardDAO();
        List<JobCard> list = dao.getAllJobCards();
        jobList.setAll(list);

        if (!jobList.isEmpty()) {
            tblJobCards.getSelectionModel().selectFirst();
        } else {
            updateActionButtons(null);
        }
    }

    private void updateActionButtons(JobCard jc) {
        boolean disabled = (jc == null);
        btnAdvanceStatus.setDisable(disabled);
        btnAssignTech.setDisable(disabled);
        btnUpdateDiagnosis.setDisable(disabled);
        btnAddParts.setDisable(disabled);
        btnViewSummary.setDisable(disabled);

        if (jc != null) {
            lblSelectedJobSummary.setText(String.format("Selected: Job #%d (%s - %s) | Status: %s",
                    jc.getJobId(), jc.getCustomer().getName(), jc.getDeviceDetails(), jc.getStatus()));
        } else {
            lblSelectedJobSummary.setText("Select a job card to manage status, assign technicians, or add parts.");
        }
    }

    @FXML
    private void handleNewJobCard() {
        CustomerDAO customerDAO = AppContext.getInstance().getCustomerDAO();
        TechnicianDAO techDAO = AppContext.getInstance().getTechnicianDAO();

        List<Customer> customers = customerDAO.getAllCustomers();
        List<Technician> techs = techDAO.getAllTechnicians();

        if (customers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No customers registered. Please create a customer first.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Dialog<JobCard> dialog = new Dialog<>();
        dialog.setTitle("New Job Card Intake");
        dialog.setHeaderText("Register Device for Repair");

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
        txtDevice.setPromptText("e.g. iPhone 13 Pro, Dell XPS 15");

        TextField txtSerial = new TextField();
        txtSerial.setPromptText("e.g. SN-892348A");

        TextArea txtComplaint = new TextArea();
        txtComplaint.setPromptText("Describe the symptoms or fault reported by customer...");
        txtComplaint.setPrefRowCount(3);

        ComboBox<Technician> cmbTech = new ComboBox<>(FXCollections.observableArrayList(techs));
        cmbTech.setConverter(new StringConverter<Technician>() {
            @Override public String toString(Technician t) { return t != null ? t.getName() + " (" + t.getSpecialty() + ")" : "Unassigned"; }
            @Override public Technician fromString(String string) { return null; }
        });

        grid.add(new Label("Customer:"), 0, 0);
        grid.add(cmbCustomer, 1, 0);
        grid.add(new Label("Device Details:"), 0, 1);
        grid.add(txtDevice, 1, 1);
        grid.add(new Label("Serial Number:"), 0, 2);
        grid.add(txtSerial, 1, 2);
        grid.add(new Label("Fault Complaint:"), 0, 3);
        grid.add(txtComplaint, 1, 3);
        grid.add(new Label("Assign Tech (Optional):"), 0, 4);
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
        dialog.setTitle("Advance Job Card Status");
        dialog.setHeaderText("Current Status: " + selected.getStatus());
        dialog.setContentText("Select new status state:");

        Optional<JobCard.RepairStatus> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            selected.updateStatus(newStatus);
            AppContext.getInstance().getJobCardDAO().updateJobCard(selected);
            tblJobCards.refresh();
            updateActionButtons(selected);
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
        dialog.setContentText("Choose Technician:");

        Optional<Technician> result = dialog.showAndWait();
        result.ifPresent(tech -> {
            selected.assignTechnician(tech);
            AppContext.getInstance().getJobCardDAO().updateJobCard(selected);
            tblJobCards.refresh();
            updateActionButtons(selected);
        });
    }

    @FXML
    private void handleUpdateDiagnosis() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Diagnosis & Labor");
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
            updateActionButtons(selected);
        }
    }

    @FXML
    private void handleAddParts() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        InventoryDAO inventoryDAO = AppContext.getInstance().getInventoryDAO();
        List<Part> parts = inventoryDAO.getAllParts();
        if (parts.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No inventory parts available.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Part to Job");
        dialog.setHeaderText("Consume Inventory Part for Job #" + selected.getJobId());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        ComboBox<Part> cmbPart = new ComboBox<>(FXCollections.observableArrayList(parts));
        cmbPart.setConverter(new StringConverter<Part>() {
            @Override public String toString(Part p) {
                return p != null ? p.getPartName() + " (In Stock: " + p.getStockQuantity() + ", $" + p.getPrice() + ")" : "";
            }
            @Override public Part fromString(String string) { return null; }
        });
        cmbPart.getSelectionModel().selectFirst();

        Spinner<Integer> spnQty = new Spinner<>(1, 100, 1);

        grid.add(new Label("Select Part:"), 0, 0);
        grid.add(cmbPart, 1, 0);
        grid.add(new Label("Quantity Used:"), 0, 1);
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
                    updateActionButtons(selected);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Insufficient stock for: " + part.getPartName(), ButtonType.OK).showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleViewSummary() {
        JobCard selected = tblJobCards.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Job Card Summary");
        alert.setHeaderText("Full Specification: Job #" + selected.getJobId());

        TextArea textArea = new TextArea(selected.toDisplayString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(350);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        loadJobCards();
    }
}

